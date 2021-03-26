package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class AddToMapEvent<T> extends AbstractEntityEvent<T> {
  private final String propertyName;

  private final Map<String,Object> addedEntries;

  public AddToMapEvent(Metadata metadata, String typeName, Serializable id, T entity,
    String propertyName,Map<String,Object> addedEntries) {
    super(metadata,typeName, id, entity);
    this.propertyName=propertyName;
    this.addedEntries=addedEntries;
  }

  @Override
  public Set<String> changedProperties() {
    return addedEntries.keySet().stream()
      .map(e->propertyName+"."+e)
      .collect(Collectors.toSet());
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    Map<String,Object> intersection = changedProperties().stream()
      .filter(filterProperties::contains)
      .collect(Collectors.toMap(k->k,addedEntries::get));

    return intersection.size()>0
      ?new AddToMapEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),getPropertyName(),intersection)
        .setEventId(getEventId())
      :null;
  }

  @Override
  public String getEventType() {
    return "am";
  }
}
