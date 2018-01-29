package in.yagnyam.digana.bank;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.*;

import java.util.Date;

@Data
@EqualsAndHashCode(of="transferNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class Transfer {

    @NonNull
    private String transferNumber;

    @NonNull
    private AccountNumber fromAccount;

    @NonNull
    private AccountNumber toAccount;

    @NonNull
    private Amount amount;

    private Date requestDate;

    private Date executionDate;

}
