package awscanner.ec2;

import awscanner.price.EBSPriceAttributes;
import awscanner.price.EC2PriceAttributes;
import awscanner.price.PricingEstimation;
import awscanner.util.UtilFunctions;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {
    public static Map<String,InstanceInfo> scanEc2Instances( Ec2Client client,
        String region, PricingEstimation pricing ) {

        Map<String,SecurityGroup> security_group_map = new HashMap<>();
        Function<String,SecurityGroup> security_group_lookup = group_id ->
            client.describeSecurityGroups(
                DescribeSecurityGroupsRequest.builder()
                    .groupIds(group_id)
                    .build()
            ).securityGroups().get(0);

        return client.describeInstancesPaginator().stream()
            .flatMap( r -> r.reservations().stream() )
            .flatMap( r -> r.instances().stream() )
//            .peek( System.out::println )
            .parallel()                     // parallel due to pricing lookups in buildInstanceInfo
            .map( i -> buildInstanceInfo(
                i,
                region,
                pricing,
                security_group_map,
                security_group_lookup
            ) )
            .collect( Collectors.toUnmodifiableMap( InstanceInfo::id, identity() ) );
    }


    public static Map<String,EBSInfo> scanVolumes( Ec2Client client,
        String region, PricingEstimation pricing ) {

        final LocalDate now = LocalDate.now();
        return client.describeVolumesPaginator().stream()
            .flatMap( r -> r.volumes().stream() )
            .map( v -> buildVolumeInfo( v, now, region, pricing ) )
            .collect( Collectors.toUnmodifiableMap( EBSInfo::id, identity() ) );
    }


    public static Map<String,SnapshotInfo> scanSnapshots( String owner, Ec2Client client ) {
        final LocalDate now = LocalDate.now();
        var request = DescribeSnapshotsRequest.builder()
            .ownerIds( owner )
            .build();
        return client.describeSnapshotsPaginator( request ).stream()
            .flatMap( s -> s.snapshots().stream() )
            .map( s -> new SnapshotInfo(
                s.snapshotId(),
                ec2TagListToMap( s.tags() ),
                s.volumeId(),
                s.ownerId(),
                s.ownerAlias(),
                s.description(),
                s.storageTierAsString(),
                UtilFunctions.daysSinceInstant( s.startTime(), now )
            ) )
            .collect( Collectors.toUnmodifiableMap( SnapshotInfo::id, identity() ) );
    }


    public static Map<String,ImageInfo> scanImages( String owner, Ec2Client client ) {
        var request = DescribeImagesRequest.builder()
            .owners( owner )
            .build();

        return client.describeImages( request ).images().stream()
            .map( i -> new ImageInfo(
                i.imageId(),
                ec2TagListToMap( i.tags() ),
                i.name(),
                i.architectureAsString(),
                i.kernelId(),
                i.platformAsString(),
                i.imageTypeAsString(),
                i.hypervisorAsString(),
                i.virtualizationTypeAsString(),
                i.tpmSupportAsString(),
                i.bootModeAsString(),
                i.stateAsString(),
                i.publicLaunchPermissions(),
                i.creationDate(),
                i.blockDeviceMappings().stream()
                    .filter( bdm -> bdm.ebs() != null )
                    .map( bdm -> bdm.ebs().snapshotId() )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() )
            ) )
            .collect( Collectors.toUnmodifiableMap( ImageInfo::id, identity() ) );
    }


    public static Map<String,CFDistributionInfo> scanCloudfrontDistributions(CloudFrontClient client) {
        ListDistributionsResponse response = client.listDistributions();
        return response.distributionList().items().stream()
            .map( d -> new CFDistributionInfo(
                d.id(),
                d.domainName(),
                d.comment(),
                d.enabled(),
                d.aliases().items()
            ))
            .collect(Collectors.toUnmodifiableMap( CFDistributionInfo::id, identity() ) );
    }


    private static Map<String,String> ec2TagListToMap( List<Tag> list ) {
        return list.stream().collect( Collectors.toUnmodifiableMap(
            Tag::key, Tag::value ) );
    }


    private static InstanceInfo buildInstanceInfo( Instance i, String region, PricingEstimation pricing,
                                                   Map<String,SecurityGroup> security_group_map,
                                                   Function<String,SecurityGroup> security_group_lookup) {

        EC2PriceAttributes price_attributes = new EC2PriceAttributes( region,
            i.instanceTypeAsString(), platformToOS( i.platformDetails() ),
            false );          // TODO: figure out how to get this

        Optional<PricingEstimation.PriceSpecs> cph = pricing.lookupCost( price_attributes );

        return new InstanceInfo(
                i.instanceId(),
                ec2TagListToMap( i.tags() ),
                i.instanceTypeAsString(),
                InstanceState.findByCode( i.state().code() ).orElse( null ),
                i.imageId(),
                i.privateIpAddress(),
                i.publicIpAddress(),
                i.subnetId(),
                i.vpcId(),
                i.securityGroups().stream()
                    .map(id -> security_group_map.computeIfAbsent(id.groupId(), security_group_lookup))
                    .collect(Collectors.toUnmodifiableMap(SecurityGroup::groupId, Function.identity())),
                i.architectureAsString(),
                i.platformAsString(),
                i.capacityReservationId(),
                i.launchTime(),
                i.blockDeviceMappings().stream()
                    .map( ibdm -> ibdm.ebs().volumeId() )
                    .collect( Collectors.toSet() ),
                i.securityGroups().stream().map( GroupIdentifier::groupId ).collect( Collectors.toSet() ),
                i.licenses(),
                cph.map( ps -> price_attributes.convertToPerHour( ps.price(), ps.unit() ) ) );
    }

    private static EBSInfo buildVolumeInfo( Volume v, LocalDate now, String region,
        PricingEstimation pricing ) {

        Optional<EBSPriceAttributes> price_attributes =
            EBSPriceAttributes.UsageType.findByApiIdentifier( v.volumeTypeAsString() )
                .map( type -> new EBSPriceAttributes( region, type ) );

        Optional<PricingEstimation.PriceSpecs> cph = price_attributes
            .flatMap(pricing::lookupCost);

        return new EBSInfo(
            v.volumeId(),
            ec2TagListToMap( v.tags() ),
            v.attachments().stream().map( VolumeAttachment::volumeId ).collect( Collectors.toSet() ),
            v.snapshotId(),
            v.stateAsString(),
            v.encrypted(),
            v.size(),
            v.iops(),
            v.throughput(),
            v.volumeTypeAsString(),
            UtilFunctions.daysSinceInstant( v.createTime(), now ),
            // NOTE: The get() is okay because cph will be empty if price_attributes was empty
            cph.map( spec -> price_attributes.get()
                .convertToPerHour( spec.price(), spec.unit(), v.size() ) ) );
    }

    private static EC2PriceAttributes.OperatingSystem platformToOS( String platform_details ) {
        switch ( platform_details.toUpperCase() ) {
            case "WINDOWS" -> {
                return EC2PriceAttributes.OperatingSystem.WINDOWS;
            }
            case "LINUX/UNIX", "RED HAT ENTERPRISE LINUX" -> {
                return EC2PriceAttributes.OperatingSystem.LINUX;
            }
            default -> {
                System.err.println( "Unknown platform: " + platform_details );
                return EC2PriceAttributes.OperatingSystem.LINUX;
            }
        }
    }
}
