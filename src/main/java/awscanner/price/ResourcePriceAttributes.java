package awscanner.price;

import awscanner.util.ResourceInfo;
import software.amazon.awssdk.services.pricing.model.Filter;

import java.math.BigDecimal;
import java.util.Collection;


public interface ResourcePriceAttributes<R extends ResourceInfo> {
    String serviceCode();
    Collection<Filter> buildFilters();

    boolean isUnitExpected( String unit );
    BigDecimal convertToPerHour( BigDecimal value, String unit, R resource );
}
