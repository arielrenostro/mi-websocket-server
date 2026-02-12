package com.masterinjection.websocket.domain.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.masterinjection.websocket.domain.message.customer.RegisterTunerRequestMessage
import com.masterinjection.websocket.domain.message.customer.RegisterTunerResponseMessage
import com.masterinjection.websocket.domain.message.generic.ErrorMessage
import com.masterinjection.websocket.domain.message.generic.GetStateRequestMessage
import com.masterinjection.websocket.domain.message.generic.GetStateResponseMessage
import com.masterinjection.websocket.domain.message.generic.RegisteredMessage
import com.masterinjection.websocket.domain.message.generic.EchoSerialDataMessage
import com.masterinjection.websocket.domain.message.generic.PairDisconnectedMessage
import com.masterinjection.websocket.domain.message.tuner.ListCustomersRequestMessage
import com.masterinjection.websocket.domain.message.tuner.ListCustomersResponseMessage
import com.masterinjection.websocket.domain.message.tuner.RegisterToCustomerRequestMessage
import com.masterinjection.websocket.domain.message.tuner.RegisterToCustomerResponseMessage

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
//    include = JsonTypeInfo.As.PROPERTY,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    Type(value = ListCustomersRequestMessage::class, name = "LIST_CUSTOMERS"),
    Type(value = RegisterToCustomerRequestMessage::class, name = "REGISTER_TO_CUSTOMER"),
    Type(value = RegisteredMessage::class, name = "REGISTERED"),
    Type(value = ErrorMessage::class, name = "ERROR"),
    Type(value = GetStateRequestMessage::class, name = "GET_STATE"),
    Type(value = GetStateResponseMessage::class, name = "STATE"),
    Type(value = ListCustomersResponseMessage::class, name = "LIST_CUSTOMERS_RESPONSE"),
    Type(value = RegisterToCustomerResponseMessage::class, name = "REGISTER_TO_CUSTOMER_RESPONSE"),
    Type(value = RegisterTunerResponseMessage::class, name = "REGISTER_TUNER_RESPONSE"),
    Type(value = RegisterTunerRequestMessage::class, name = "REGISTER_TUNER"),
    Type(value = EchoSerialDataMessage::class, name = "ECHO_SERIAL_DATA"),
    Type(value = PairDisconnectedMessage::class, name = "PAIR_DISCONNECTED"),
)
abstract class BaseMessage(
    /**
     * Unix timestamp
     */
    val timestamp: Long,
    /**
     * Message Type
     */
    val type: MessageType,
)
