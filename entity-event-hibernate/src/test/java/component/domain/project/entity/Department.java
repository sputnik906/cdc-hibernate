package component.domain.project.entity;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.sun.istack.NotNull;
import component.domain.project.utils.IdentifiableLong;
import java.util.Set;
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
public class Department extends IdentifiableLong {

  @NotNull
  @NonNull
  private String label;

  @OneToMany(fetch = FetchType.LAZY)
  private Set<Employee> employees;

}
