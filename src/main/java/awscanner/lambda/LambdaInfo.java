package awscanner.lambda;

import software.amazon.awssdk.services.lambda.model.Runtime;

public record LambdaInfo(String function_name,
                         String arn,
                         String last_modified,
                         Integer memory_size,
                         Runtime runtime) {

    public RuntimeStatus getRuntimeStatus() {
        return RuntimeStatus.getStatus(runtime);
    }
}
