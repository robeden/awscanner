package awscanner.ec2;

import awscanner.price.ResourceWithPrice;
import awscanner.util.ResourceInfo;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.LicenseConfiguration;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;


public record InstanceInfo(String id,
                           Map<String,String> tags,
                           String type,
                           InstanceState state,
                           String image_id,
                           String private_ip,
                           String public_ip,
                           String subnet_id,
                           String vpc_id,
                           Map<String,SecurityGroup> security_groups,
                           String arch,
                           String platform,
                           String reservation_id,
                           Instant launch_time,
                           Set<String> volume_ids,
                           Set<String> security_group_ids,
                           List<LicenseConfiguration> licenses,
                           Optional<BigDecimal> price_per_hour ) implements ResourceInfo, ResourceWithPrice {

    public boolean isRunning() {
        return state == InstanceState.RUNNING || state == InstanceState.PENDING;
    }


    @Override
    public Stream<String> usesIds() {
        return Stream.concat(
            Stream.of( image_id ),
            Stream.concat(
                volume_ids.stream(),
                security_group_ids.stream()
            )
        );
    }
}
