package com.github.sputnik906.entity.event.api;

import com.github.sputnik906.entity.event.api.type.AbstractEntityEvent;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class TransactionEvents {
  private final String transactionId;
  private final List<AbstractEntityEvent<?>> events;

  public TransactionEvents(
    List<AbstractEntityEvent<?>> events) {
    if (events.isEmpty()) throw new IllegalArgumentException();
    this.transactionId = events.get(0).getMetadata().getTransactionId();
    if (!events.stream()
      .skip(1)
      .allMatch(e->e.getMetadata().getTransactionId().equals(transactionId)))
        throw new IllegalArgumentException();

    this.events = Collections.unmodifiableList(events);
  }
}
