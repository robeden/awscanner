package awscanner.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;


public class UtilFunctions {
    public static int daysSinceInstant( Instant instant, LocalDate now ) {
        return ( int ) ChronoUnit.DAYS.between(
            instant.atZone( ZoneId.systemDefault() ).toLocalDate(), now );
    }
}
