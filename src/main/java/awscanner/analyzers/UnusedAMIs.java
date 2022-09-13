package awscanner.analyzers;

import awscanner.ColorWriter;
import awscanner.RegionInfo;
import awscanner.ec2.EBSInfo;
import awscanner.ec2.ImageInfo;
import awscanner.ec2.InstanceInfo;
import awscanner.ec2.SnapshotInfo;

import java.util.Set;
import java.util.stream.Collectors;


public class UnusedAMIs {
    public static void report( RegionInfo region_info, ColorWriter writer ) {
        Set<String> images_in_use = region_info.instances().values().stream()
            .map( InstanceInfo::image_id )
            .collect( Collectors.toSet() );

        boolean found_one = false;
        for ( ImageInfo image : region_info.images().values() ) {
            if ( images_in_use.contains( image.id() ) ) continue;

            found_one = true;
            writer.print( "  " );
            ColorWriter.Color color = ColorWriter.RED;
            if ( image.public_launch_permissions() ) {
                writer.print( "[PUBLIC] ", ColorWriter.YELLOW );
                color = ColorWriter.YELLOW;
            }
            writer.println( image.id(), color );


            for ( String snapshot_id : image.block_device_mapping_snapshot_ids() ) {
                SnapshotInfo snapshot = region_info.snapshots().get( snapshot_id );
                if ( snapshot == null ) continue;

                writer.println( "    " + snapshot_id, ColorWriter.RED );

                EBSInfo volume = region_info.ebs_volumes().get( snapshot.volume_id() );
                if ( volume != null ) {
                    writer.println( "      " + volume.id(), ColorWriter.RED );
                }
            }
        }

        if ( !found_one ) writer.println( "  None found." );
    }
}
