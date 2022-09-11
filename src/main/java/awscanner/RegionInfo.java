package awscanner;

import awscanner.ec2.EBSInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;
import awscanner.rds.DBInstanceInfo;
import software.amazon.awssdk.regions.Region;

import java.util.Map;


public record RegionInfo(Region region,
                         Map<String,InstanceInfo> instances,
                         Map<String,EBSInfo> ebs_volumes,
                         Map<String,SnapshotInfo> snapshots,
                         Map<String,DBInstanceInfo> rds_instances ) {


	@Override
	public String toString() {
		return "Region " + region +
			"\n  EC2 Instances: " + instances +
			"\n  EBS Volumes: " + ebs_volumes +
			"\n  EC2 Snapshots: " + snapshots +
			"\n  RDS Instances: " + rds_instances;
	}
}
