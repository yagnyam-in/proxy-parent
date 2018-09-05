package in.yagnyam.proxy.server.banking.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import in.yagnyam.proxy.messages.banking.Amount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Entity to represent Original Account
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "accountId")
public class OriginalAccountEntity {

  @Id
  @NonNull
  private String accountId;

  @NonNull
  private String bank;

  @NonNull
  private String accountHolder;

  @NonNull
  private String currency;

  @NonNull
  private Amount balance;

}
