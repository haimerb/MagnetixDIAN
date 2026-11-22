package com.magnetixdian.infrastructure.dian;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenDianAdapterSpringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TokenDianAdapter.class);

    @Test
    void creaBeanConPropiedadesPorDefecto() {
        contextRunner.run(context -> assertNotNull(context.getBean(TokenDianAdapter.class)));
    }
}
