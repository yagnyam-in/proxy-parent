package in.yagnyam.digana.cheque;

import java.util.Date;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;

import static in.yagnyam.digana.utils.ChequeDateUtils.daysAfterToday;
import static in.yagnyam.digana.utils.ChequeDateUtils.today;
import static in.yagnyam.digana.cheque.ChequeValidation.assertNotNull;
import static in.yagnyam.digana.cheque.ChequeValidation.assertPositive;

public class ChequeBuilder {

    private ChequeBook chequeBook;
    private long chequeNumber;
    private Payee payee;
    private AccountNumber payeeAccount;
    private ChequeTransaction transaction;
    private Amount amount;
    private int validityInDays = 60;

    public ChequeBook getChequeBook() {
        return chequeBook;
    }

    public ChequeBuilder chequeBook(ChequeBook chequeBook) {
        this.chequeBook = chequeBook;
        return this;
    }

    public long getChequeNumber() {
        return chequeNumber;
    }

    public ChequeBuilder chequeNumber(Long chequeNumber) {
        this.chequeNumber = chequeNumber;
        return this;
    }

    public Payee getPayee() {
        return payee;
    }

    public ChequeBuilder payee(Payee payee) {
        this.payee = payee;
        return this;
    }

    public AccountNumber getPayeeAccount() {
        return payeeAccount;
    }

    public ChequeBuilder payeeAccount(AccountNumber payeeAccount) {
        this.payeeAccount = payeeAccount;
        return this;
    }

    public ChequeTransaction getTransaction() {
        return transaction;
    }

    public ChequeBuilder transaction(ChequeTransaction transaction) {
        this.transaction = transaction;
        return this;
    }

    public Amount getAmount() {
        return amount;
    }

    public ChequeBuilder amount(Amount amount) {
        this.amount = amount;
        return this;
    }

    public int getValidityInDays() {
        return validityInDays;
    }

    public ChequeBuilder validityInDays(int validityInDays) {
        if (validityInDays < 0) {
            throw new IllegalArgumentException("validityInDays " + validityInDays + " can't be " + validityInDays);
        }
        this.validityInDays = validityInDays;
        return this;
    }

    public Cheque build() {
        Cheque cheque = new Cheque();
        assertNotNull(chequeBook, "Cheque Book");
        if (chequeBook.getExpiryDate().before(today())) {
            throw new IllegalArgumentException("Cheque Book " + chequeBook + " already expired");
        }
        cheque.setChequeBook(chequeBook);
        assertPositive(chequeNumber, "Cheque Number");
        if (chequeNumber < chequeBook.getInitialChequeNumber() || chequeNumber > chequeBook.getInitialChequeNumber() + chequeBook.getNumberOfCheques()) {
            throw new IllegalArgumentException("Cheque Number " + chequeNumber + " can't belong to Cheque Book " + chequeBook);
        }
        cheque.setChequeNumber(chequeNumber);
        assertNotNull(payee, "Payee");
        cheque.setPayee(payee);
        assertNotNull(amount, "Amount");
        cheque.setAmount(amount);
        // Payee AccountNumber can be null
        cheque.setPayeeAccount(payeeAccount);
        // Transaction can be null
        cheque.setTransaction(transaction);
        cheque.setIssueDate(today());
        cheque.setValidFrom(today());
        Date validTill = daysAfterToday(validityInDays);
        if (validTill.after(chequeBook.getExpiryDate())) {
            throw new IllegalArgumentException("Invalid value for validityInDays. Cheque Book expires on " + chequeBook.getExpiryDate());
        }
        cheque.setValidTill(validTill);
        return cheque;
    }

    public static ChequeBuilder builder() {
        return new ChequeBuilder();
    }

}