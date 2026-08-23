package com.alquiler.furent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publicador centralizado de eventos del dominio.
 * Usa Spring ApplicationEventPublisher como bus interno.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ApplicationEventPublisher publisher;

    public EventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(FurentEvent event) {
        log.info("Publicando evento: {} [tenant={}, user={}]",
                event.getClass().getSimpleName(), event.getTenantId(), event.getUserId());
        publisher.publishEvent(event);
    }
}
