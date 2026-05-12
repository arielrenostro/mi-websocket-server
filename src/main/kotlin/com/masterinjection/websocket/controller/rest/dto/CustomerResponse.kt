package com.masterinjection.websocket.controller.rest.dto

import com.masterinjection.websocket.domain.ClientStatus

data class CustomerResponse(val id: String, val name: String, val status: ClientStatus)
