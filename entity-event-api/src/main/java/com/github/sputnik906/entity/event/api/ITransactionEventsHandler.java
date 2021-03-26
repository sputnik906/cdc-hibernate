package com.github.sputnik906.entity.event.api;

public interface ITransactionEventsHandler {
  void handle(TransactionEvents transactionEvents);
}
