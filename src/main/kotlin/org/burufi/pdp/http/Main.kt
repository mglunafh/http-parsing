package org.burufi.pdp.http

import java.net.ServerSocket


// мапа с заголовками,
// тест на регистронезависимость заголовков
// тест на несколько одинаковых заголовков
// тест на запятые https://datatracker.ietf.org/doc/html/rfc9110#name-field-values

fun main() {
    val server = ServerSocket(8080)
    println("Hi from http server running on ${server.localPort}")
    val bufferSize = 1024
    val buffer = ByteArray(bufferSize)
    val sb = StringBuilder()

    val client = server.accept()
    println("Client connected: ${client.inetAddress.hostAddress}")
    val clientInput = client.getInputStream()

    val request = RequestParser.parse(clientInput)
    println(request)
    client.close()

//    var totalReadBytes = 0
//    var readBytes = clientInput.read(buffer)
//    while (readBytes != -1) {
//        totalReadBytes += readBytes
//        println("Read $readBytes bytes")
//        val str = String(buffer, 0, readBytes)
//        sb.append(str)
//        if (bufferSize > readBytes) {
//            break
//        }
//        if (client.isInputShutdown) {
//            break
//        }
//        readBytes = clientInput.read(buffer)
//    }
//    client.close()

    println(sb)
    server.close()
}

// curl -X POST -H "Content-Type: application/json" -d '{"hello": "everyone"}' http://localhost:8080

//
