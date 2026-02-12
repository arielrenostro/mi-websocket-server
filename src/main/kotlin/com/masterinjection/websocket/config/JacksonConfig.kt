package com.masterinjection.websocket.config

import com.masterinjection.websocket.extension.GlobalMapper
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import tools.jackson.databind.ObjectMapper

@Configuration
class JacksonConfig(
    private val mapper: ObjectMapper,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun registerGlobal() {
        GlobalMapper.register(this.mapper)
    }
}