package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class EntityUpdateEvent<T> extends AbstractEntityEvent<T> {

  private final Map<String,Object> oldStates;

  private final Map<String,Object> newStates;

  public EntityUpdateEvent(Metadata metadata, String typeName, Serializable id, T entity,
    Map<String,Object> oldStates,Map<String,Object> newStates) {
    super(metadata,typeName, id, entity);
    this.oldStates=oldStates;
    this.newStates=newStates;
  }

  @Override
  public Set<String> changedProperties() {
    return newStates.keySet();
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    Map<String,Object> newIntersection = newStates.keySet().stream()
      .filter(filterProperties::contains)
      .collect(Collectors.toMap(k->k,newStates::get));

    Map<String,Object> oldIntersection = oldStates.keySet().stream()
      .filter(filterProperties::contains)
      .collect(Collectors.toMap(k->k,oldStates::get));

    return newIntersection.size()>0
      ?new EntityUpdateEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),oldIntersection,newStates)
         .setEventId(getEventId())
      :null;
  }

  @Override
  public String getEventType() {
    return "u";
  }
}
