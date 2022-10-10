package awscanner.ec2;

import java.util.Map;


public record SnapshotInfo(String id,
                           Map<String,String> tags,
                           String volume_id,
                           String owner_id,
                           String owner_alias,
                           String description,
                           String storage_tier,
                           int days_since_creation ) {
}
