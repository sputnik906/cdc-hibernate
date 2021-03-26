package component.domain.project.entity;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.sun.istack.NotNull;
import component.domain.project.utils.IdentifiableLong;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldNameConstants
@RequiredArgsConstructor
@Getter
@Entity
@CdcEntity
public class Project extends IdentifiableLong {

  @NotNull
  @NonNull
  private String label;

  @OneToMany(
    fetch = FetchType.LAZY,
    mappedBy = Task.Fields.project,
    cascade = {CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE},
    orphanRemoval = true)
  private Set<Task> tasks = new HashSet<>();

  public Project addTasks(Set<Task> tasks) {
    this.tasks.addAll(tasks);
    tasks.forEach(t->t.setProject(this));
    return this;
  }
}
