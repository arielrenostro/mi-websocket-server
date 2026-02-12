package com.masterinjection.websocket.controller.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/health-check")
class HealthCheckController {

    @GetMapping("/")
    fun healthCheck() {

    }
}