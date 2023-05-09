package awscanner.ec2;

import awscanner.price.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.pricing.model.PricingException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {
    private static final AtomicBoolean DISABLE_PRICING = new AtomicBoolean( false );

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
                daysSinceInstant( s.startTime(), now )
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


    public static boolean isPricingEnabled() {
        return ! DISABLE_PRICING.get();
    }


    private static Map<String,String> ec2TagListToMap( List<Tag> list ) {
        return list.stream().collect( Collectors.toUnmodifiableMap(
            Tag::key, Tag::value ) );
    }


    private static Optional<PricingEstimation.PriceSpecs> lookupCost( PricingEstimation pricing,
        ResourcePriceAttributes<?> attributes ) {

        Optional<PricingEstimation.PriceSpecs> cph = Optional.empty();
        if ( DISABLE_PRICING.get() ) return cph;

        Future<Optional<PricingEstimation.PriceSpecs>> cph_future = pricing.findPrice(attributes);
        try {
            cph = cph_future.get();
        }
        catch ( Exception e ) {
            boolean handled = false;
            if ( e.getCause() instanceof PricingException ) {
                if ( ( ( PricingException ) e.getCause() ).statusCode() == 400 ) {
                    DISABLE_PRICING.set( true );
                    System.err.println( "Pricing lookups disabled due to lookup permission error: " +
                        e.getCause() );
                    handled = true;
                }
            }

            if ( !handled ) e.printStackTrace();
        }
        return cph;
    }


    private static InstanceInfo buildInstanceInfo( Instance i, String region, PricingEstimation pricing ) {

        EC2PriceAttributes price_attributes = new EC2PriceAttributes( region,
            i.instanceTypeAsString(), platformToOS( i.platformDetails() ),
            false );          // TODO: figure out how to get this

        Optional<PricingEstimation.PriceSpecs> cph = lookupCost( pricing, price_attributes );

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
                cph.map( ps -> price_attributes.convertToPerHour( ps.price(), ps.unit() ) ) );
    }

    private static EBSInfo buildVolumeInfo( Volume v, LocalDate now, String region,
        PricingEstimation pricing ) {

        Optional<EBSPriceAttributes> price_attributes =
            EBSPriceAttributes.UsageType.findByApiIdentifier( v.volumeTypeAsString() )
                .map( type -> new EBSPriceAttributes( region, type ) );

        Optional<PricingEstimation.PriceSpecs> cph = price_attributes
            .flatMap( att -> lookupCost( pricing, att ) );

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
            daysSinceInstant( v.createTime(), now ),
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


    private static int daysSinceInstant( Instant instant, LocalDate now ) {
        return ( int ) ChronoUnit.DAYS.between(
            instant.atZone( ZoneId.systemDefault() ).toLocalDate(), now );
    }
}
