package awscanner.efs;

import awscanner.price.PriceResults;
import awscanner.price.PricingEstimation;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static awscanner.util.UtilFunctions.daysSinceInstant;
import static java.util.function.Function.identity;


public class ScanFunctions {
    public static Map<String,EFSInfo> scanFileSystems( EfsClient client,
        String region, PricingEstimation pricing ) {

        final LocalDate now = LocalDate.now();
        return client.describeFileSystemsPaginator().stream()
            .flatMap( r -> r.fileSystems().stream() )
            .map( fs -> buildFilesystemInfo( fs, now, region, pricing ) )
            .collect( Collectors.toUnmodifiableMap( EFSInfo::id, identity() ) );
    }


    private static EFSInfo buildFilesystemInfo( FileSystemDescription fs, LocalDate now, String region,
        PricingEstimation pricing ) {

        Optional<PriceResults> cph = Optional.empty();
//        Optional<PriceResults> cph =
//            EFSPriceAttributes.UsageType.findByApiIdentifier( v.volumeTypeAsString() )
//                .flatMap( type -> pricing.lookupCost(
//                    new EFSPriceAttributes(
//                        region,
//                        type ) ) );

        return new EFSInfo(
            fs.fileSystemId(),
            fs.name(),
            fs.sizeInBytes().value(),
            fs.encrypted(),
            fs.performanceModeAsString(),
            fs.throughputModeAsString(),
            fs.provisionedThroughputInMibps(),
            fs.lifeCycleStateAsString(),
            daysSinceInstant( fs.creationTime(), now ),
            cph.orElse( null ) );
    }
}
