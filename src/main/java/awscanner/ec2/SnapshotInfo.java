package awscanner.ec2;

import awscanner.util.ResourceInfo;

import java.util.Map;
import java.util.stream.Stream;


public record SnapshotInfo(String id,
                           Map<String,String> tags,
                           String volume_id,
                           String owner_id,
                           String owner_alias,
                           String description,
                           String storage_tier,
                           int days_since_creation ) implements ResourceInfo {

    @Override
    public Stream<String> usesIds() {
        return Stream.of( volume_id );
    }
}
