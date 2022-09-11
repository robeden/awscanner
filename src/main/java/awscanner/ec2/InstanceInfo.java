package awscanner.ec2;

import java.time.Instant;
import java.util.Map;
import java.util.Set;


public record InstanceInfo(String id,
                           Map<String,String> tags,
                           InstanceState state,
                           String image_id,
                           String private_ip,
                           String public_ip,
                           String subnet_id,
                           String vpc_id,
                           String arch,
                           String reservation_id,
                           Instant launch_time,
                           Set<String> volume_ids,
                           Set<String> security_group_ids) {

	public boolean isRunning() {
		return state == InstanceState.RUNNING || state == InstanceState.PENDING;
	}
}
