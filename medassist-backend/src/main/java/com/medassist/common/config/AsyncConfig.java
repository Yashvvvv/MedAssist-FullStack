package com.medassist.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Async configuration with proper thread pool and exception handling.
 * This ensures async operations are properly managed and exceptions are logged.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("MedAssist-Async-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            logger.error("Task rejected from async executor. Queue is full.");
            throw new RuntimeException("Async task queue is full. Please try again later.");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * Custom exception handler for uncaught async exceptions.
     * Ensures all async failures are properly logged.
     */
    private static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
            logger.error("Uncaught async exception in method: {} with parameters: {}",
                    method.getName(), params, throwable);

            // Log additional details for debugging
            logger.error("Exception type: {}", throwable.getClass().getName());
            logger.error("Exception message: {}", throwable.getMessage());

            if (throwable.getCause() != null) {
                logger.error("Root cause: {}", throwable.getCause().getMessage());
            }
        }
    }
}
