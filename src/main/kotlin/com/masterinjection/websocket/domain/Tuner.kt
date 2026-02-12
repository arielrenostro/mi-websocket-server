package com.masterinjection.websocket.domain

import org.springframework.web.socket.WebSocketSession

data class Tuner(
    val id: String,
    val name: String,
    val session: WebSocketSession,
    var customerConnected: Customer? = null,
)