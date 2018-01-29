package in.yagnyam.digana.cheque;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.NonNull;

import static in.yagnyam.digana.utils.ChequeDateUtils.monthsAfterToday;
import static in.yagnyam.digana.utils.ChequeDateUtils.today;
import static in.yagnyam.digana.cheque.ChequeValidation.*;

/**
 * Builder Class to Create ChequeBook
 */
public class ChequeBookBuilder {

    private Bank bank;
    private long chequeBookNumber;
    private long initialChequeNumber;
    private long numberOfCheques;
    private Payer payer;
    private AccountNumber payerAccount;
    private Amount maximumAmount;
    private int validityInMonths = 6;

    public static ChequeBookBuilder builder() {
        return new ChequeBookBuilder();
    }

    public Bank getBank() {
        return bank;
    }

    public ChequeBookBuilder bank(@NonNull Bank bank) {
        this.bank = bank;
        return this;
    }

    public long getChequeBookNumber() {
        return chequeBookNumber;
    }

    public ChequeBookBuilder chequeBookNumber(long chequeBookNumber) {
        assertNullOrPositive(chequeBookNumber, "chequeBookNumber");
        this.chequeBookNumber = chequeBookNumber;
        return this;
    }

    public long getInitialChequeNumber() {
        return initialChequeNumber;
    }

    public ChequeBookBuilder initialChequeNumber(long initialChequeNumber) {
        assertNullOrPositive(initialChequeNumber, "initialChequeNumber");
        this.initialChequeNumber = initialChequeNumber;
        return this;
    }

    public long getNumberOfCheques() {
        return numberOfCheques;
    }

    public ChequeBookBuilder numberOfCheques(long numberOfCheques) {
        assertNullOrPositive(numberOfCheques, "numberOfCheques");
        this.numberOfCheques = numberOfCheques;
        return this;
    }

    public Payer getPayer() {
        return payer;
    }

    public ChequeBookBuilder payer(@NonNull Payer payer) {
        this.payer = payer;
        return this;
    }

    public AccountNumber getPayerAccount() {
        return payerAccount;
    }

    public ChequeBookBuilder payerAccount(AccountNumber payerAccount) {
        this.payerAccount = payerAccount;
        return this;
    }

    public Amount getMaximumAmount() {
        return maximumAmount;
    }

    public ChequeBookBuilder maximumAmount(@NonNull Amount maximumAmount) {
        if (maximumAmount != null && maximumAmount.getValue() <= 0) {
            throw new IllegalArgumentException("maximumAmount can't be " + maximumAmount);
        }
        this.maximumAmount = maximumAmount;
        return this;
    }

    public int getValidityInMonths() {
        return validityInMonths;
    }

    public ChequeBookBuilder validityInMonths(int validityInMonths) {
        assertPositive(validityInMonths, "validityInMonths");
        this.validityInMonths = validityInMonths;
        return this;
    }

    public ChequeBook build() {
        ChequeBook chequeBook = new ChequeBook();
        assertNotNull(bank, "bank");
        chequeBook.setBank(bank);
        assertPositive(chequeBookNumber, "chequeBookNumber");
        chequeBook.setChequeBookNumber(chequeBookNumber);
        assertPositive(initialChequeNumber, "initialChequeNumber");
        chequeBook.setInitialChequeNumber(initialChequeNumber);
        assertPositive(numberOfCheques, "numberOfCheques");
        chequeBook.setNumberOfCheques(numberOfCheques);
        assertNotNull(payer, "payer");
        chequeBook.setPayer(payer);
        // Payer AccountNumber could be NULL
        chequeBook.setPayerAccount(payerAccount);
        assertNotNull(maximumAmount, "maximumAmount");
        chequeBook.setMaximumAmount(maximumAmount);
        chequeBook.setCreationDate(today());
        chequeBook.setExpiryDate(monthsAfterToday(validityInMonths));
        chequeBook.setVersionNumber(ChequeBook.CURRENT_VERSION);
        return chequeBook;
    }

}