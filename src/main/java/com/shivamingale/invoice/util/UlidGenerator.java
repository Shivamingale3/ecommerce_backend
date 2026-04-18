package com.shivamingale.invoice.util;

import com.github.f4b6a3.ulid.UlidCreator;
import java.io.Serializable;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

@IdGeneratorType(UlidGenerator.Generator.class)
public @interface UlidGenerator {

    class Generator implements IdentifierGenerator {

        @Override
        public Serializable generate(SharedSessionContractImplementor session, Object object) {
            return UlidCreator.getUlid().toString();
        }
    }
}
