package awscanner.analyzers;

import awscanner.ColorWriter;
import awscanner.RegionInfo;
import awscanner.ec2.EBSInfo;
import awscanner.ec2.SnapshotInfo;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.*;
import java.util.stream.Collectors;


public class UnusedSnapshots {
    /**
     * @return          IDs of unused volumes.
     */
    public static Set<String> analyze( RegionInfo region_info, ColorWriter writer,
        Ec2Client client ) {

        Set<String> snapshots_in_use = new HashSet<>();
        region_info.ebs_volumes().values().stream()
            .map( EBSInfo::snapshot_id )
            .filter( Objects::nonNull )
            .forEach( snapshots_in_use::add );

        List<SnapshotInfo> unused_snapshots = region_info.snapshots().values().stream()
            .filter( s -> snapshots_in_use.contains( s.id() ) )
            .sorted( Comparator.comparing( SnapshotInfo::id ) )
            .toList();

        if ( unused_snapshots.isEmpty() ) return Set.of();

        writer.println( "Unused snapshots (" + unused_snapshots.size() + "):" );
        for ( var snapshot : unused_snapshots ) {
            ColorWriter.Color color = null;
            String suffix = "";
            if ( snapshot.tags().isEmpty() ) {
                color = ColorWriter.Color.RED;
                suffix = " (untagged)";
            }
            else {
                suffix = " (" + snapshot.tags().entrySet().stream()
                    .map( e -> e.getKey() + "=" + e.getValue() )
                    .sorted( String.CASE_INSENSITIVE_ORDER )
                    .collect( Collectors.joining( "," ) ) + ")";
            }
            writer.println( "    " + snapshot.id() + " - " +
                snapshot.days_since_creation() + " days old" + suffix, color );
        }
        return unused_snapshots.stream()
            .map( SnapshotInfo::id )
            .collect( Collectors.toSet() );
    }
}
