package component.domain.project.entity;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.sun.istack.NotNull;
import component.domain.project.utils.IdentifiableLong;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldNameConstants
@RequiredArgsConstructor
@Getter
@Entity
@CdcEntity
public class Task extends IdentifiableLong {

  @NotNull
  @NonNull
  private String label;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @NotNull
  @Setter(AccessLevel.PROTECTED)
  private Project project;

  //TODO need to fix ManyToMany FetchType.Lazy - возникает ошибка при доступе к этому свойству
  // в слушателе PostDeleteEventListener onPostDelete(PostDeleteEvent event)
  @ManyToMany(fetch = FetchType.EAGER)
  @NonNull @NotNull private Set<Skill> requiredSkills;
}
