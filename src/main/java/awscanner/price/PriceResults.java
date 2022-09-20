package awscanner.price;

import java.math.BigDecimal;


public record PriceResults( BigDecimal price_per_hour,
                            boolean price_is_guess ) {
}
