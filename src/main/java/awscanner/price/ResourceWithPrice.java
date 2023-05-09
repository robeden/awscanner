package awscanner.price;

import java.math.BigDecimal;
import java.util.Optional;


public interface ResourceWithPrice {
    Optional<BigDecimal> price_per_hour();
}
