package awscanner.route53;

import software.amazon.awssdk.services.route53.Route53Client;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {

    public static Map<String, ZoneInfo> scanZones(Route53Client client ) {
        return client.listHostedZones().hostedZones().stream()
            .map( i -> new ZoneInfo(
                i.id(),
                i.name(),
                i.config().comment(),
                i.config().privateZone()
            ) )
            .collect( Collectors.toUnmodifiableMap( ZoneInfo::id, identity() ) );
    }
}
