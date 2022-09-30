package awscanner.price;

import software.amazon.awssdk.services.pricing.model.Filter;

import java.util.Collection;
import java.util.List;

import static awscanner.price.PricingEstimation.createFilter;


public record EBSPriceAttributes(String region,
                                 UsageType type )
    implements ResourcePriceAttributes {

    public enum UsageType {
        GP2( "General Purpose", "EBS:VolumeUsage.gp2" ),
        GP3( "General Purpose", "EBS:VolumeUsage.gp3" ),
        MAGNETIC( "Magnetic", "EBS:VolumeUsage" ),
        COLD_HDD( "Cold HDD",  "EBS:VolumeUsage.sc1" ),
        PROVISIONED_IOPS1( "Provisioned IOPS", "EBS:VolumeUsage.io2" ),
        PROVISIONED_IOPS2( "Provisioned IOPS", "EBS:VolumeUsage.piops" ),
        THROUGHPUT_OPTIMIZED_HDD( "Throughput Optimized HDD", "EBS:VolumeUsage.st1" );

        private final String volumeType;
        private final String usageType;

        UsageType( String volumeType, String usageType ) {
            this.volumeType = volumeType;
            this.usageType = usageType;
        }
    }


    @Override
    public String serviceCode() {
        return "AmazonEC2";
    }


    @Override
    public Collection<Filter> buildFilters() {
        return List.of(
            createFilter( "regionCode", region ),
            createFilter( "volumeType", type.volumeType ),
            createFilter( "usageType", type.usageType )
        );
    }
}
