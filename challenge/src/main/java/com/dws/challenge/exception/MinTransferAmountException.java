package com.dws.challenge.exception;

public class MinTransferAmountException extends RuntimeException {

  public MinTransferAmountException() {
    super("Minimum transfer amount should be more than 0.1");
  }
}
