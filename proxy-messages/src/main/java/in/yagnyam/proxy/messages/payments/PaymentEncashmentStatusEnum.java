package in.yagnyam.proxy.messages.payments;

public enum PaymentEncashmentStatusEnum {
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
