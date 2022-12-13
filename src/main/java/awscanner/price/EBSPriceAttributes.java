package awscanner.price;

import software.amazon.awssdk.services.pricing.model.Filter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static awscanner.price.PricingEstimation.createFilter;


public record EBSPriceAttributes(String region,
                                 UsageType type )
    implements ResourcePriceAttributes {

    public enum UsageType {
        // For API identifiers, see:
        // https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeVolumes.html
        GP2( "General Purpose", "EBS:VolumeUsage.gp2", "gp2" ),
        GP3( "General Purpose", "EBS:VolumeUsage.gp3", "gp3" ),
        MAGNETIC( "Magnetic", "EBS:VolumeUsage", "standard" ),
        COLD_HDD( "Cold HDD",  "EBS:VolumeUsage.sc1", "sc1" ),
        PROVISIONED_IOPS1( "Provisioned IOPS", "EBS:VolumeUsage.io2", "io2" ),
        PROVISIONED_IOPS2( "Provisioned IOPS", "EBS:VolumeUsage.piops", "io1" ),
        THROUGHPUT_OPTIMIZED_HDD( "Throughput Optimized HDD", "EBS:VolumeUsage.st1", "st1" );

        private final String volumeType;
        private final String usageType;
        private final String apiIdentifier;

        UsageType( String volumeType, String usageType, String apiIdentifier ) {
            this.volumeType = volumeType;
            this.usageType = usageType;
            this.apiIdentifier = apiIdentifier;
        }

        public static Optional<UsageType> findByApiIdentifier( String identifier ) {
            for ( var type : values() ) {
                if ( type.apiIdentifier.equalsIgnoreCase( identifier ) ) {
                    return Optional.of( type );
                }
            }
            return Optional.empty();
        }
    }


    @Override
    public String serviceCode() {
        return "AmazonEC2";
    }


    @Override
    public Collection<Filter> buildFilters() {
        return List.of(
            createFilter( "productFamily", "Storage" ),
            createFilter( "regionCode", region ),
            createFilter( "volumeApiName", type.apiIdentifier )
        );
    }
}
