package awscanner;

import awscanner.ec2.EBSInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.InstanceState;
import awscanner.ec2.SnapshotInfo;
import awscanner.rds.DBInstanceInfo;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBSecurityGroupMembership;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class RegionScanner implements Callable<RegionInfo> {
	private final Region region;
	private final Ec2Client ec2_client;
	private final RdsClient rds_client;
	private final ExecutorService executor;


	public RegionScanner( Region region, AwsCredentialsProvider creds, ExecutorService executor ) {
		this.region = region;
		ec2_client = Ec2Client.builder()
			.region( region )
			.credentialsProvider( creds )
			.build();
		rds_client = RdsClient.builder()
			.region( region )
			.credentialsProvider( creds )
			.build();
		this.executor = executor;
	}


	@Override
	public RegionInfo call() throws Exception {
		return scan( executor );
	}


	public RegionInfo scan( ExecutorService executor ) throws Exception {
		Future<Map<String,SnapshotInfo>> snapshot_future = executor.submit( this::scanSnapshots );
		Future<Map<String,EBSInfo>> volume_future = executor.submit( this::scanVolumes );
		Future<Map<String,DBInstanceInfo>> db_instance_future = executor.submit( this::scanRdsInstances );
		return new RegionInfo(
			region,
			scanInstances(),
			volume_future.get(),
			snapshot_future.get(),
			db_instance_future.get() );
	}


	private Map<String,InstanceInfo> scanInstances() {
		return ec2_client.describeInstancesPaginator().stream()
			.flatMap( r -> r.reservations().stream() )
			.flatMap( r -> r.instances().stream() )
			.map( i -> new InstanceInfo(
				i.instanceId(),
				ec2TagListToMap( i.tags() ),
				InstanceState.findByCode( i.state().code() ).orElse( null ),
				i.imageId(),
				i.privateIpAddress(),
				i.publicIpAddress(),
				i.subnetId(),
				i.vpcId(),
				i.architectureAsString(),
				i.capacityReservationId(),
				i.launchTime(),
				i.blockDeviceMappings().stream()
					.map( ibdm -> ibdm.ebs().volumeId() )
					.collect( Collectors.toSet()),
				i.securityGroups().stream().map( GroupIdentifier::groupId ).collect( Collectors.toSet()) ) )
			.collect( Collectors.toUnmodifiableMap( InstanceInfo::id, identity()) );
	}


	private Map<String,SnapshotInfo> scanSnapshots() {
		return Map.of();            // TODO
	}


	private Map<String,EBSInfo> scanVolumes() {
		return ec2_client.describeVolumesPaginator().stream()
			.flatMap( r -> r.volumes().stream() )
			.map( v -> new EBSInfo( v.volumeId(),
				ec2TagListToMap( v.tags() ),
				v.attachments().stream().map( VolumeAttachment::volumeId ).collect( Collectors.toSet()),
				v.snapshotId(),
				v.stateAsString(),
				v.encrypted(),
				v.size(),
				v.iops(),
				v.throughput(),
				v.volumeTypeAsString() ) )
			.collect( Collectors.toUnmodifiableMap( EBSInfo::id, identity() ) );
	}


	private Map<String,DBInstanceInfo> scanRdsInstances() {
		return rds_client.describeDBInstancesPaginator().stream()
			.flatMap( r -> r.dbInstances().stream() )
			.map( i -> new DBInstanceInfo(
				i.dbInstanceIdentifier(),
				rdsTagListToMap( i.tagList() ),
				i.dbName(),
				i.dbInstanceClass(),
				i.dbInstanceStatus(),
				i.dbClusterIdentifier(),
				i.publiclyAccessible(),
				i.dbSecurityGroups().stream()
					.map( DBSecurityGroupMembership::dbSecurityGroupName )
					.collect( Collectors.toSet() ),
				i.vpcSecurityGroups().stream()
					.map( VpcSecurityGroupMembership::vpcSecurityGroupId )
					.collect( Collectors.toSet() ),
				i.engine(),
				i.engineVersion(),
				i.iops(),
				i.allocatedStorage(),
				i.storageType(),
				i.storageEncrypted(),
				i.instanceCreateTime()
			) )
			.collect( Collectors.toUnmodifiableMap( DBInstanceInfo::id, identity() ) );
	}


	private static Map<String,String> ec2TagListToMap( List<Tag> list ) {
		return list.stream().collect( Collectors.toUnmodifiableMap(
			Tag::key, Tag::value ) );
	}

	private static Map<String,String> rdsTagListToMap(
		List<software.amazon.awssdk.services.rds.model.Tag> list ) {
		return list.stream().collect( Collectors.toUnmodifiableMap(
			software.amazon.awssdk.services.rds.model.Tag::key,
			software.amazon.awssdk.services.rds.model.Tag::value ) );
	}
}
