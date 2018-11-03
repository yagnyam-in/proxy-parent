package in.yagnyam.proxy.messages.banking;


import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>Amount class represents certain amount in a given currency</p>
 *
 * @author Yagnyam
 * @see Currency
 */
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(staticName = "of")
public class Amount implements Comparable<Amount> {

  @NonNull
  private String currency;

  private double value;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(currency) && Currency.isValidCurrency(currency) && value >= 0;
  }

  public Amount add(@NonNull Amount amount) {
    if (!StringUtils.equals(currency, amount.currency)) {
      throw new IllegalArgumentException("Currencies " + currency + ", " + amount.currency + " are not same");
    }
    return Amount.of(currency, value + amount.value);
  }


  public Amount subtract(@NonNull Amount amount) {
    if (!StringUtils.equals(currency, amount.currency)) {
      throw new IllegalArgumentException("Currencies " + currency + ", " + amount.currency + " are not same");
    }
    return Amount.of(currency, value - amount.value);
  }


  @Override
  public int compareTo(@SuppressWarnings("NullableProblems") Amount amount) {
    if (!StringUtils.equals(currency, amount.currency)) {
      throw new IllegalArgumentException("Currencies " + currency + ", " + amount.currency + " are not same");
    }
    return Double.compare(value, amount.value);
  }
}
