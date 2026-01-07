package org.burufi.pdp.http

import org.burufi.pdp.http.exception.FieldLineContainsNoValue
import org.burufi.pdp.http.exception.FieldNameContainsForbiddenSymbols
import org.burufi.pdp.http.exception.FieldNameContainsTrailingSpaces
import org.burufi.pdp.http.exception.UnrecognizedHttpVersion
import org.burufi.pdp.http.exception.UnrecognizedRequestMethod

class HttpRequestBuilder1 {

    var debug = true

    private lateinit var requestLine: String
    private lateinit var method: HttpMethod
    private lateinit var url: String
    private lateinit var version: HttpVersion
    private val fieldLines: MutableList<String> = mutableListOf()
    private lateinit var body: String

    private val tokenRegex = """^[-+._^$0-9a-zA-Z]+$""".toRegex()

    fun toRequest(): HttpRequest {
        return HttpRequest(
            requestLine = this.requestLine,
            method = this.method,
            version = this.version,
            url = this.url,
            headerLines = this.fieldLines,
            body = if (::body.isInitialized) body else null
        )
    }

    fun setRequestLine(line: String) {
        if (debug) {
            println(line)
        }
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

    fun addFieldLine(line: String) {
        if (debug) {
            println(line)
        }

        val lineSplit = line.split(':', limit = 2)
        if (lineSplit.size < 2) throw FieldLineContainsNoValue(line)

        val (fieldName, fieldValue) = lineSplit
        if (fieldName.endsWith(' ')) throw FieldNameContainsTrailingSpaces(line)

        if (!tokenRegex.matches(fieldName)) throw FieldNameContainsForbiddenSymbols(line, fieldName)

        val trimmedFieldValue = fieldValue.trim(' ')
        if (trimmedFieldValue.isEmpty()) throw FieldLineContainsNoValue(line)

        val header = "$fieldName: $trimmedFieldValue"
        fieldLines.add(header)
    }

    fun setBody(body: String?) {
        if (debug) {
            println(body)
        }

        body?.let { this.body = it}
    }
}