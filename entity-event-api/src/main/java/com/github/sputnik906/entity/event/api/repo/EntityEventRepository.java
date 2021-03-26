package com.github.sputnik906.entity.event.api.repo;

import com.github.sputnik906.entity.event.api.TransactionEvents;
import com.github.sputnik906.entity.event.api.type.AbstractEntityEvent;
import java.util.List;

public interface EntityEventRepository {

  void saveAllEventsOneTransaction(TransactionEvents events);

  long size();

  void deleteHead(int count);

  Long lastEventId();

  AbstractEntityEvent<?> lastEvent();

  List<AbstractEntityEvent<?>> lastEvents(long count);

  List<AbstractEntityEvent<?>> executeQuery(EntityEventQuery query);

  List<AbstractEntityEvent<?>> executeQuery(List<EntityEventQuery> query);

  default List<AbstractEntityEvent<?>> after(Long eventId){
    return executeQuery(new EntityEventQuery(eventId));
  }

}
