package awscanner.analyzers;

import awscanner.ColorWriter;
import awscanner.RegionInfo;
import awscanner.ec2.EBSInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class UnusedEbsVolumes {
    public static void report( RegionInfo region_info, ColorWriter writer ) {
        Set<String> images_in_use = region_info.instances().values().stream()
            .map( InstanceInfo::image_id )
            .collect( Collectors.toSet() );

        Set<String> volumes_in_use = new HashSet<>();
        // In use directly by active instances
        region_info.instances().values().stream()
            .flatMap( i -> i.volume_ids().stream() )
            .forEach( volumes_in_use::add );
        // In use by snapshots
        region_info.snapshots().values().stream()
            .map( SnapshotInfo::volume_id )
            .forEach( volumes_in_use::add );
        // In use by AMIs
        region_info.images().values().stream()
            .flatMap( i -> i.block_device_mapping_snapshot_ids().stream() )
            .forEach( volumes_in_use::add );


        boolean found_one = false;
        for ( EBSInfo ebs : region_info.ebs_volumes().values() ) {
            if ( volumes_in_use.contains( ebs.id() ) ) continue;

            if ( !found_one ) {
                // Header
                writer.println( "Unused EBS:" );
                found_one = true;
            }

            writer.println( "    " + ebs.id() );
        }

//        if ( !found_one ) writer.println( "  None found." );
    }
}
