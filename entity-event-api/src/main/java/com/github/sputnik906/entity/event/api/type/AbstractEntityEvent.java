package com.github.sputnik906.entity.event.api.type;

import com.github.sputnik906.entity.event.api.Metadata;
import java.io.Serializable;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class AbstractEntityEvent<T> {
  private final Metadata metadata;
  private volatile Long eventId; //persist id event
  private final String entityType;
  private final Serializable entityId;

  @Getter(AccessLevel.NONE) //for not visible for jackson
  private transient final T entity;

  public String getEventType() {
    return this.getClass().getSimpleName();
  }

  public abstract Set<String> changedProperties();

  public abstract AbstractEntityEvent<T> withFilterProperties(Set<String> filterProperties);

  public AbstractEntityEvent<T> removeProperties(Set<String> removedProperties){
    if (removedProperties.size()==0) return this;
    Set<String> filteredProperties = changedProperties();
    filteredProperties.removeAll(removedProperties);
    return withFilterProperties(filteredProperties);
  }

  public synchronized AbstractEntityEvent<T> setEventId(Long eventId){
    if (this.eventId !=null) throw new IllegalArgumentException("You cann't change id");
    this.eventId = eventId;
    return this;
  }

  public T entity(){
    return entity;
  }
}
