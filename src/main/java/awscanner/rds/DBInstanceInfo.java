package awscanner.rds;

import java.time.Instant;
import java.util.Map;
import java.util.Set;


public record DBInstanceInfo( String id,
                              Map<String,String> tags,
                              String name,
                              String instance_class,
                              String instance_status,
                              String cluster_identifier,
                              Boolean publicly_accessible,
                              Set<String> db_security_groups,
                              Set<String> vpc_security_groups,
                              String engine,
                              String engine_version,
                              Integer iops,
                              Integer allocated_storage,
                              String storage_type,
                              Boolean storage_encrypted,
                              Instant instance_creation_time ) {
}
