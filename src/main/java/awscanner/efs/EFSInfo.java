package awscanner.efs;

import awscanner.price.PriceResults;
import software.amazon.awssdk.services.efs.model.FileSystemSize;


public record EFSInfo( String id,
                       String name,
                       long size_bytes,
                       Boolean encrypted,
                       String performance_mode,
                       String throughput_mode,
                       Double provisioned_throughput_mibps,
                       String life_cycle_state,
                       int days_since_creation,
                       PriceResults price ) {

}
