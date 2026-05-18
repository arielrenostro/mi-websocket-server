package com.masterinjection.remotetuningserver.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.masterinjection"])
class RemoteTuningServerApplication

fun main(args: Array<String>) {
	runApplication<RemoteTuningServerApplication>(*args)
}
