package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class EntityPersistEvent<T> extends AbstractEntityEvent<T> {

  private final Map<String, Object> states;

  public EntityPersistEvent(Metadata metadata, String typeName, Serializable id, T entity,
    Map<String, Object> states) {
    super(metadata,typeName, id, entity);
    this.states=states;
  }

  @Override
  public Set<String> changedProperties() {
    return states.keySet();
  }

  @Override
  public AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties) {
    Map<String,Object> intersection = changedProperties().stream()
      .filter(filterProperties::contains)
      .collect(Collectors.toMap(k->k,states::get));

    return intersection.size()>0
      ?new EntityPersistEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),intersection)
        .setEventId(getEventId())
      :null;
  }

  @Override
  public String getEventType() {
    return "c";
  }
}
