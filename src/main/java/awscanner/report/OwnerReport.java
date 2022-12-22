package awscanner.report;

import awscanner.graph.ResourceGraph;
import awscanner.price.PricingEstimation;
import awscanner.price.ResourceWithPrice;
import awscanner.util.ResourceInfo;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;


public class OwnerReport {
    private static final BigDecimal TWENTY_FOUR = BigDecimal.valueOf( 24 ); // for hour/day conversions
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance();


    private final String owner_tag;
    private final String tag_split_by;
    private final File report_data_file;

    private final Map<String,Set<ResourceInfo>> owner_resources =
        new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
    private final Set<ResourceInfo> unowned_resources = new HashSet<>();


    public OwnerReport( String owner_tag, String tag_split_by, File report_data_file ) {
        this.owner_tag = Objects.requireNonNull( owner_tag );
        this.tag_split_by = tag_split_by == null || tag_split_by.length() == 0 ? null : tag_split_by;
        this.report_data_file = report_data_file;
    }


    public void report() throws IOException {
        if ( report_data_file == null ) reportToStdout();
        else reportToFile( report_data_file );
        // TODO: print to stdout (text) or file (json)
    }


    public void process( ResourceGraph graph ) {
        graph.forEachResource( this::processResource );
    }

    private void processResource( ResourceInfo resource ) {
        Set<String> owners = splitTagValue( resource.tags().get( owner_tag ), tag_split_by );
        if ( owners.isEmpty() ) {
            // TODO: Look for using resources to see if it's ultimately only owned by one
            //       which does have an owner?
            unowned_resources.add( resource );
            return;
        }
        owners.forEach( o -> owner_resources.computeIfAbsent( o, _o -> new HashSet<>() ).add( resource ) );
    }


    private void reportToStdout() {
        boolean first = true;
        for( Map.Entry<String,Set<ResourceInfo>> entry : owner_resources.entrySet() ) {
            if ( first ) first = false;
            else System.out.println();

            reportToStdout_singleEntry( entry.getKey(), entry.getValue() );
        }

        if ( !owner_resources.isEmpty() ) {
            System.out.println();
        }
        reportToStdout_singleEntry( "Unowned", unowned_resources );
    }

    private void reportToStdout_singleEntry( String owner, Set<ResourceInfo> resources ) {
        System.out.println( owner );
        var total = resources.stream()
            .sorted( Comparator.comparing( ResourceInfo::id ) )
            .filter( ri -> ri instanceof ResourceWithPrice )
            .filter( ri -> ( ( ResourceWithPrice ) ri ).price_per_hour().isPresent() )
            .peek( ri -> System.out.println( "  - " + describe( ri ) + " - " +
                ( ( ResourceWithPrice ) ri ).price_per_hour()
                    .map( in -> in.multiply( TWENTY_FOUR ) )
                    .map( CURRENCY_FORMATTER::format )
                    .orElse( "unknown" )
                + "/day" ) )
            // convert to price/day
            .map( ri -> ( ( ResourceWithPrice ) ri ).price_per_hour().get().multiply( TWENTY_FOUR ) )
            .reduce( BigDecimal::add );
        if ( total.isPresent() ) {
            System.out.println( "TOTAL: " + CURRENCY_FORMATTER.format( total.get() ) + "/day" );
        }
    }

    private void reportToFile( File file ) throws IOException {
        // TODO
    }


    private String describe( ResourceInfo resource ) {
        var name = resource.name();
        if ( name == null ) {
            return resource.id();
        }
        else {
            return name + " (" + resource.id() + ")";
        }
    }


    static Set<String> splitTagValue( String value, String split_by ) {
        if ( value == null ) return Set.of();
        value = value.trim();
        if ( value.isEmpty() ) return Set.of();

        if ( split_by == null ) return Set.of( value );

        return Set.of( value.split( Pattern.quote( split_by ) ) );
    }
}
