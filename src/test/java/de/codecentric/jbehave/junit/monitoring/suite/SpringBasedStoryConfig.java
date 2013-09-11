package de.codecentric.jbehave.junit.monitoring.suite;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JBehaveSpringConfig.class)
@ComponentScan(
        basePackages = {
                "de.codecentric.jbehave.junit.monitoring.step"
        }
)
public class SpringBasedStoryConfig {
}
