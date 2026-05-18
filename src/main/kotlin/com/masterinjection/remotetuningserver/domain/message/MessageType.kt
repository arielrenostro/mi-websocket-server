package com.masterinjection.remotetuningserver.domain.message

enum class MessageType {

    // Generic (bidirectional or server → client push)
    REGISTERED,
    ERROR,
    ECHO_SERIAL_DATA,
    PAIR_CONNECTED,
    PAIR_DISCONNECTED,

    // Server → Tuner push
    REGISTER_TO_CUSTOMER_RESPONSE,

    // Server → Customer push
    REGISTER_TUNER,
}
