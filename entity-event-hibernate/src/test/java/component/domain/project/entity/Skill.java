package component.domain.project.entity;

import com.github.sputnik906.entity.event.api.annotation.CdcEntity;
import com.sun.istack.NotNull;
import component.domain.project.utils.IdentifiableLong;
import javax.persistence.Entity;
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
public class Skill extends IdentifiableLong {

  @NotNull @NonNull @Setter
  private String label;


}
