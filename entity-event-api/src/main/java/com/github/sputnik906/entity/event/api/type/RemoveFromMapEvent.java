package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class RemoveFromMapEvent<T> extends AbstractEntityEvent<T> {
  private final String propertyName;

  private final Map<String,Object> removedEntries;

  public RemoveFromMapEvent(Metadata metadata, String typeName, Serializable id, T entity,
    String propertyName,Map<String,Object> removedEntries) {
    super(metadata,typeName, id, entity);
    this.propertyName=propertyName;
    this.removedEntries=removedEntries;
  }

  @Override
  public Set<String> changedProperties() {
    return removedEntries.keySet().stream()
      .map(e->propertyName+"."+e)
      .collect(Collectors.toSet());
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    Map<String,Object> intersection = changedProperties().stream()
      .filter(filterProperties::contains)
      .collect(Collectors.toMap(k->k,removedEntries::get));

    return intersection.size()>0
      ?new RemoveFromMapEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),getPropertyName(),intersection)
         .setEventId(getEventId())
      :null;
  }

  @Override
  public String getEventType() {
    return "rm";
  }
}
