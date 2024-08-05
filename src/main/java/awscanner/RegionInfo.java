package awscanner;

import awscanner.ec2.*;
import awscanner.efs.EFSInfo;
import awscanner.rds.DBInstanceInfo;
import awscanner.route53.ZoneInfo;
import software.amazon.awssdk.regions.Region;

import java.util.Map;


public record RegionInfo(Region region,
                         Map<String,InstanceInfo> instances,
                         Map<String,EBSInfo> ebs_volumes,
                         Map<String,ImageInfo> images,
                         Map<String,SnapshotInfo> snapshots,
                         Map<String,EFSInfo> efs,
                         Map<String,DBInstanceInfo> rds_instances,
                         Map<String, ZoneInfo> route53_zones,
                         Map<String, CFDistributionInfo> cf_dists,
                         String ownerId,
                         boolean pricing_enabled) {
}
