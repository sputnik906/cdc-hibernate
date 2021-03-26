package com.github.sputnik906.entity.event.api.repo;

import java.io.Serializable;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Accessors(chain = true)
public class EntityEventQuery{

  private final Long afterEventId;

  private String entityType;

  private Set<String> eventTypes;

  private Set<Serializable> entityIds;

  private Set<String> propertyNames;

  private Set<String> authors;

  private Serializable greatOrEqualEntityId;

  private Serializable lessOrEqualEntityId;

  public EntityEventQuery with(Long afterEventId){
    return new EntityEventQuery(
      afterEventId,
      entityType,
      eventTypes,
      entityIds,
      propertyNames,
      authors,
      greatOrEqualEntityId,
      lessOrEqualEntityId
    );
  }

}
