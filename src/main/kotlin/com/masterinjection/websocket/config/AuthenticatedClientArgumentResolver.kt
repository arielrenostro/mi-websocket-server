package com.masterinjection.websocket.config

import com.masterinjection.websocket.config.ClientAuthInterceptor.Companion.ATTR_AUTHENTICATED_CLIENT
import com.masterinjection.websocket.domain.AuthenticatedClient
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedClientArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == AuthenticatedClient::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthenticatedClient {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)!!
        return request.getAttribute(ATTR_AUTHENTICATED_CLIENT) as AuthenticatedClient
    }
}
