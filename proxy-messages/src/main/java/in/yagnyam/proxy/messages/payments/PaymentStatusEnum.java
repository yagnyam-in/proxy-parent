package in.yagnyam.proxy.messages.payments;

public enum PaymentStatusEnum {
  Registered,
  Rejected,
  InsufficientFunds,
  CancelledByPayer,
  CancleedByPayee,
  InProcess,
  Processed,
  Expired,
  Error
}
