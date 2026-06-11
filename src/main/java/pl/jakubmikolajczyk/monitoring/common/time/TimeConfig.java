package pl.jakubmikolajczyk.monitoring.common.time;

import java.time.InstantSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Single injectable source of "now". Production uses the system clock; tests can
/// substitute a fixed [java.time.Clock], which makes time-dependent logic (audit
/// timestamps, frequency windows) deterministic.
@Configuration(proxyBeanMethods = false)
class TimeConfig {

    @Bean
    InstantSource instantSource() {
        return InstantSource.system();
    }
}
