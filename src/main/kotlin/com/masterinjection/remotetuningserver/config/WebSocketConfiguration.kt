package com.masterinjection.remotetuningserver.config

import com.masterinjection.remotetuningserver.controller.ws.CustomerWebSocket
import com.masterinjection.remotetuningserver.controller.ws.TunerWebSocket
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfiguration(
    private val tunerWebSocket: TunerWebSocket,
    private val customerWebSocket: CustomerWebSocket,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(tunerWebSocket, "/ws/tuner")
            .setAllowedOrigins("*")
            .addInterceptors(Interceptor())

        registry.addHandler(customerWebSocket, "/ws/customer")
            .setAllowedOrigins("*")
            .addInterceptors(Interceptor())
    }

    private class Interceptor : HandshakeInterceptor {

        override fun beforeHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>,
        ): Boolean {
            val headers = request.headers
            headers.getFirst("X-Client-Id")?.let { attributes["clientId"] = it }
            headers.getFirst("X-Client-Secret")?.let { attributes["clientSecret"] = it }
            headers.getFirst("X-Client-Name")?.let { attributes["clientName"] = it }
            return true
        }

        override fun afterHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            exception: Exception?,
        ) = Unit
    }
}
