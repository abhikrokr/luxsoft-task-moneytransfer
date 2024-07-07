package com.dws.challenge.exception;

public class NotEnoughBalanceException extends RuntimeException {

  public NotEnoughBalanceException() {
    super("Not Enough Balance");
  }
}
