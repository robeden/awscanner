package awscanner.ec2;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {
    public static Map<String,InstanceInfo> scanEc2Instances( Ec2Client client ) {
        return client.describeInstancesPaginator().stream()
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
                    .collect( Collectors.toSet() ),
                i.securityGroups().stream().map( GroupIdentifier::groupId ).collect( Collectors.toSet() ) ) )
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
}
