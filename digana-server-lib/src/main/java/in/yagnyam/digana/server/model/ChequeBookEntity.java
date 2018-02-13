package in.yagnyam.digana.server.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import in.yagnyam.digana.cheque.ChequeBook;
import lombok.*;

/**
 * Entity class to persist Cheque Book in DB
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ChequeBookEntity {

    public enum Status {
        Active,
        Cancelled,
        Expired
    }

    @Id
    private Long id;

    @Index
    private String bankNumber;

    @Index
    private String payerNumber;

    @Index
    private Long chequeBookNumber;

    @Index
    private String accountNumber;

    private ChequeBook chequeBook;

    private Status status;

    public void setChequeBook(ChequeBook chequeBook) {
        this.chequeBook = chequeBook;
        if (chequeBook != null) {
            this.chequeBookNumber = chequeBook.getChequeBookNumber();
            if (chequeBook.getBank() != null) {
                this.bankNumber = chequeBook.getBank().getBankNumber();
            }
            if (chequeBook.getPayer() != null) {
                this.payerNumber = chequeBook.getPayer().getPayerNumber();
            }
            if (chequeBook.getPayerAccount() != null) {
                this.accountNumber = chequeBook.getPayerAccount().getAccountNumber();
            }
        }
    }

}
