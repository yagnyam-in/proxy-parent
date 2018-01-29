package in.yagnyam.digana.cheque;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.*;

import java.util.Date;

/**
 * <p>
 * ChequeBook represents collection of Cheques that could you issued individually
 * </p>
 * <p/>
 * <p>
 * Cheque book can contain any number of Cheques
 * </p>
 *
 * @author Yagnyam
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode(of={"bank", "chequeBookNumber"})
@Setter(AccessLevel.PACKAGE)
@Getter
@ToString
public class ChequeBook {

    public static final String CURRENT_VERSION = ChequeSigner_V1.VERSION;

    @NonNull
    private String versionNumber = CURRENT_VERSION;

    private long chequeBookNumber;

    private long initialChequeNumber;

    private long numberOfCheques;

    @NonNull
    private Bank bank;

    @NonNull
    private String bankCertificateSerial;

    private String bankCertificateEncoded;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @NonNull
    private String bankSignatureOne;

    @NonNull
    private String bankSignatureTwo;

    /**
     * Maximum amount for which *each* Cheque is issued
     */
    @NonNull
    private Amount maximumAmount;

    @NonNull
    private Payer payer;

    private AccountNumber payerAccount;
}
