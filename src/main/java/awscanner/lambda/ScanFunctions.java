package awscanner.lambda;

import software.amazon.awssdk.services.lambda.LambdaClient;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


public class ScanFunctions {
    public static Map<String, LambdaInfo> scanLambdaFunctions(LambdaClient lambda_client ) {
        return lambda_client.listFunctionsPaginator().stream()
            .flatMap( f -> f.functions().stream() )
            .map( f -> new LambdaInfo(
                f.functionName(),
                f.functionArn(),
                f.lastModified(),
                f.memorySize(),
                f.runtime()
            ) )
            .collect(Collectors.toUnmodifiableMap( LambdaInfo::function_name, identity() ));
    }
}
