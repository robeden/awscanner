package awscanner.ec2;

import awscanner.price.PriceResults;
import awscanner.price.ResourceWithPrice;
import awscanner.util.ResourceInfo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public record EBSInfo(String id,
                      Map<String,String> tags,
                      Set<String> attached_instance_ids,
                      String snapshot_id,
                      String state,
                      Boolean encrypted,
                      Integer size,
                      Integer iops,
                      Integer throughput,
                      String type,
                      int days_since_creation,
                      Optional<BigDecimal> price_per_hour ) implements ResourceInfo, ResourceWithPrice {


    public boolean isAttached() {
        return attached_instance_ids.size() > 0;
    }

    public boolean isAttachedOrImage(Set<String> image_ids) {
        return attached_instance_ids.size() > 0 || image_ids.contains( snapshot_id );
    }
}
