package in.yagnyam.proxy.messages.banking;


import com.fasterxml.jackson.annotation.JsonIgnore;
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
  private Currency currency;

  private double value;

  @JsonIgnore
  public boolean isValid() {
    return currency != null && value >= 0;
  }

}
