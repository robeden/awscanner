package awscanner;

import awscanner.ec2.EBSInfo;
import awscanner.ec2.ImageInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static awscanner.ec2.ScanFunctions.*;
import static awscanner.rds.ScanFunctions.scanRdsInstances;


public class RegionScanner implements Callable<RegionInfo> {
    private final Region region;
    private final Ec2Client ec2_client;
    private final RdsClient rds_client;
    private final ExecutorService executor;
    private final String owner_id;


    public RegionScanner( Region region, AwsCredentialsProvider creds, ExecutorService executor,
        String owner_id ) {

        this.region = region;
        this.owner_id = owner_id;
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
        return scan( executor, owner_id );
    }


    public RegionInfo scan( ExecutorService executor, String owner_id ) throws Exception {
        // EC2
        Future<Map<String,InstanceInfo>> instance_future =
            executor.submit( () -> scanEc2Instances( ec2_client ) );
        Future<Map<String,ImageInfo>> image_future =
            executor.submit( () -> scanImages( owner_id, ec2_client ) );
        Future<Map<String,SnapshotInfo>> snapshot_future =
            executor.submit( () -> scanSnapshots( ec2_client ) );
        Future<Map<String,EBSInfo>> volume_future =
            executor.submit( () -> scanVolumes( ec2_client ) );

        // RDS
//        Future<Map<String,DBInstanceInfo>> db_instance_future =
//            executor.submit( () -> scanRdsInstances( rds_client ) );



        return new RegionInfo(
            region,
            instance_future.get(),
            volume_future.get(),
            image_future.get(),
            snapshot_future.get(),
            scanRdsInstances( rds_client ) );
    }
}
