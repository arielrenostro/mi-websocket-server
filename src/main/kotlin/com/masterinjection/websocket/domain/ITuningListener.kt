package com.masterinjection.websocket.domain

import com.masterinjection.websocket.domain.Tuner
import org.springframework.web.socket.CloseStatus

interface ITuningListener {

    fun onCustomerRegister(customer: Customer)
    fun onCustomerUnregister(customer: Customer, status: CloseStatus): Int
    fun onTunerUnregister(tuner: Tuner, status: CloseStatus): Int

}