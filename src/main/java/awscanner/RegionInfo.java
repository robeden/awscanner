package awscanner;

import awscanner.ec2.EBSInfo;
import awscanner.ec2.ImageInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;
import awscanner.rds.DBInstanceInfo;
import software.amazon.awssdk.regions.Region;

import java.util.Map;


public record RegionInfo(Region region,
                         Map<String,InstanceInfo> instances,
                         Map<String,EBSInfo> ebs_volumes,
                         Map<String,ImageInfo> images,
                         Map<String,SnapshotInfo> snapshots,
                         Map<String,DBInstanceInfo> rds_instances,
                         String ownerId) {
}
