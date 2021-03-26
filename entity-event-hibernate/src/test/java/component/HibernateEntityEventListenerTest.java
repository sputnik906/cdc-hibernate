package component;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import com.github.sputnik906.entity.event.api.repo.EntityEventRepository;
import com.github.sputnik906.entity.event.api.type.AbstractEntityEvent;
import com.github.sputnik906.entity.event.api.type.AddToCollectionEvent;
import com.github.sputnik906.entity.event.api.type.AddToMapEvent;
import com.github.sputnik906.entity.event.api.type.EntityDeleteEvent;
import com.github.sputnik906.entity.event.api.type.EntityPersistEvent;
import com.github.sputnik906.entity.event.api.type.EntityUpdateEvent;
import com.github.sputnik906.entity.event.api.type.RemoveFromCollectionEvent;
import com.github.sputnik906.entity.event.hibernate.HibernateEntityEventConfiguration;
import com.github.sputnik906.entity.event.hibernate.HibernateEntityEventListener;
import component.domain.project.valueobject.Address;
import component.domain.project.valueobject.Position;
import component.domain.project.entity.Employee;
import component.domain.project.entity.Project;
import component.domain.project.entity.Skill;
import component.domain.project.entity.Task;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


@DataJpaTest
@ContextConfiguration(classes = {HibernateEntityEventConfiguration.class,HibernateEntityEventListener.class})
@Transactional(propagation = NOT_SUPPORTED) // we're going to handle transactions manually
@AutoConfigurationPackage
public class HibernateEntityEventListenerTest {

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private EntityManager em;

  @Autowired
  private EntityEventRepository entityEventRepository;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  public void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
  }


  @Test
  public void skillCreateAndDeleteTest() {

    Skill skill = new Skill("Skill 1");

    transactionTemplate.executeWithoutResult(s->em.persist(skill));

    EntityPersistEvent<?> entityPersistEvent =
      (EntityPersistEvent<?>)entityEventRepository.lastEvent();

    Assertions.assertEquals(skill,entityPersistEvent.entity());

    skill.setLabel("Skill Changed");

    Skill newSkill = transactionTemplate.execute(s->em.merge(skill));

    EntityUpdateEvent<?> entityUpdateEvent =
      (EntityUpdateEvent<?>)entityEventRepository.lastEvent();

    Assertions.assertEquals(newSkill,entityUpdateEvent.entity());

    transactionTemplate.executeWithoutResult(s->em.remove(em.merge(newSkill)));

    EntityDeleteEvent<?> entityDeleteEvent =
      (EntityDeleteEvent<?>)entityEventRepository.lastEvent();

    Assertions.assertNotNull(entityDeleteEvent.entity());

  }

  @Test
  public void cascadePersistTest() {

    Task task1 =  new Task("Task 1",new HashSet<>());
    Task task2 =  new Task("Task 2",new HashSet<>());

    Project project = new Project("Project 1").addTasks(new HashSet<>(Arrays.asList(task1,task2)));

    Long lastId = entityEventRepository.lastEventId();

    transactionTemplate.executeWithoutResult(s->em.persist(project));


    List<AbstractEntityEvent<?>> lastEvents = entityEventRepository.after(lastId);

    EntityPersistEvent<Project> event1 = cast(lastEvents.get(0));
    EntityPersistEvent<Task> event2 = cast(lastEvents.get(1));
    EntityPersistEvent<Task> event3 =  cast(lastEvents.get(2));

    Task task3 =  new Task("Task 3",new HashSet<>());

    project.addTasks(new HashSet<>(Collections.singletonList(task3)));

    lastId = entityEventRepository.lastEventId();

    Project newProject = transactionTemplate.execute(s->em.merge(project));


    lastEvents = entityEventRepository.after(lastId);

    EntityPersistEvent<Task> event7 = cast(lastEvents.get(0));
    EntityUpdateEvent<Task> event8 = cast(lastEvents.get(1)); //TODO почему возникаес событие обновления requiredSkills?
    AddToCollectionEvent<Project> event9 = cast(lastEvents.get(2));

    Assertions.assertEquals("tasks",event9.getPropertyName());

    newProject.getTasks().removeIf(t->t.getLabel().equals("Task 1"));

    lastId = entityEventRepository.lastEventId();

    Project newProject2 = transactionTemplate.execute(s->em.merge(newProject));

    lastEvents = entityEventRepository.after(lastId);

    RemoveFromCollectionEvent<Project> event11 = cast(lastEvents.get(0));

    Assertions.assertEquals("tasks",event11.getPropertyName());

    EntityDeleteEvent<Task> event12 = cast(lastEvents.get(1));
    Assertions.assertEquals("Task 1",event12.entity().getLabel());

    lastId = entityEventRepository.lastEventId();

    transactionTemplate.executeWithoutResult(s->em.remove(em.merge(newProject2)));


    lastEvents = entityEventRepository.after(lastId);

    EntityDeleteEvent<Task> event13 = cast(lastEvents.get(0));
    EntityDeleteEvent<Task> event14 = cast(lastEvents.get(1));
    EntityDeleteEvent<Project> event15 = cast(lastEvents.get(2));

  }



  @Test
  public void elementCollectionTest() {

    Employee employee = new Employee("Employee 1",new HashSet<>(),new Address("Street 1",443029),new Position(10,20));
    employee.getPhones().add("111-11-11");

    Long lastId = entityEventRepository.lastEventId();

    transactionTemplate.executeWithoutResult(s->em.persist(employee));


    List<AbstractEntityEvent<?>> lastEvents = entityEventRepository.after(lastId);

    EntityPersistEvent<Employee> event1 = cast(lastEvents.get(0));

    employee.getPhones().add("222-22-22");

    lastId = entityEventRepository.lastEventId();

    Employee employee1 = transactionTemplate.execute(s->em.merge(employee));

    lastEvents = entityEventRepository.after(lastId);

    EntityUpdateEvent<Employee> event4 = cast(lastEvents.get(0));;
    Assertions.assertEquals("version",event4.getNewStates().keySet().stream().findFirst().orElse(""));

    AddToCollectionEvent<Project> event5 = cast(lastEvents.get(1));;
    Assertions.assertEquals("phones",event5.getPropertyName());

    employee1.getAddress().setStreet("New Street");
    employee1.getAddress().setPostcode(56788);

    lastId = entityEventRepository.lastEventId();

    Employee employee2 = transactionTemplate.execute(s->em.merge(employee1));

    lastEvents = entityEventRepository.after(lastId);

    EntityUpdateEvent<Employee> event51 = cast(lastEvents.get(0));;

    Assertions.assertNotNull(event51.getNewStates().get("address.street"));
    Assertions.assertNotNull(event51.getNewStates().get("version"));
    Assertions.assertNotNull(event51.getNewStates().get("address.postcode"));

    employee2.getAttributes().put("attribute1","value1");

    lastId = entityEventRepository.lastEventId();

    Employee employee3 = transactionTemplate.execute(s->em.merge(employee2));

    lastEvents = entityEventRepository.after(lastId);

    EntityUpdateEvent<Employee> event6 = cast(lastEvents.get(0));;
    Assertions.assertEquals("version",event6.getNewStates().keySet().stream().findFirst().orElse(""));

    AddToMapEvent<Employee> event7 = cast(lastEvents.get(1));
    Assertions.assertEquals("attributes",event7.getPropertyName());
    Assertions.assertEquals("value1",event7.getAddedEntries().get("attribute1"));

    employee3.getAttributes().put("attribute1","changed value");

    lastId = entityEventRepository.lastEventId();

    Employee employee4 = transactionTemplate.execute(s->em.merge(employee3));

    lastEvents = entityEventRepository.after(lastId);

    EntityUpdateEvent<Employee> event8 = cast(lastEvents.get(0));
    Assertions.assertEquals("version",event8.getNewStates().keySet().stream().findFirst().orElse(""));

    EntityUpdateEvent<Employee> event9 = cast(lastEvents.get(1));
    Assertions.assertEquals("attributes.attribute1", event9.getNewStates().keySet().stream().findFirst().orElse(""));


  }

  private <T> T cast(AbstractEntityEvent<?> event){
    return (T) event;
  }



}
