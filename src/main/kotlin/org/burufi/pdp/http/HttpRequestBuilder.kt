package org.burufi.pdp.http

import org.burufi.pdp.http.exception.UnrecognizedHttpVersion
import org.burufi.pdp.http.exception.UnrecognizedRequestMethod

class HttpRequestBuilder {

    private lateinit var requestLine: String
    private lateinit var method: HttpMethod
    private lateinit var url: String
    private lateinit var version: HttpVersion
    private val headerLines: MutableList<String> = mutableListOf()
    private lateinit var body: String

    fun addLine(line: String) {
        if (line.isEmpty()) {
            return
        }
        if (!::requestLine.isInitialized) {
            setHttpRequestLine(line)
        }
        else {
            headerLines.add(line)
        }
    }

    fun setHttpBody(body: String?) {
        body?.let { this.body = it}
    }

    fun toRequest(): HttpRequest {
        return HttpRequest(
            requestLine = this.requestLine,
            method = this.method,
            version = this.version,
            url = this.url,
            headerLines = this.headerLines,
            body = if (::body.isInitialized) body else null
        )
    }

    fun setHttpRequestLine(line: String) {
        val lineSplit = line.split(' ', limit = 3)
        require(lineSplit.size == 3)
        method = lineSplit[0].let {
            HttpMethod.fromValue(it) ?: throw UnrecognizedRequestMethod(it)
        }

        url = lineSplit[1]

        version = lineSplit[2].let {
            HttpVersion.fromValue(it) ?: throw UnrecognizedHttpVersion(it)
        }
        requestLine = line
    }
}
