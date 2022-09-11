package awscanner.ec2;

import java.util.Map;
import java.util.Set;


public record EBSInfo(String id,
                      Map<String,String> tags,
                      Set<String> attached_instances,
                      String snapshot_id,
                      String state,
                      Boolean encrypted,
                      Integer size,
                      Integer iops,
                      Integer throughput,
                      String type) {
}
