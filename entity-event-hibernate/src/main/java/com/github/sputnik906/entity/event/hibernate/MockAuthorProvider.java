package com.github.sputnik906.entity.event.hibernate;

import com.github.sputnik906.entity.event.api.AuthorProvider;

public class MockAuthorProvider implements AuthorProvider {
    @Override
    public String provide() {
        return "unknown";
    }
}
