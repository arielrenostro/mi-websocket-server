package com.masterinjection.websocket.domain.message

enum class MessageType {

    // Generic
    REGISTERED,
    ERROR,
    GET_STATE,
    STATE,
    ECHO_SERIAL_DATA,
    PAIR_DISCONNECTED,

    // Tuner to Server Messages
    LIST_CUSTOMERS,
    REGISTER_TO_CUSTOMER,

    // Server to Tuner Messages
    LIST_CUSTOMERS_RESPONSE,
    REGISTER_TO_CUSTOMER_RESPONSE,

    // Customer to Server Messages
    REGISTER_TUNER_RESPONSE,

    // Server to Customer Messages
    REGISTER_TUNER
}