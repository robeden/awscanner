package awscanner.price;

import software.amazon.awssdk.services.pricing.model.Filter;

import java.util.Collection;
import java.util.List;

import static awscanner.price.PricingEstimation.createFilter;


public record EC2PriceAttributes( String region,
                                  String instance_type,
                                  OperatingSystem os,
                                  boolean dedicated )
    implements ResourcePriceAttributes {

    public enum OperatingSystem {
        LINUX( "Linux" ),
        WINDOWS( "Windows" );

        private final String string;

        OperatingSystem( String string ) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
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
            createFilter( "instanceType", instance_type ),
            createFilter( "operatingSystem", os.string ),
            createFilter( "tenancy", dedicated ? "Dedicated" : "Shared" ),
            createFilter( "preInstalledSw", "NA" ),
            createFilter( "capacitystatus", "Used")   // or AllocatedCapacityReservation,
                                                      //    UnusedCapacityReservation (possibly others)
        );
    }
}
