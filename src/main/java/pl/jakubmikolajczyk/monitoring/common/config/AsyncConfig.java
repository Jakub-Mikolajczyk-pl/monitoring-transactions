package pl.jakubmikolajczyk.monitoring.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/// Enables @Async processing. With `spring.threads.virtual.enabled=true` the default
/// executor starts each task on a fresh virtual thread (ADR-0006) - no pool to size,
/// no queue to tune; blocking JDBC calls park the carrier thread for free.
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig {
}
