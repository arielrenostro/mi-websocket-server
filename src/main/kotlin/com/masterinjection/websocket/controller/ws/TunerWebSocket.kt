package com.masterinjection.websocket.controller.ws

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.extension.sendJsonError
import com.masterinjection.websocket.service.TuningSessionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper

@Component
class TunerWebSocket(
    private val tuningSessionService: TuningSessionService,
    private val mapper: ObjectMapper,
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(TunerWebSocket::class.java)

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val message = this.mapper.readValue(message.payload, BaseMessage::class.java)
            this.tuningSessionService.onTunerMessage(session, message)
        } catch (e: Exception) {
            this.logger.warn("Failure on Tuner WebSocket. Session: ${session.id} Message: ${message.payload} Error: ${e.message}")
            session.sendJsonError(null, e.message)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        this.logger.info("New Tuner Connected: ${session.id} - ${session.attributes["name"]}")
        this.tuningSessionService.registerTuner(session, session.attributes["name"] as String?)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        this.logger.info("Tuner Disconnected: ${session.id} - ${session.attributes["name"]} - $status")
        this.tuningSessionService.unregisterTuner(session, status)
    }
}