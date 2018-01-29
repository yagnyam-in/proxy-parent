package in.yagnyam.digana.cheque;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.*;

import java.util.Date;

/**
 * <p>
 * Cheque represents the promise by a bank and issuer to pay given maximum
 * amount from the issuers account to the Payee.
 * </p>
 * 
 * <p>
 * Cheque can be claimed only when both bank and issuers has signed it.
 * </p>
 * 
 * @author Yagnyam
 *
 */
@EqualsAndHashCode(of={"chequeBook", "chequeNumber"})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Getter
@ToString
public class Cheque {

	public static final String CURRENT_VERSION = ChequeSigner_V1.VERSION;

    @NonNull
	private ChequeBook chequeBook;

	private long chequeNumber;

    @NonNull
	private String versionNumber = CURRENT_VERSION;

    @NonNull
	private Date validFrom;

    @NonNull
	private Date validTill;

    @NonNull
	private String payerCertificateSerial;

	private String payerCertificateEncoded;

    @NonNull
	private Date issueDate;

    @NonNull
	private Amount amount;

    @NonNull
	private String payerSignatureOne;

    @NonNull
	private String payerSignatureTwo;

    /**
     * Payee is mandatory (No Anonymous Cheques are supported to prevent Fraud)
     */
    @NonNull
	private Payee payee;

    /**
     * If specified, this Cheque can be deposited by anyone with issuing bank.
     * While paying to Merchants, payee AccountNumber should be populated
     */
	private AccountNumber payeeAccount;

    /**
     * Transaction for which this Cheque is being issued.
     * Only applicable while paying to a merchant
     */
	private ChequeTransaction transaction;

}
