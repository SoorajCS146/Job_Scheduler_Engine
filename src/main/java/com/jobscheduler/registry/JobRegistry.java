package com.jobscheduler.registry;

import com.jobscheduler.handler.JobTypeHandler;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that maintains a mapping of JobType to JobTypeHandler.
 * Automatically registers all handlers via Spring dependency injection.
 * This eliminates the need for switch statements in the executor.
 */
@Slf4j
@Component
public class JobRegistry {
    
    private final Map<JobType, JobTypeHandler> handlers = new ConcurrentHashMap<>();
    /**
     * Constructor that auto-registers all JobTypeHandler beans.
     * Spring automatically injects all implementations of JobTypeHandler.
     * 
     * @param handlerList List of all JobTypeHandler beans found by Spring
     */
    @Autowired
    public JobRegistry(List<JobTypeHandler> handlerList) {
        for (JobTypeHandler handler : handlerList) {
            register(handler);
        }
        log.info("✓ JobRegistry initialized with {} handlers", handlers.size());
    }
    
    /**
     * Register a handler for a specific job type.
     * 
     * @param handler The handler to register
     */
    public void register(JobTypeHandler handler) {
        JobType type = handler.getType();
        handlers.put(type, handler);
        System.out.println("  ✓ Registered handler: " + type + " -> " + handler.getClass().getSimpleName());
    }
    
    /**
     * Get the handler for a specific job type.
     * 
     * @param type The job type
     * @return The handler for that type
     * @throws IllegalArgumentException if no handler is registered for the type
     */
    public JobTypeHandler getHandler(JobType type) {
        JobTypeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for job type: " + type);
        }
        return handler;
    }
    
    /**
     * Check if a handler is registered for a specific job type.
     * 
     * @param type The job type
     * @return true if a handler is registered, false otherwise
     */
    public boolean hasHandler(JobType type) {
        return handlers.containsKey(type);
    }
}

