package com.github.sputnik906.entity.event.api;

import lombok.Value;

@Value
public class Metadata {
  private final String transactionId;
  private final long timestamp;
  private final String author;
}
