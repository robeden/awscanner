package awscanner.ec2;

import java.util.List;

public record CFDistributionInfo(
    String id,
    String domain_name,
    String comment,
    boolean enabled,
    List<String> aliases) {
}
