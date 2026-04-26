package com.hieu.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Two dedicated thread pools: sagaExecutor for parallel saga steps, eventExecutor for async events. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "sagaExecutor")
    public Executor sagaExecutor() {
        return new ThreadPoolExecutor(8, 32, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                r -> new Thread(r, "order-saga-" + System.nanoTime()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        return new ThreadPoolExecutor(4, 16, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> new Thread(r, "order-event-" + System.nanoTime()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
