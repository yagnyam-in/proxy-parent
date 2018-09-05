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
public class Amount {

  @NonNull
  private String currency;

  private double value;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(currency) && Currency.isValidCurrency(currency) && value >= 0;
  }

}
