package com.github.sputnik906.entity.event.api;

import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class EntityReference {
  private final Serializable id;
  private final String entityType;
}
