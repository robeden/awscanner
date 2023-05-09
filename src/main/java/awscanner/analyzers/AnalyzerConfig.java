package awscanner.analyzers;

public record AnalyzerConfig(boolean delete_obvious,
                             int obvious_when_unused_for_days) {
}
