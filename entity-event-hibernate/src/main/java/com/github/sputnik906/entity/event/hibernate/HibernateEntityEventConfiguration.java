package com.github.sputnik906.entity.event.hibernate;

import com.github.sputnik906.entity.event.api.AuthorProvider;
import com.github.sputnik906.entity.event.api.repo.EntityEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateEntityEventConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AuthorProvider authorProvider(){
    return new MockAuthorProvider();
  }

  @Bean
  @ConditionalOnMissingBean
  public EntityEventRepository entityEventRepository(){
    return new InMemorySimpleEntityEventRepository();
  }

}
