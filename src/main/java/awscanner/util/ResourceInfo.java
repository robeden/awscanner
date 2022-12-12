package awscanner.util;

import java.util.Map;
import java.util.stream.Stream;


public interface ResourceInfo {
    String id();
    Map<String,String> tags();

    /**
     * Return IDs of resources which this resource relies on.
     */
    default Stream<String> usesIds() {
        return Stream.empty();
    }
}
