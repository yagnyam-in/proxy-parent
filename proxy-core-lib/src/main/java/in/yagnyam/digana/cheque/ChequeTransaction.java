package in.yagnyam.digana.cheque;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Transaction details to specify in Cheque Object
 * 
 * Not all fields are mandatory, depending on type of Transaction few fields
 * might be optional and few might be mandatory.
 * 
 * @author Yagnyam
 *
 */

@Data
@EqualsAndHashCode(of="transactionNumber")
@NoArgsConstructor
public class ChequeTransaction {

    private String transactionNumber;

    private String merchantTransactionNumber;

    private String shortDescription;

    private String transactionURL;

}
