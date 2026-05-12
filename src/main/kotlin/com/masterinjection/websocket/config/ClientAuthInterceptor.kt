package com.masterinjection.websocket.config

import com.masterinjection.websocket.service.TuningSessionService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ClientAuthInterceptor(
    private val tuningSessionService: TuningSessionService,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val id = request.getHeader("X-Client-Id")
            ?: throw SecurityException("Missing required header: X-Client-Id")
        val secret = request.getHeader("X-Client-Secret")
            ?: throw SecurityException("Missing required header: X-Client-Secret")

        val client = tuningSessionService.authenticate(id, secret)
        request.setAttribute(ATTR_AUTHENTICATED_CLIENT, client)
        return true
    }

    companion object {
        const val ATTR_AUTHENTICATED_CLIENT = "authenticatedClient"
    }
}
