package awscanner.graph;

public record GraphResource(String id,
                            ResourceType type,
                            String region,
                            String account) {

}
