package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class AddToCollectionEvent<T> extends AbstractEntityEvent<T>{

  private final String propertyName;

  private final Set<?> addedValues;

  public AddToCollectionEvent(Metadata metadata, String typeName, Serializable id, T entity,
    String propertyName,Set<?> addedValues) {
    super(metadata,typeName, id, entity);
    this.propertyName=propertyName;
    this.addedValues=addedValues;
  }

  @Override
  public Set<String> changedProperties() {
    return new HashSet<>(Collections.singletonList(propertyName));
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    return filterProperties.contains(propertyName)
      ?new AddToCollectionEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),getPropertyName(),getAddedValues())
        .setEventId(getEventId())
      :null;
  }
  @Override
  public String getEventType() {
    return "ac";
  }
}
