package awscanner.price;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.DescribeServicesResponse;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;


public class Testing {
    public static void main( String[] args ) {
        AwsCredentialsProvider cred_provider = ProfileCredentialsProvider.create( args[0] );

        var client = PricingClient.builder()
            .region( Region.US_EAST_1 )//region )
            .credentialsProvider( cred_provider )
            .build();

        client.describeServicesPaginator().stream()
            .flatMap( r -> r.services().stream() )
            .forEach( s -> System.out.println(s.serviceCode() + "  -  " + s) );


        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        client.getProductsPaginator(
            GetProductsRequest.builder()
                .serviceCode( "AmazonEFS" )
                .filters(
                    PricingEstimation.createFilter( "regionCode", "us-east-1" )
//                    PricingEstimation.createFilter( "productFamily", "storage" )
                )
                .build() ).stream()
//            .flatMap( p -> p.priceList().stream() )
//            .filter( s -> !s.startsWith( "{\"product\":{\"productFamily\":\"Compute Instance\"" ) )
//            .filter( s -> !s.startsWith( "{\"product\":{\"productFamily\":\"Dedicated Host\"" ) )
//            .filter( s -> !s.startsWith( "{\"product\":{\"productFamily\":\"CPU Credits\"" ) )
//            .filter( s -> !s.startsWith( "{\"product\":{\"productFamily\":\"Load Balancer\"" ) )
//            .filter( s -> !s.startsWith( "{\"product\":{\"productFamily\":\"Compute Instance (bare metal)\"" ) )
            .forEach( System.out::println );
    }
}
