package awscanner.ec2;

import awscanner.util.ResourceInfo;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public record ImageInfo(String id,
                        Map<String,String> tags,
                        String name,
                        String architecture,
                        String kernel,
                        String platform,
                        String image_type,
                        String hypervisor,
                        String virtualization_type,
                        String tpm_support,
                        String boot_mode,
                        String state,
                        Boolean public_launch_permissions,
                        String creation_date,
                        Set<String> block_device_mapping_snapshot_ids) implements ResourceInfo {

    @Override
    public Stream<String> usesIds() {
        return block_device_mapping_snapshot_ids.stream();
    }
}
