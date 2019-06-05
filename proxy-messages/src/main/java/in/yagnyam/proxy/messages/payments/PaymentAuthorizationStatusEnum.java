package in.yagnyam.proxy.messages.payments;

public enum PaymentAuthorizationStatusEnum {
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
