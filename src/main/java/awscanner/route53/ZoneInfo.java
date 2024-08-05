package awscanner.route53;

import software.amazon.awssdk.services.route53.model.HostedZoneConfig;

public record ZoneInfo(
    String id,
    String name,
    String comment,
    boolean is_private ) {
}
