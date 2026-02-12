package com.masterinjection.websocket.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.masterinjection"])
class WebsocketServerApplication

fun main(args: Array<String>) {
	runApplication<WebsocketServerApplication>(*args)
}
