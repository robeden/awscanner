package awscanner.ec2;

import java.util.Map;


public record SnapshotInfo(String id,
                           Map<String,String> tags,
                           String volume_id,
                           String description,
                           String storage_tier) {
}
