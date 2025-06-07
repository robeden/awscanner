package awscanner.lambda;

import software.amazon.awssdk.services.lambda.model.Runtime;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import static java.util.Map.entry;

public enum RuntimeStatus {
    DEPRECATED,
    DEPRECATED_SOON,
    OKAY;


    private static Map<Runtime, LocalDate> DEPRECATION_DATES = Map.ofEntries(
        entry(Runtime.NODEJS22_X, LocalDate.parse("2027-04-30")),
        entry(Runtime.NODEJS20_X, LocalDate.parse("2026-04-30")),
        entry(Runtime.NODEJS18_X, LocalDate.parse("2025-09-01")),
        entry(Runtime.PYTHON3_13, LocalDate.parse("2029-06-30")),
        entry(Runtime.PYTHON3_12, LocalDate.parse("2028-10-31")),
        entry(Runtime.PYTHON3_11, LocalDate.parse("2026-06-30")),
        entry(Runtime.PYTHON3_10, LocalDate.parse("2026-06-30")),
        entry(Runtime.PYTHON3_9, LocalDate.parse("2025-12-15")),
        entry(Runtime.JAVA21, LocalDate.parse("2029-06-30")),
        entry(Runtime.JAVA17, LocalDate.parse("2026-06-30")),
        entry(Runtime.JAVA11, LocalDate.parse("2026-06-30")),
        entry(Runtime.JAVA8_AL2, LocalDate.parse("2026-06-30")),
        entry(Runtime.DOTNET8, LocalDate.parse("2026-11-10")),
//        entry(Runtime.RUBY3_4, LocalDate.parse("")),
        entry(Runtime.RUBY3_3, LocalDate.parse("2027-03-31")),
        entry(Runtime.RUBY3_2, LocalDate.parse("2026-03-31")),
        entry(Runtime.PROVIDED_AL2023, LocalDate.parse("2029-06-30")),
        entry(Runtime.PROVIDED_AL2, LocalDate.parse("2026-06-30"))
    );

    public static RuntimeStatus getStatus(Runtime runtime) {
        return getStatus(runtime, LocalDate.now());
    }

    static RuntimeStatus getStatus(Runtime runtime, LocalDate check_as_of_date) {
        switch(runtime) {
            // See https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html#runtimes-deprecated
            case DOTNET6:
            case PYTHON3_8:
            case NODEJS16_X:
            case JAVA8:
            case GO1_X:
            case PROVIDED:
            case RUBY2_7:
            case NODEJS14_X:
            case PYTHON3_7:
            case DOTNETCORE3_1:
            case NODEJS12_X:
            case PYTHON3_6:
            case DOTNETCORE2_1:
            case NODEJS10_X:
            case RUBY2_5:
            case PYTHON2_7:
            case NODEJS8_10:
            case NODEJS4_3:
            case NODEJS4_3_EDGE:
            case NODEJS6_10:
            case DOTNETCORE1_0:
            case DOTNETCORE2_0:
            case NODEJS:
                return RuntimeStatus.DEPRECATED;
        }

        LocalDate dep_date = DEPRECATION_DATES.get(runtime);
        if (dep_date == null) return RuntimeStatus.OKAY;

        Period duration = Period.between(check_as_of_date, dep_date);
        if (duration.isNegative() || duration.isZero()) return RuntimeStatus.DEPRECATED;

        if (duration.toTotalMonths() <= 12) return RuntimeStatus.DEPRECATED_SOON;

        return RuntimeStatus.OKAY;
    }
}
