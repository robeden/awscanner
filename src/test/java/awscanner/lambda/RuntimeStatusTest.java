package awscanner.lambda;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.Runtime;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;


class RuntimeStatusTest {
    @Test
    void testPastDeprecation() {
        assertEquals(RuntimeStatus.DEPRECATED, RuntimeStatus.getStatus(Runtime.DOTNETCORE1_0));
    }

    @Test
    void testTimedDeprecation() {
        assertEquals(RuntimeStatus.OKAY,
            RuntimeStatus.getStatus(Runtime.PYTHON3_9, LocalDate.parse("2020-01-01")));


        assertEquals(RuntimeStatus.DEPRECATED_SOON,
            RuntimeStatus.getStatus(Runtime.PYTHON3_9, LocalDate.parse("2025-06-06")));
        assertEquals(RuntimeStatus.DEPRECATED_SOON,
            RuntimeStatus.getStatus(Runtime.PYTHON3_9, LocalDate.parse("2025-12-14")));

        assertEquals(RuntimeStatus.DEPRECATED,
            RuntimeStatus.getStatus(Runtime.PYTHON3_9, LocalDate.parse("2025-12-15")));
        assertEquals(RuntimeStatus.DEPRECATED,
            RuntimeStatus.getStatus(Runtime.PYTHON3_9, LocalDate.parse("2026-01-01")));
    }
}
