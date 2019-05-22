package in.yagnyam.proxy.messages.payments;

public enum PaymentStatusEnum {

    Registered,
    InsufficientFunds,
    CancelledByPayer,
    CancleedByPayee,
    Processed,
    Expired,
    Error
}
