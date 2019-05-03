package in.yagnyam.proxy.messages.banking;

public enum WithdrawalStatusEnum {
  Registered,
  Rejected,
  Cancelled,
  InTransit,
  Completed,
  FailedInTransit,
  FailedCompleted,
}
