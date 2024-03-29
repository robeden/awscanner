package awscanner.price;

import com.squareup.moshi.JsonReader;
import okio.Buffer;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Optional.*;


public class PricingEstimation {
    private final ExecutorService executor;
    private final PricingClient client;
    private final ConcurrentHashMap<ResourcePriceAttributes<?>, Future<Optional<PriceSpecs>>> lookup_map =
        new ConcurrentHashMap<>();

    private final AtomicBoolean disable = new AtomicBoolean( false );


    public record PriceSpecs(BigDecimal price, String unit) {};


    public PricingEstimation( ExecutorService executor, PricingClient client ) {
        this.executor = executor;
        this.client = client;
    }


    public boolean disabled() {
        return disable.get();
    }


    public Optional<PriceSpecs> lookupCost( ResourcePriceAttributes<?> attributes ) {
        Optional<PriceSpecs> cph = Optional.empty();
        if ( disable.get() ) return cph;

        Future<Optional<PriceSpecs>> cph_future =
            lookup_map.computeIfAbsent( attributes, a -> executor.submit( () -> doPriceLookup( a ) ) );
        try {
            cph = cph_future.get();
        }
        catch ( Exception e ) {
            boolean handled = false;
            if ( e.getCause() instanceof PricingException ) {
                if ( ( ( PricingException ) e.getCause() ).statusCode() == 400 ) {
                    disable.set( true );
                    System.err.println( "Pricing lookups disabled due to lookup permission error: " +
                        e.getCause() );
                    handled = true;
                }
            }

            if ( !handled ) e.printStackTrace();
        }
        return cph;
    }


    private Optional<PriceSpecs> doPriceLookup( ResourcePriceAttributes<?> attributes ) {
        GetProductsResponse products = client.getProducts(
            GetProductsRequest.builder()
                .serviceCode( attributes.serviceCode() )
                .filters( attributes.buildFilters() )
                .build() );

        Set<PriceSpecs> values = products.priceList().stream()
            .map( PricingEstimation::parsePrice )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .filter( ps -> attributes.isUnitExpected( ps.unit ) )
            .collect( Collectors.toSet() );

        if ( values.size() == 1 ) {
            return Optional.of( values.iterator().next() );
        }
        else return Optional.empty();
    }


    public static Filter createFilter( String field, String value ) {
        return Filter.builder().type( FilterType.TERM_MATCH ).field( field ).value( value ).build();
    }

    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    static Optional<PriceSpecs> parsePrice(String price_list_json) {
        try ( Buffer read_buffer = new Buffer() ) {
            read_buffer.writeString( price_list_json, StandardCharsets.UTF_8 );

            JsonReader reader = JsonReader.of( read_buffer );

            // HACK ALERT: All of this is SUPER GROSS, but the structure gets weird and I don't want to
            //             deal with it correctly (i.e., using proper Moshi parsing) right now.
            //             (Missing dpath in python for this...)
            //
            // Example snippet:
            //  {
            //    "product": {
            //      "productFamily": "Compute Instance",
            //      "attributes": { ...snipped... },
            //      "sku": "PR3XTEQYEMKGK56X"
            //    },
            //    "serviceCode": "AmazonEC2",
            //    "terms": {
            //      "OnDemand": {
            //        "PR3XTEQYEMKGK56X.JRTCKXETXF": {
            //          "priceDimensions": {
            //            "PR3XTEQYEMKGK56X.JRTCKXETXF.6YS6EN2CT7": {
            //              ... snipped stuff...
            //              "pricePerUnit": {
            //                "USD": "1.7440000000"
            //              }
            //            }
            //          },
            //          ... snipped stuff...
            //        }
            //      },
            try {
                reader.beginObject();
                if ( !seekToName( "terms", reader ) ) return empty();
                reader.beginObject();
                if ( !seekToName( "OnDemand", reader ) ) return empty();   // TODO: might want to handle reserved
                reader.beginObject();
                reader.nextName();      // crazy identifier
                reader.beginObject();
                if ( !seekToName( "priceDimensions", reader ) ) return empty();
                reader.beginObject();
                reader.nextName();      // crazy identifier II
                reader.beginObject();

                if ( !seekToName( "unit", reader ) ) return empty();
                String unit = reader.nextString();

                if ( !seekToName( "pricePerUnit", reader ) ) return empty();
                reader.beginObject();
                if ( !seekToName( "USD", reader ) ) return empty();
                return of( reader.nextString() )
                    .map( BigDecimal::new )
                    .map( BigDecimal::stripTrailingZeros )
                    .filter( bd -> !bd.equals( BigDecimal.ZERO ) )     // remove zero values
                    .map( bd -> new PriceSpecs( bd, unit ) );
            }
            catch( Exception ex ) {
                ex.printStackTrace();
                return empty();
            }
        }
    }


    private static boolean seekToName(String name, JsonReader reader) throws IOException {
        while ( reader.hasNext() ) {
            if ( reader.nextName().equals( name ) ) {
                return true;
            }
            reader.skipValue();
        }
        return false;
    }
}
