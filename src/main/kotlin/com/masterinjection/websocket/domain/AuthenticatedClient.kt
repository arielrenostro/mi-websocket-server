package com.masterinjection.websocket.domain

data class AuthenticatedClient(val id: String, val type: ClientType)

enum class ClientType { TUNER, CUSTOMER }
