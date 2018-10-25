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
import lombok.Setter;
import lombok.ToString;

/**
 * Entity to persist Payment
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "paymentId")
public class PaymentEntity {

  public enum Status {
    Registered,
    WaitingForFunds,
    Processed,
    Cancelled,
  }

  @Id
  @NonNull
  private String paymentId;

  @NonNull
  private String paymentEncoded;

  @NonNull
  private Amount amount;

  @NonNull
  @Setter
  private Status status;

}
