package component.domain.project.utils;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import lombok.Getter;
import lombok.ToString;

@MappedSuperclass
@ToString
@Getter
public abstract class IdentifiableLong {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  @Version
  protected Long version;

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (id == null) return super.equals(obj);
    if (this == obj) return true;
    if (obj == null) return false;
    if (!getClass().isAssignableFrom(obj.getClass())) return false;
    IdentifiableLong other = (IdentifiableLong) obj;
    return id.equals(other.getId());
  }
}
