package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class RemoveFromCollectionEvent<T> extends AbstractEntityEvent<T>{

  private final String propertyName;

  private final Set<?> removedValues;

  public RemoveFromCollectionEvent(Metadata metadata, String typeName, Serializable id, T entity,
    String propertyName,Set<?> removedValues) {
    super(metadata,typeName, id, entity);
    this.propertyName=propertyName;
    this.removedValues=removedValues;
  }

  @Override
  public Set<String> changedProperties() {
    return new HashSet<>(Collections.singletonList(propertyName));
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    return filterProperties.contains(propertyName)
      ?new RemoveFromCollectionEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),getPropertyName(),getRemovedValues())
        .setEventId(getEventId())
      :null;
  }

  @Override
  public String getEventType() {
    return "rc";
  }
}
