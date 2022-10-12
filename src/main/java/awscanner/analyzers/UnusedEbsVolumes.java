package awscanner.analyzers;

import awscanner.ColorWriter;
import awscanner.RegionInfo;
import awscanner.ec2.EBSInfo;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class UnusedEbsVolumes {
    /**
     * @return          IDs of unused volumes.
     */
    public static Set<String> analyze( RegionInfo region_info, ColorWriter writer,
        Ec2Client client ) {

        Set<String> volumes_in_use = new HashSet<>();
        // In use directly by active instances
        region_info.instances().values().stream()
            .flatMap( i -> i.volume_ids().stream() )
            .forEach( volumes_in_use::add );
        // In use by AMIs
        region_info.images().values().stream()
            .flatMap( i -> i.block_device_mapping_snapshot_ids().stream() )
            .forEach( volumes_in_use::add );


        List<EBSInfo> unused_volumes = region_info.ebs_volumes().values().stream()
            .filter( ebs -> !volumes_in_use.contains( ebs.id() ) )
            .sorted( Comparator.comparing( EBSInfo::id ) )
            .toList();

        if ( unused_volumes.isEmpty() ) return Set.of();

        writer.println( "Unused EBS (" + unused_volumes.size() + "):" );
        for ( var ebs : unused_volumes ) {
            if ( ebs.isAttached() ) {
                throw new RuntimeException("Logic error: attached  - " + ebs.id() );
            }
            ColorWriter.Color color = null;
            String suffix = "";
            if ( ebs.tags().isEmpty() ) {
                color = ColorWriter.Color.RED;
                suffix = " (untagged)";
            }
            else {
                suffix = " (" + ebs.tags().entrySet().stream()
                    .map( e -> e.getKey() + "=" + e.getValue() )
                    .sorted( String.CASE_INSENSITIVE_ORDER )
                    .collect( Collectors.joining( "," ) ) + ")";
            }
            writer.println( "    " + ebs.id() + " - " + ebs.size() + " GB, " +
                ebs.days_since_creation() + " days old" +
                suffix, color );
        }
        return unused_volumes.stream()
            .map( EBSInfo::id )
            .collect( Collectors.toSet() );
    }
}
