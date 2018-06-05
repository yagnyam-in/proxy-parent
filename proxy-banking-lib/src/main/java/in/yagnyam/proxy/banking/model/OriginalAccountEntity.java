package in.yagnyam.proxy.banking.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import in.yagnyam.proxy.messages.payments.Amount;
import lombok.*;

import java.util.Date;

/**
 * Entity to represent Orignal Account
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
    private String accountHolder;

    @NonNull
    private Amount balance;

}
