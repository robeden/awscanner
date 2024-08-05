package awscanner;

import awscanner.ec2.*;
import awscanner.efs.EFSInfo;
import awscanner.price.PricingEstimation;
import awscanner.rds.DBInstanceInfo;
import awscanner.route53.ZoneInfo;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.route53.Route53Client;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static awscanner.ec2.ScanFunctions.*;
import static awscanner.efs.ScanFunctions.scanFileSystems;
import static awscanner.rds.ScanFunctions.scanRdsInstances;
import static awscanner.route53.ScanFunctions.scanZones;


public class RegionScanner implements Callable<RegionInfo> {
    private final Region region;
    private final CloudFrontClient cf_client;
    private final Ec2Client ec2_client;
    private final EfsClient efs_client;
    private final PricingClient pricing_client;
    private final RdsClient rds_client;
    private final Route53Client route53_client;
    private final ExecutorService executor;
    private final String owner_id;


    public RegionScanner( Region region, AwsCredentialsProvider creds, AwsCredentialsProvider pricing_creds,
        ExecutorService executor, String owner_id ) {

        this.region = region;
        this.owner_id = owner_id;
        cf_client = CloudFrontClient.builder()
            .region( region )
            .credentialsProvider( creds )
            .build();
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
        route53_client = Route53Client.builder()
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

        Future<Map<String, CFDistributionInfo>> cloudfront_future = emptyFuture();
        Future<Map<String,InstanceInfo>> instance_future = emptyFuture();
        Future<Map<String,ImageInfo>> image_future = emptyFuture();
        Future<Map<String,SnapshotInfo>> snapshot_future = emptyFuture();
        Future<Map<String,EBSInfo>> volume_future = emptyFuture();
        Future<Map<String,EFSInfo>> efs_future = emptyFuture();
        Future<Map<String, DBInstanceInfo>> rds_future = emptyFuture();
        Future<Map<String, ZoneInfo>> route53_zone_future = emptyFuture();

        if (region.equals(Region.AWS_GLOBAL) || region.equals(Region.AWS_US_GOV_GLOBAL)) {
            route53_zone_future =
                executor.submit(() -> scanZones(route53_client));
            if (region.equals(Region.AWS_GLOBAL)) {
                cloudfront_future =
                    executor.submit(() -> scanCloudfrontDistributions(cf_client));
            }
        }
        else {
            instance_future =
                executor.submit( () -> scanEc2Instances( ec2_client, region.id(), pricing ) );
            image_future =
                executor.submit( () -> scanImages( owner_id, ec2_client ) );
            snapshot_future =
                executor.submit( () -> scanSnapshots( owner_id, ec2_client ) );
            volume_future =
                executor.submit( () -> scanVolumes( ec2_client, region.id(), pricing ) );
            efs_future =
                executor.submit( () -> scanFileSystems( efs_client, region.id(), pricing ) );
            rds_future =
                executor.submit( () -> scanRdsInstances( rds_client ) );
        }

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
            rds_future.get(),
            route53_zone_future.get(),
            cloudfront_future.get(),
            owner_id,
            !pricing.disabled() );
    }


    private static <K,V> Future<Map<K,V>> emptyFuture() {
        CompletableFuture<Map<K,V>> future = new CompletableFuture<>();
        future.complete(Map.of());
        return future;
    }
}
