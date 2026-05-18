package com.masterinjection.remotetuningserver.controller.ws

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.extension.sendJsonError
import com.masterinjection.remotetuningserver.service.TuningSessionService
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
            val msg = mapper.readValue(message.payload, BaseMessage::class.java)
            tuningSessionService.onTunerMessage(session, msg)
        } catch (e: Exception) {
            logger.warn("Failure on Tuner WebSocket. Session: ${session.id} Error: ${e.message}")
            session.sendJsonError(null, e.message)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val id = session.attributes["clientId"] as String?
        val secret = session.attributes["clientSecret"] as String?
        val name = session.attributes["clientName"] as String?
        try {
            when {
                id != null && secret != null -> tuningSessionService.reconnectTunerWs(session, id, secret)
                name != null -> tuningSessionService.newTunerConnection(session, name)
                else -> {
                    logger.warn("Tuner WS rejected: missing X-Client-Name or X-Client-Id/X-Client-Secret headers")
                    session.close(CloseStatus.NOT_ACCEPTABLE)
                }
            }
        } catch (e: Exception) {
            logger.warn("Tuner WS connection rejected: ${e.message}")
            session.close(CloseStatus.NOT_ACCEPTABLE)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        tuningSessionService.disconnectTunerWs(session)
    }
}
