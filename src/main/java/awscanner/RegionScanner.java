package awscanner;

import awscanner.ec2.EBSInfo;
import awscanner.ec2.ImageInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;
import awscanner.efs.EFSInfo;
import awscanner.price.PricingEstimation;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.rds.RdsClient;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static awscanner.ec2.ScanFunctions.*;
import static awscanner.efs.ScanFunctions.scanFileSystems;
import static awscanner.rds.ScanFunctions.scanRdsInstances;


public class RegionScanner implements Callable<RegionInfo> {
    private final Region region;
    private final Ec2Client ec2_client;
    private final EfsClient efs_client;
    private final PricingClient pricing_client;
    private final RdsClient rds_client;
    private final ExecutorService executor;
    private final String owner_id;


    public RegionScanner( Region region, AwsCredentialsProvider creds, AwsCredentialsProvider pricing_creds,
        ExecutorService executor, String owner_id ) {

        this.region = region;
        this.owner_id = owner_id;
        ec2_client = Ec2Client.builder()
            .region( region )
            .credentialsProvider( creds )
            .build();
        efs_client = EfsClient.builder()
            .region( region )
            .credentialsProvider( creds )
            .build();
        rds_client = RdsClient.builder()
            .region( region )
            .credentialsProvider( creds )
            .build();


        pricing_client = PricingClient.builder()
            .region( Region.US_EAST_1 )//region )
            .credentialsProvider( pricing_creds )
            .build();

        this.executor = executor;
    }


    @Override
    public RegionInfo call() throws Exception {
        return scan( executor, owner_id );
    }


    public RegionInfo scan( ExecutorService executor, String owner_id ) throws Exception {
        PricingEstimation pricing = new PricingEstimation( executor, pricing_client );

        // EC2
        Future<Map<String,InstanceInfo>> instance_future =
            executor.submit( () -> scanEc2Instances( ec2_client, region.id(), pricing ) );
        Future<Map<String,ImageInfo>> image_future =
            executor.submit( () -> scanImages( owner_id, ec2_client ) );
        Future<Map<String,SnapshotInfo>> snapshot_future =
            executor.submit( () -> scanSnapshots( owner_id, ec2_client ) );
        Future<Map<String,EBSInfo>> volume_future =
            executor.submit( () -> scanVolumes( ec2_client, region.id(), pricing ) );
        Future<Map<String,EFSInfo>> efs_future =
            executor.submit( () -> scanFileSystems( efs_client, region.id(), pricing ) );

        // RDS
//        Future<Map<String,DBInstanceInfo>> db_instance_future =
//            executor.submit( () -> scanRdsInstances( rds_client ) );



        return new RegionInfo(
            region,
            instance_future.get(),
            volume_future.get(),
            image_future.get(),
            snapshot_future.get(),
            efs_future.get(),
            scanRdsInstances( rds_client ),
            owner_id );
    }
}
