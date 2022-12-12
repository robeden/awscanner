package awscanner.rds;

import awscanner.util.ResourceInfo;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public record DBInstanceInfo(String id,
                             Map<String,String> tags,
                             String name,
                             String instance_class,
                             String instance_status,
                             String cluster_identifier,
                             Boolean publicly_accessible,
                             Set<String> db_security_group_ids,
                             Set<String> vpc_security_group_ids,
                             String engine,
                             String engine_version,
                             Integer iops,
                             Integer allocated_storage,
                             String storage_type,
                             Boolean storage_encrypted,
                             Instant instance_creation_time) implements ResourceInfo {

    @Override
    public Stream<String> usesIds() {
        return Stream.concat(
            db_security_group_ids.stream(),
            vpc_security_group_ids.stream()
        );
    }
}
