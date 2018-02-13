package in.yagnyam.digana.server.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

import in.yagnyam.digana.cheque.Cheque;
import lombok.*;

/**
 * Entity class to persist Entity in DB
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ChequeEntity {

    public enum ChequeStatus {
        Empty,
        Issued,
        Cancelled,
        Encashed,
    }

    @Id
    private Long id;

    @Index
    private String payerBankNumber;

    @Index
    private Long chequeNumber;

    @Index
    private Long chequeBookNumber;

    @Index
    private String payerAccountNumber;

    @Index
    private String payerNumber;

    @Index
    private String payeeNumber;

    @Index
    private Date registrationTimestamp;

    @Index
    private String transactionNumber;

    Cheque cheque;

    ChequeStatus chequeStatus;

}
