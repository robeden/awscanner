package awscanner.price;

import software.amazon.awssdk.services.pricing.model.Filter;

import java.util.Collection;


public interface ResourcePriceAttributes {
    String serviceCode();
    Collection<Filter> buildFilters();
}
