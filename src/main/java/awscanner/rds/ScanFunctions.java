package awscanner.rds;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBSecurityGroupMembership;
import software.amazon.awssdk.services.rds.model.Tag;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {


    public static Map<String,DBInstanceInfo> scanRdsInstances( RdsClient rds_client ) {
        return rds_client.describeDBInstancesPaginator().stream()
            .flatMap( r -> r.dbInstances().stream() )
            .map( i -> new DBInstanceInfo(
                i.dbInstanceIdentifier(),
                rdsTagListToMap( i.tagList() ),
                i.dbName(),
                i.dbInstanceClass(),
                i.dbInstanceStatus(),
                i.dbClusterIdentifier(),
                i.publiclyAccessible(),
                i.dbSecurityGroups().stream()
                    .map( DBSecurityGroupMembership::dbSecurityGroupName )
                    .collect( Collectors.toSet() ),
                i.vpcSecurityGroups().stream()
                    .map( VpcSecurityGroupMembership::vpcSecurityGroupId )
                    .collect( Collectors.toSet() ),
                i.engine(),
                i.engineVersion(),
                i.iops(),
                i.allocatedStorage(),
                i.storageType(),
                i.storageEncrypted(),
                i.instanceCreateTime()
            ) )
            .collect( Collectors.toUnmodifiableMap( DBInstanceInfo::id, identity() ) );
    }


    private static Map<String,String> rdsTagListToMap(
        List<Tag> list ) {
        return list.stream().collect( Collectors.toUnmodifiableMap(
            software.amazon.awssdk.services.rds.model.Tag::key,
            software.amazon.awssdk.services.rds.model.Tag::value ) );
    }
}
