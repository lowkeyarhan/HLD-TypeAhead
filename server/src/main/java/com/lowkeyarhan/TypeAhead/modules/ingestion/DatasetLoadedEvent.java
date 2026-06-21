package com.lowkeyarhan.TypeAhead.modules.ingestion;

import org.springframework.context.ApplicationEvent;

// Event published when the database query count ingestion has successfully completed.
public class DatasetLoadedEvent extends ApplicationEvent {
    public DatasetLoadedEvent(Object source) {
        super(source);
    }
}
