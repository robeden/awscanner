package awscanner.report;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class OwnerReportTest {


    @ParameterizedTest
    @MethodSource( "splitTagValue_args" )
    void splitTagValue( String tag_value, String split_by, Set<String> expected ) {
        assertEquals( expected, OwnerReport.splitTagValue( tag_value, split_by ) );
    }


    static Stream<Arguments> splitTagValue_args() {
        return Stream.of(
            arguments( null, " ", Set.of() ),
            arguments( "", " ", Set.of() ),
            arguments( "rob.eden", " ", Set.of( "rob.eden" ) ),
            arguments( "rob.eden testy-mctester", " ",
                Set.of( "rob.eden", "testy-mctester" ) ),
            arguments( "rob.eden testy-mctester me@example.com", " ",
                Set.of( "rob.eden", "testy-mctester", "me@example.com" ) ),

            arguments( "a/b", "/",  Set.of( "a", "b" ) )
        );
    }
}
