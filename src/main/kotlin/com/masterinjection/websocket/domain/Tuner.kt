package com.masterinjection.websocket.domain

import org.springframework.web.socket.WebSocketSession

class Tuner(
    val id: String,
    val name: String,
    val secret: String,
    var wsSession: WebSocketSession? = null,
    var customerConnected: Customer? = null,
)