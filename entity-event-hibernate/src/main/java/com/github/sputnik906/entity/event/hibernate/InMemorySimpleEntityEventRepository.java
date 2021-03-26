package com.github.sputnik906.entity.event.hibernate;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.github.sputnik906.entity.event.api.type.AbstractEntityEvent;
import com.github.sputnik906.entity.event.api.repo.EntityEventQuery;
import com.github.sputnik906.entity.event.api.repo.EntityEventRepository;
import com.github.sputnik906.entity.event.api.TransactionEvents;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import org.springframework.transaction.event.TransactionalEventListener;

public class InMemorySimpleEntityEventRepository implements EntityEventRepository {

  private final List<AbstractEntityEvent<?>> array = Collections.synchronizedList(new ArrayList<>());

  @Override
  public synchronized void saveAllEventsOneTransaction(TransactionEvents transactionEvents) {
    List<AbstractEntityEvent<?>> events = transactionEvents.getEvents();
    int startIndex = array.size();
    for(int i=0; i<events.size(); i++) events.get(i).setEventId((long)(startIndex+i));
    array.addAll(transactionEvents.getEvents());
  }

  @Override
  public long size() {
    return array.size();
  }

  @Override
  public void deleteHead(int count) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long lastEventId() {
    return array.size()- 1L;
  }

  @Override
  public AbstractEntityEvent<?> lastEvent() {
    return array.size()>0
      ?array.get(array.size()-1)
      :null;
  }

  @Override
  public List<AbstractEntityEvent<?>> lastEvents(long count) {
    int size = array.size();
    return new ArrayList<>(array.subList((int)Math.min(size-count,0),size));
  }


  @Override
  public List<AbstractEntityEvent<?>> executeQuery(List<EntityEventQuery> query) {

    long theEarliestAfterEventId = query.stream()
       .mapToLong(e->e.getAfterEventId()!=null?e.getAfterEventId():-1L)
       .min()
       .orElseThrow(NoSuchElementException::new);

    if (theEarliestAfterEventId>=array.size())
      throw new IllegalArgumentException("AfterEventId "+theEarliestAfterEventId+" is greater or equal than max id in repository");

    long startIndex = Math.max(0,theEarliestAfterEventId);
    List<AbstractEntityEvent<?>> subList = array.subList((int)startIndex,array.size());

    Map<String,EntityEventQuery> entityTypeToQueryMap = query.stream()
      .collect(Collectors.toMap(EntityEventQuery::getEntityType, e->e));

    EntityEventQuery defaultQuery = entityTypeToQueryMap.get(null);

    return subList.stream()
      .filter(e->entityTypeToQueryMap.containsKey(null)||entityTypeToQueryMap.containsKey(e.getEntityType()))
      //т.к. искали с theEarliestAfterId то отсеиваем лишнии events для каждого entityType
      .filter(e->
          entityTypeToQueryMap.getOrDefault(e.getEntityType(),defaultQuery).getAfterEventId()==null
          || entityTypeToQueryMap.getOrDefault(e.getEntityType(),defaultQuery).getAfterEventId()<e.getEventId()
      )
     .map(e->entityTypeToQueryMap.getOrDefault(e.getEntityType(),defaultQuery).getPropertyNames()==null
        ?e
        :e.withFilterProperties(entityTypeToQueryMap.getOrDefault(e.getEntityType(),defaultQuery).getPropertyNames())
      )
      .filter(Objects::nonNull)
      .filter(e->isMatch(e,entityTypeToQueryMap.getOrDefault(e.getEntityType(),defaultQuery)))
      .collect(Collectors.toList());
  }

  @Override
  public List<AbstractEntityEvent<?>> executeQuery(EntityEventQuery query) {
    return executeQuery(Collections.singletonList(query));
  }

  @TransactionalEventListener
  public  void handler(TransactionEvents transactionEvents){
    List<AbstractEntityEvent<?>> filteredCdcEntityEvents = transactionEvents.getEvents().stream()
      .filter(e->
        clearProxyOfEntityClass(e.entity().getClass())
          .map(c->c.isAnnotationPresent(CdcEntity.class))
          .orElse(false)
      ).map(e->e.removeProperties(new HashSet<>(
        Arrays.asList(clearProxyOfEntityClass(e.entity().getClass())
          .map(c->c.getAnnotation(CdcEntity.class))
          .orElseThrow(IllegalStateException::new)
          .ignoredProperties())))
      )
      .collect(Collectors.toList());
    if (filteredCdcEntityEvents.isEmpty()) return;
    saveAllEventsOneTransaction(new TransactionEvents(filteredCdcEntityEvents));
  }

  private Optional<Class<?>> clearProxyOfEntityClass(Class<?> entityClass){
    if (entityClass.isAnnotationPresent(Entity.class)) return Optional.of(entityClass);
    if (entityClass.equals(Object.class)) return Optional.empty();
    return clearProxyOfEntityClass(entityClass.getSuperclass());
  }


  private boolean isMatch(AbstractEntityEvent<?> e, EntityEventQuery query){
    return (query.getEventTypes()==null||query.getEventTypes().contains(e.getEventType()))
      &&(query.getEntityIds()==null||query.getEntityIds().contains(e.getEntityId()))
      &&(query.getLessOrEqualEntityId()==null||isIdLessOrEqual(e.getEntityId(),query.getLessOrEqualEntityId()))
      &&(query.getGreatOrEqualEntityId()==null||isIdGreatOrEqual(e.getEntityId(),query.getGreatOrEqualEntityId()))
      &&(query.getAuthors()==null||query.getAuthors().contains(e.getMetadata().getAuthor()));
  }

  private boolean isIdLessOrEqual(Serializable id, Serializable compareId){
    return id.toString().compareTo(compareId.toString())<=0;
  }

  private boolean isIdGreatOrEqual(Serializable id, Serializable compareId){
    return id.toString().compareTo(compareId.toString())>=0;
  }

}
