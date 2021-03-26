package component.domain.project.entity;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.sun.istack.NotNull;
import component.domain.project.utils.IdentifiableLong;
import component.domain.project.valueobject.Address;
import component.domain.project.valueobject.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
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
public class Employee extends IdentifiableLong {

  @NotNull
  @NonNull
  private String label;

  @ManyToMany
  @NonNull @NotNull private Set<Skill> skills;

  @ElementCollection
  private List<String> phones = new ArrayList<>();

  @Embedded @NotNull @NonNull
  private Address address;

  @ElementCollection
  private Map<String,String> attributes = new HashMap<>();

  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  private int age;

  @Embedded @Setter
  @NonNull
  private Position position;
}
