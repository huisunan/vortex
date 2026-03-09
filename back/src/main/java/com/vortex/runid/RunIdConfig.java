package com.vortex.runid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RunIdConfig {

    @Bean("httpRunIdGenerator")
    public RunIdGenerator httpRunIdGenerator() {
        return new HttpRunIdGenerator();
    }

    @Bean("uuidRunIdGenerator")
    public RunIdGenerator uuidRunIdGenerator() {
        return new UuidRunIdGenerator();
    }

    @Bean
    public RunIdService runIdService(
            @Qualifier("httpRunIdGenerator") RunIdGenerator httpGenerator,
            @Qualifier("uuidRunIdGenerator") RunIdGenerator uuidGenerator
    ) {
        return new RunIdService(httpGenerator, uuidGenerator);
    }
}
