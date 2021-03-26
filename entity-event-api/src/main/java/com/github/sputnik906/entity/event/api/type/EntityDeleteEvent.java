package com.github.sputnik906.entity.event.api.type;


import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

@Getter
public class EntityDeleteEvent<T> extends AbstractEntityEvent<T> {

  private final Map<String, Object> states;

  public EntityDeleteEvent(Metadata metadata, String typeName, Serializable id, T entity,
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
    return new EntityDeleteEvent<>(getMetadata(), getEntityType(), getEntityId(),entity(),states).setEventId(
        getEventId());
  }

  @Override
  public String getEventType() {
    return "d";
  }
}
