package awscanner.ec2;

import awscanner.price.EC2PriceAttributes;
import awscanner.price.PriceResults;
import awscanner.price.PricingEstimation;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {
    public static Map<String,InstanceInfo> scanEc2Instances( Ec2Client client,
        String region, PricingEstimation pricing ) {

        return client.describeInstancesPaginator().stream()
            .flatMap( r -> r.reservations().stream() )
            .flatMap( r -> r.instances().stream() )
//            .peek( System.out::println )
            .parallel()                     // parallel due to pricing lookups in buildInstanceInfo
            .map( i -> buildInstanceInfo( i, region, pricing ) )
            .collect( Collectors.toUnmodifiableMap( InstanceInfo::id, identity() ) );
    }


    public static Map<String,EBSInfo> scanVolumes( Ec2Client client ) {
        return client.describeVolumesPaginator().stream()
            .flatMap( r -> r.volumes().stream() )
            .map( v -> new EBSInfo( v.volumeId(),
                ec2TagListToMap( v.tags() ),
                v.attachments().stream().map( VolumeAttachment::volumeId ).collect( Collectors.toSet() ),
                v.snapshotId(),
                v.stateAsString(),
                v.encrypted(),
                v.size(),
                v.iops(),
                v.throughput(),
                v.volumeTypeAsString() ) )
            .collect( Collectors.toUnmodifiableMap( EBSInfo::id, identity() ) );
    }


    public static Map<String,SnapshotInfo> scanSnapshots( String owner, Ec2Client client ) {
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
                s.storageTierAsString()
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


    private static Map<String,String> ec2TagListToMap( List<Tag> list ) {
        return list.stream().collect( Collectors.toUnmodifiableMap(
            Tag::key, Tag::value ) );
    }


    private static InstanceInfo buildInstanceInfo( Instance i, String region, PricingEstimation pricing ) {
        Future<Optional<PriceResults>> cph_future = pricing.findCostPerHour(
            new EC2PriceAttributes(
                region,
                i.instanceTypeAsString(),
                platformToOS( i.platformDetails() ),
                false ) );          // TODO: figure out how to get this
        Optional<PriceResults> cph = Optional.empty();
        try {
            cph = cph_future.get();
        }
        catch ( Exception e ) {
            // ignore
            e.printStackTrace();
        }

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
                i.architectureAsString(),
                i.platformAsString(),
                i.capacityReservationId(),
                i.launchTime(),
                i.blockDeviceMappings().stream()
                    .map( ibdm -> ibdm.ebs().volumeId() )
                    .collect( Collectors.toSet() ),
                i.securityGroups().stream().map( GroupIdentifier::groupId ).collect( Collectors.toSet() ),
                i.licenses(),
                cph.orElse( null ) );
    }

    private static EC2PriceAttributes.OperatingSystem platformToOS( String platform_details ) {
        switch( platform_details.toUpperCase() ) {
            case "WINDOWS":
                return EC2PriceAttributes.OperatingSystem.WINDOWS;
            case "LINUX/UNIX":
            case "RED HAT ENTERPRISE LINUX":
                return EC2PriceAttributes.OperatingSystem.LINUX;
            default:
                System.err.println("Unknown platform: " + platform_details);
                return EC2PriceAttributes.OperatingSystem.LINUX;
        }
    }
}
