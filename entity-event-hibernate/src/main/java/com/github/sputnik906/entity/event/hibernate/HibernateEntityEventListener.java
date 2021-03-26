package com.github.sputnik906.entity.event.hibernate;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_SET;

import com.github.sputnik906.entity.event.api.annotation.Related;
import com.github.sputnik906.entity.event.api.type.AbstractEntityEvent;
import com.github.sputnik906.entity.event.api.type.AddToCollectionEvent;
import com.github.sputnik906.entity.event.api.type.AddToMapEvent;
import com.github.sputnik906.entity.event.api.AuthorProvider;
import com.github.sputnik906.entity.event.api.type.EntityDeleteEvent;
import com.github.sputnik906.entity.event.api.type.EntityPersistEvent;
import com.github.sputnik906.entity.event.api.EntityReference;
import com.github.sputnik906.entity.event.api.type.EntityUpdateEvent;
import com.github.sputnik906.entity.event.api.IEntityEventProviderMarker;
import com.github.sputnik906.entity.event.api.Metadata;
import com.github.sputnik906.entity.event.api.TransactionEvents;
import com.github.sputnik906.entity.event.api.type.RemoveFromCollectionEvent;
import com.github.sputnik906.entity.event.api.type.RemoveFromMapEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Synchronization;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.MapType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy(false)
@Component
@ConditionalOnClass(SessionFactoryImpl.class)
public class HibernateEntityEventListener implements IEntityEventProviderMarker {
  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  private AuthorProvider authorProvider;

  private TransactionMetadataProvider transactionMetadataProvider;

  @PostConstruct
  public void init() {

    SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

    ChangeCollectionHandler changeCollectionHandler = new ChangeCollectionHandler();

    this.transactionMetadataProvider = new TransactionMetadataProvider(new AtomicLong(0));


    registry
      .getEventListenerGroup(EventType.POST_INSERT)
      .appendListener(
        new PostInsertEventListener() {
          @Override
          public boolean requiresPostCommitHanding(EntityPersister persister) {
            return false;
          }
          @Override
          public void onPostInsert(PostInsertEvent event) {

            Map<String, Object> states = new HashMap<>();
            String[] propertyNames =  event.getPersister().getPropertyNames();

            for(int i=0; i<propertyNames.length; i++){
              states.put(propertyNames[i],replaceOnEntityReferenceIfNeed(event.getSession(),event.getState()[i]));
            }

            publish(new EntityPersistEvent<>(
              new Metadata(
                transactionMetadataProvider.getTransactionId(event.getSession()),
                System.currentTimeMillis(),
                authorProvider.provide()
              ),
              getEntityTypeName(event.getEntity().getClass()),
              event.getId(),
              event.getEntity(),
              states
            ));
          }
        });


    registry
      .getEventListenerGroup(EventType.POST_UPDATE)
      .appendListener(
        new PostUpdateEventListener(){

          @Override
          public boolean requiresPostCommitHanding(EntityPersister persister) {
            return false;
          }

          @Override
          public boolean requiresPostCommitHandling(EntityPersister persister) {
            return false;
          }

          @Override
          public void onPostUpdate(PostUpdateEvent event) {

            Map<String, Object> oldStates = new HashMap<>();
            Map<String, Object> newStates = new HashMap<>();

            String[] propertyNames =  event.getPersister().getPropertyNames();

            String typeName = getEntityTypeName(event.getEntity().getClass());

            for(int i=0; i<event.getDirtyProperties().length; i++){
              int propIndex = event.getDirtyProperties()[i];

              Type propType = event.getPersister().getEntityMetamodel().getPropertyTypes()[propIndex];
              if (propType instanceof ComponentType){
                ComponentType componentPropType = (ComponentType)propType;
                for(String propName:componentPropType.getPropertyNames()){
                  int propNameIndex = componentPropType.getPropertyIndex(propName);
                  Object oldValue = componentPropType.getPropertyValue(event.getOldState()[propIndex],propNameIndex);
                  Object currentValue = componentPropType.getPropertyValue(event.getState()[propIndex],propNameIndex);
                  if (!oldValue.equals(currentValue)){
                    oldStates.put(propertyNames[propIndex]+"."+propName,replaceOnEntityReferenceIfNeed(event.getSession(),oldValue));
                    newStates.put(propertyNames[propIndex]+"."+propName,replaceOnEntityReferenceIfNeed(event.getSession(),currentValue));
                  }
                }
              }else{
                oldStates.put(propertyNames[propIndex],replaceOnEntityReferenceIfNeed(event.getSession(),event.getOldState()[propIndex]));
                newStates.put(propertyNames[propIndex],replaceOnEntityReferenceIfNeed(event.getSession(),event.getState()[propIndex]));

              }

            }

            int versionIndex = event.getPersister().getVersionProperty();
            if (versionIndex>-1){
              oldStates.put(propertyNames[versionIndex],event.getOldState()[versionIndex]);
              newStates.put(propertyNames[versionIndex],event.getState()[versionIndex]);
            }

            publish(new EntityUpdateEvent<>(
              new Metadata(
                transactionMetadataProvider.getTransactionId(event.getSession()),
                System.currentTimeMillis(),
                authorProvider.provide()
              ),
              typeName,
              event.getId(),
              event.getEntity(),
              oldStates,
              newStates
            ));

            //Handle related fields
            Stream.of(event.getPersister().getClassMetadata().getMappedClass().getDeclaredFields())
              .filter(f->f.isAnnotationPresent(Related.class))
              .filter(f->f.getDeclaringClass().isAnnotationPresent(Entity.class))
              .forEach(f->{

                Object relatedEntity = event.getPersister()
                  .getClassMetadata()
                  .getPropertyValue(event.getEntity(),f.getName());

                if (relatedEntity==null) return;

                String relatedPropName = f.getAnnotation(Related.class).value();
                if (relatedPropName.trim().isEmpty()) return;

                Serializable relatedId = event.getPersister()
                  .getIdentifier(relatedEntity,event.getSession());



                publish(new EntityUpdateEvent<>(
                  new Metadata(
                    transactionMetadataProvider.getTransactionId(event.getSession()),
                    System.currentTimeMillis(),
                    authorProvider.provide()
                  ),
                  getEntityTypeName(f.getType()),
                  relatedId,
                  relatedEntity,
                  addPrefixToKey(relatedPropName+".",oldStates),
                  addPrefixToKey(relatedPropName+".",newStates)
                 ));
              });



          }
        }
      );


    registry
      .getEventListenerGroup(EventType.POST_DELETE)
      .appendListener(
        new PostDeleteEventListener() {

          @Override
          public boolean requiresPostCommitHanding(EntityPersister persister) {
            return false;
          }

          @Override
          public boolean requiresPostCommitHandling(EntityPersister persister) {
            return false;
          }

          @Override
          public void onPostDelete(PostDeleteEvent event) {
            String typeName = getEntityTypeName(event.getEntity().getClass());

            Map<String, Object> states = new HashMap<>();
            String[] propertyNames =  event.getPersister().getPropertyNames();

            for(int i=0; i<propertyNames.length; i++){
              try {
                states.put(propertyNames[i],
                  replaceOnEntityReferenceIfNeed(event.getSession(), event.getDeletedState()[i]));
              }catch (Exception e){
                //TODO разобраться почему hibernate при удалении поля ElementCollection не дает доступ к ней
                states.put(propertyNames[i],"<unknow state>");
              }
            }

            publish(new EntityDeleteEvent<>(
              new Metadata(
                transactionMetadataProvider.getTransactionId(event.getSession()),
                System.currentTimeMillis(),
                authorProvider.provide()
              ),
              typeName,
              event.getId(),
              event.getEntity(),
              states
            ));
          }
        });

    registry
      .getEventListenerGroup(EventType.POST_COLLECTION_RECREATE)
      .appendListener(
        (PostCollectionRecreateEventListener) event -> {

        });

    registry
      .getEventListenerGroup(EventType.PRE_COLLECTION_UPDATE)
      .appendListener(
        (PreCollectionUpdateEventListener) event -> {

          CollectionEntry collectionEntry =  event.getSession()
            .getPersistenceContextInternal()
            .getCollectionEntry( event.getCollection() );

          changeCollectionHandler.saveSnapshot(collectionEntry,collectionEntry.getSnapshot());

        });

    registry
        .getEventListenerGroup(EventType.POST_COLLECTION_UPDATE)
        .appendListener(
            (PostCollectionUpdateEventListener)
                event -> {
                  CollectionEntry collectionEntry =
                      event
                          .getSession()
                          .getPersistenceContextInternal()
                          .getCollectionEntry(event.getCollection());

                  Serializable currentSnapshot = collectionEntry.getSnapshot();

                  Serializable preUpdateSnapshot =
                      changeCollectionHandler.getSnapshotAndClear(collectionEntry);

                  String typeName = getEntityTypeName(event.getAffectedOwnerOrNull().getClass());

                  String propertyName =
                      event
                          .getCollection()
                          .getRole()
                          .replace(event.getAffectedOwnerEntityName() + ".", "");

                  if (preUpdateSnapshot==null)
                    return; //Значит раньше этой коллекции не было, это может быть в
                  // случае создания Entity, тогда должен быть создано событие PersistEvent
                  // или в случае обновления, тогда должно быть создано событие UpdateEvent c протсавленным этим полем


                  if (collectionEntry.getCurrentPersister().getCollectionType() instanceof MapType) {
                    Map<?,?> currentMap = (Map<?,?>)currentSnapshot;
                    Map<?,?> prevUpdateMap = (Map<?,?>)preUpdateSnapshot;

                    Set<?> removed = keysDifference(prevUpdateMap,currentMap);

                    if (removed.size()>0) publish(new RemoveFromMapEvent<>(
                      new Metadata(
                        transactionMetadataProvider.getTransactionId(event.getSession()),
                        System.currentTimeMillis(),
                        authorProvider.provide()
                      ),
                      typeName,
                      event.getAffectedOwnerIdOrNull(),
                      event.getAffectedOwnerOrNull(),
                      propertyName,
                      removed.stream()
                        .collect(Collectors.toMap(Object::toString, k->replaceOnEntityReferenceIfNeed(
                          event.getSession(),
                          prevUpdateMap.get(k)
                        )))
                    ));

                    Set<?> added = keysDifference(currentMap,prevUpdateMap);

                    if (added.size()>0) publish(new AddToMapEvent<>(
                      new Metadata(
                        transactionMetadataProvider.getTransactionId(event.getSession()),
                        System.currentTimeMillis(),
                        authorProvider.provide()
                      ),
                      typeName,
                      event.getAffectedOwnerIdOrNull(),
                      event.getAffectedOwnerOrNull(),
                      propertyName,
                      added.stream()
                        .collect(Collectors.toMap(Object::toString, k->replaceOnEntityReferenceIfNeed(
                          event.getSession(),
                          currentMap.get(k)
                        )))
                    ));

                    Set<?> commonKeys = commonKeys(prevUpdateMap,currentMap);

                    Map<String, Object> oldStates = new HashMap<>();
                    Map<String, Object> newStates = new HashMap<>();

                    for(Object key:commonKeys){
                      Object currentValue = currentMap.get(key);
                      Object prevUpdateValue = prevUpdateMap.get(key);
                      if (!currentValue.equals(prevUpdateValue))
                        oldStates.put(propertyName+"."+key.toString(),replaceOnEntityReferenceIfNeed(event.getSession(),prevUpdateValue));
                        newStates.put(propertyName+"."+key.toString(),replaceOnEntityReferenceIfNeed(event.getSession(),currentValue));
                    }

                    if (oldStates.size()>0) publish(new EntityUpdateEvent<>(
                      new Metadata(
                        transactionMetadataProvider.getTransactionId(event.getSession()),
                        System.currentTimeMillis(),
                        authorProvider.provide()
                      ),
                      typeName,
                      event.getAffectedOwnerIdOrNull(),
                      event.getAffectedOwnerOrNull(),
                      oldStates,
                      newStates
                    ));



                  }else{
                    Set<?> removed = difference(preUpdateSnapshot,currentSnapshot);
                    if (removed.size()>0) publish(new RemoveFromCollectionEvent<>(
                      new Metadata(
                        transactionMetadataProvider.getTransactionId(event.getSession()),
                        System.currentTimeMillis(),
                        authorProvider.provide()
                      ),
                      typeName,
                      event.getAffectedOwnerIdOrNull(),
                      event.getAffectedOwnerOrNull(),
                      propertyName,
                      (Set<?>)replaceOnEntityReferenceIfNeed(
                        event.getSession(),
                        removed
                      )
                    ));

                    Set<?> added = difference(currentSnapshot,preUpdateSnapshot);



                    if (added.size()>0)  publish(new AddToCollectionEvent<>(
                      new Metadata(
                        transactionMetadataProvider.getTransactionId(event.getSession()),
                        System.currentTimeMillis(),
                        authorProvider.provide()
                      ),
                      typeName,
                      event.getAffectedOwnerIdOrNull(),
                      event.getAffectedOwnerOrNull(),
                      propertyName,
                      (Set<?>)replaceOnEntityReferenceIfNeed(
                        event.getSession(),
                        added
                      )
                    ));
                  }


                });

  }

  private void publish(AbstractEntityEvent<?> event){
    applicationEventPublisher.publishEvent(event);
    transactionMetadataProvider.addEvent(event);
  }

  private static Object replaceOnEntityReferenceIfNeed(EventSource eventSource,Object object){
    if (object instanceof List) return ((List<?>)object).stream()
      .map(o->replaceOnEntityReferenceIfNeed(eventSource,o)).collect(Collectors.toList());
    if (object instanceof Set) return ((Set<?>)object).stream()
      .map(o->replaceOnEntityReferenceIfNeed(eventSource,o)).collect(Collectors.toSet());
    if (object instanceof Map) return ((Map<?,?>)object).entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey,
        e -> replaceOnEntityReferenceIfNeed(eventSource,e)));
    EntityEntry entityEntry = eventSource.getPersistenceContext().getEntry(object);
    if (entityEntry!=null) return new EntityReference(entityEntry.getId(),entityEntry.getEntityName());
    return object;
  }

  private static String getEntityTypeName(Class<?> entityClass){
    return entityClass.getCanonicalName();
  }

  private static class ChangeCollectionHandler{
    ThreadLocal<CollectionEntry>  collectionEntry = new ThreadLocal<>();
    ThreadLocal<Serializable>  collection = new ThreadLocal<>();

    public void saveSnapshot(CollectionEntry collectionEntry,Serializable collection){
      if (this.collectionEntry.get()!=null) throw new IllegalStateException();
      if (this.collection.get()!=null) throw new IllegalStateException();

      this.collectionEntry.set(collectionEntry);
      this.collection.set(collection);
    }

    public Serializable getSnapshotAndClear(CollectionEntry key){
      if (!collectionEntry.get().equals(key)) throw new IllegalStateException();
      Serializable result = collection.get();
      collectionEntry.remove();
      collection.remove();
      return result;
    }
  }

  @RequiredArgsConstructor
  private  class TransactionMetadataProvider {
    final AtomicLong idCounter;

    final ThreadLocal<TransactionMetadata>  savedTransactionMetadata = new ThreadLocal<>();

    public String getTransactionId(EventSource eventSource){
      TransactionMetadata transactionMetadata = savedTransactionMetadata.get();
      if (transactionMetadata==null||!transactionMetadata.transaction.equals(eventSource.getTransaction())){
        transactionMetadata = new TransactionMetadata(
          eventSource.getTransaction(),
          idCounter.incrementAndGet()
        );
        savedTransactionMetadata.set(transactionMetadata);
        eventSource.getTransaction().registerSynchronization(new Synchronization() {
          @Override
          public void beforeCompletion() {
            List<AbstractEntityEvent<?>> events = savedTransactionMetadata.get().getEvents();
            if (events.size()>0) applicationEventPublisher.publishEvent(new TransactionEvents(events));
          }

          @Override
          public void afterCompletion(int status) {

          }
        });
      }
      return String.valueOf(transactionMetadata.getId());
    }

    public void addEvent(AbstractEntityEvent<?> event){
      savedTransactionMetadata.get().getEvents().add(event);
    }

    @RequiredArgsConstructor
    @Getter
    private class TransactionMetadata{
      final Transaction transaction;
      final long id;
      final List<AbstractEntityEvent<?>> events = new ArrayList<>();
    }
  }

  private static Set<?> keysDifference(Map<?,?> left, Map<?,?> right) {
    if (left == null){
      return Collections.emptySet();
    }

    if (right == null){
      return left.keySet();
    }

    return differenceSet(left.keySet(), right.keySet());
  }

  private static Set<?> differenceSet(Set<?> first, Set<?> second) {
    if (first == null || first.size() == 0) {
      return new HashSet<>();
    }

    if (second == null || second.size() == 0) {
      return first;
    }

    Set<?> difference = new HashSet<>(first);
    difference.removeAll(second);
    return difference;
  }

  private static Set<?> intersection(Set<?> first, Set<?> second) {
    if (first == null || second == null) {
      return EMPTY_SET;
    }

    Set<Object> intersection = new HashSet<>();

    for (Object e : first) {
      if (second.contains(e)) {
        intersection.add(e);
      }
    }
    return intersection;
  }

  private static Set<?> commonKeys(Map<?,?> left, Map<?,?> right) {
    if (left == null || right == null) {
      return Collections.emptySet();
    }

    return intersection(left.keySet(),right.keySet());
  }

  private static List<?> differenceList(List<?> first, List<?> second) {
    if (first == null) {
      return EMPTY_LIST;
    }

    if (second == null) {
      return first;
    }

    List<Object> difference = new ArrayList<>(first);
    difference.removeAll(second);
    return difference;
  }

  private static Set<?> difference(Object first, Object second){
    if (first instanceof List) return new HashSet<>(differenceList((List<?>)first,(List<?>)second));
    if (first instanceof Map) return keysDifference((Map<?,?>) first, (Map<?,?>) second);
    throw new IllegalStateException();
  }

  public static Map<String, Object> addPrefixToKey(String prefix,Map<String, Object> map){
    if (map==null) return null;

    Map<String, Object> result = new HashMap<>();

    for(Entry<String, Object> entry : map.entrySet()){
      result.put(prefix+entry.getKey(), entry.getValue());
    }

    return result;
  }



}
