package org.burufi.pdp.http

import org.burufi.pdp.http.RequestParser.SearchIndex.Companion.ENDS_WITH_CARRIAGE_RETURN
import org.burufi.pdp.http.RequestParser.SearchIndex.Companion.NOT_FOUND
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader

object RequestParser {
    const val CARRIAGE_RETURN = '\r'.code.toByte()
    const val LINE_FEED = '\n'.code.toByte()


    const val bufferSize = 8
    const val lineBufferSize = 24
    const val lineByteSize = 1500


    fun parseFromFile(resourceName: String): HttpRequest {
        val request = javaClass.classLoader.getResourceAsStream(resourceName)
            ?.let { inputStream ->
                inputStream.use { InputStreamReader(it, Charsets.UTF_8).use { utf8Stream -> parse(utf8Stream) } }
            }
        return request!!
    }

    fun parse(request: String) = parseStateMachine(StringReader(request))

    fun parse(stream: InputStream): HttpRequest {
        val buffer = ByteArray(bufferSize)
        val lineBuffer = ByteArray(lineByteSize)
        val builder = HttpRequestBuilder1()

        var offset = 0
        var httpLineStart = 0
        var readBytes = stream.read(buffer)
        var state = ParsingState.REQUEST_LINE
        var expectBody = false

        while (readBytes != -1) {
            when (state) {
                ParsingState.REQUEST_LINE -> {
                    when (val idx = findLineBreak(buffer, httpLineStart, readBytes)) {
                        NOT_FOUND -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, readBytes)
                            offset += readBytes - httpLineStart
                            httpLineStart = 0
                            readBytes = stream.read(buffer)
                        }
                        ENDS_WITH_CARRIAGE_RETURN -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, readBytes - 1)
                            offset += readBytes - httpLineStart - 1
                            buffer[0] = CARRIAGE_RETURN
                            httpLineStart = 0
                            readBytes = 1 + stream.read(buffer, 1, bufferSize - 1)
                        }
                        else -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, idx.index)
                            offset += idx.index - httpLineStart
                            httpLineStart = idx.index + 2
                            if (offset > 0) {
                                val requestLine = lineBuffer.decodeToString(endIndex = offset)
                                builder.setRequestLine(requestLine)
                                state = ParsingState.HEADER_LINE
                                offset = 0
                            }
                        }
                    }
                }
                ParsingState.HEADER_LINE -> {
                    when (val idx = findLineBreak(buffer, httpLineStart, readBytes)) {
                        NOT_FOUND -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, readBytes)
                            offset += readBytes - httpLineStart
                            httpLineStart = 0
                            readBytes = stream.read(buffer)
                        }
                        ENDS_WITH_CARRIAGE_RETURN -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, readBytes - 1)
                            offset += readBytes - httpLineStart - 1
                            buffer[0] = CARRIAGE_RETURN
                            httpLineStart = 0
                            readBytes = 1 + stream.read(buffer, 1, bufferSize - 1)
                        }
                        else -> {
                            buffer.copyInto(lineBuffer, offset, httpLineStart, idx.index)
                            offset += idx.index - httpLineStart
                            httpLineStart = idx.index + 2
                            if (offset == 0) {
                                if (expectBody) {
                                    state = ParsingState.BODY
                                } else {
                                    break
                                }
                            } else {
                                val headerLine = lineBuffer.decodeToString(endIndex = offset)
                                builder.addFieldLine(headerLine)
                                offset = 0
                            }
                        }
                    }
                }
                ParsingState.BODY -> {
                    buffer.copyInto(lineBuffer, offset, httpLineStart, readBytes)
                    offset += readBytes - httpLineStart
                    httpLineStart = 0
                    readBytes = stream.read(buffer)
                }
            }
        }

        if (offset > 0) {
            when (state) {
                ParsingState.REQUEST_LINE -> {
                    val line = lineBuffer.decodeToString(endIndex = offset)
                    builder.setRequestLine(line)
                }
                ParsingState.HEADER_LINE -> {
                    val line = lineBuffer.decodeToString(endIndex = offset)
                    builder.addFieldLine(line)
                }
                ParsingState.BODY -> {
                    val body = lineBuffer.decodeToString(endIndex = offset)
                    builder.setBody(body)
                }
            }
        }

        return builder.toRequest()
    }

    private fun parse(isr: Reader): HttpRequest {
        val buffer = CharArray(bufferSize)
        val lineBuffer = StringBuilder(lineBufferSize)
        val builder = HttpRequestBuilder()

        var readChars = isr.read(buffer)
        var parseBody = false
        while (readChars != -1) {
            if (parseBody) {
                lineBuffer.append(buffer, 0, readChars)
                readChars = isr.read(buffer)
                continue
            }

            var httpLineStart = 0
            for (i in 0 ..< readChars - 1) {
                if (buffer[i] == '\r' && buffer [i + 1] == '\n') {
                    lineBuffer.append(buffer, httpLineStart, i - httpLineStart)
                    httpLineStart = i + 2
                    if (lineBuffer.isEmpty()) {
                        parseBody = true
                        break
                    }
                    val line = lineBuffer.toString()
                    builder.addLine(line)
                    lineBuffer.clear()
                }
            }
            if (parseBody) {
                val amount = readChars - httpLineStart
                lineBuffer.append(buffer, httpLineStart, amount)
                readChars = isr.read(buffer)
            } else if (buffer[readChars - 1] == '\r') {
                val amount = readChars - httpLineStart - 1
                lineBuffer.append(buffer, httpLineStart, amount)
                buffer[0] = '\r'
                readChars = 1 + isr.read(buffer, 1, bufferSize - 1)
            } else {
                if (httpLineStart < readChars) {
                    val amount = readChars - httpLineStart
                    lineBuffer.append(buffer, httpLineStart, amount)
                }
                readChars = isr.read(buffer)
            }
        }
        if (lineBuffer.isNotEmpty()) {
            val line = lineBuffer.toString()
            if (parseBody) {
                builder.setHttpBody(line)
            } else {
                builder.addLine(line)
            }
        }
        return builder.toRequest()
    }

    fun parseStateMachine(isr: Reader): HttpRequest {
        val buffer = CharArray(bufferSize)
        val lineBuffer = StringBuilder(lineBufferSize)
        val builder = HttpRequestBuilder()

        var httpLineStart = 0
        var readChars = isr.read(buffer)
        var state = ParsingState.REQUEST_LINE

        while (readChars != -1) {
            when (state) {
                ParsingState.REQUEST_LINE -> {
                    when (val idx = findLineBreak(buffer, 0, readChars)) {
                        -1 -> {
                            lineBuffer.append(buffer, 0, readChars)
                            readChars = isr.read(buffer)
                        }
                        -2 -> {
                            lineBuffer.append(buffer, 0, readChars - 1)
                            buffer[0] = '\r'
                            httpLineStart = 1
                            readChars = isr.read(buffer, 1, bufferSize - 1)
                        }
                        else -> {
                            lineBuffer.append(buffer, 0, idx)
                            httpLineStart = idx + 2
                            if (!lineBuffer.isEmpty()) {
                                val line = lineBuffer.toString()
                                builder.setHttpRequestLine(line)
                                state = ParsingState.HEADER_LINE
                                lineBuffer.clear()
                            }
                        }
                    }
                }
                ParsingState.HEADER_LINE -> {
                    when (val idx = findLineBreak(buffer, httpLineStart, readChars)) {
                        -1 -> {
                            val amount = readChars - httpLineStart
                            lineBuffer.append(buffer, httpLineStart, amount)
                            httpLineStart = 0
                            readChars = isr.read(buffer)
                        }
                        -2 -> {
                            val amount = readChars - httpLineStart - 1
                            lineBuffer.append(buffer, httpLineStart, amount)
                            buffer[0] = '\r'
                            httpLineStart = 1
                            readChars = isr.read(buffer, 1, bufferSize - 1)
                        }
                        else -> {
                            val amount = idx - httpLineStart
                            lineBuffer.append(buffer, httpLineStart, amount)
                            httpLineStart = idx + 2
                            if (lineBuffer.isEmpty()) {
                                state = ParsingState.BODY
                            } else {
                                val line = lineBuffer.toString()
                                builder.addLine(line)
                                lineBuffer.clear()
                            }
                        }
                    }
                }
                ParsingState.BODY -> {
                    val amount = readChars - httpLineStart
                    lineBuffer.append(buffer, httpLineStart, amount)
                    readChars = isr.read(buffer)
                    httpLineStart = 0
                }
            }
        }
        if (lineBuffer.isNotEmpty()) {
            val line = lineBuffer.toString()
            when (state) {
                ParsingState.REQUEST_LINE -> builder.setHttpRequestLine(line)
                ParsingState.HEADER_LINE -> builder.addLine(line)
                ParsingState.BODY -> builder.setHttpBody(line)
            }
        }

        return builder.toRequest()
    }

    private fun findLineBreak(buffer: CharArray, from: Int, to: Int): Int {
        for (i in from ..< to - 1) {
            if (buffer[i] == '\r' && buffer [i + 1] == '\n') {
                return i
            }
        }
        return if (buffer[to - 1] == '\r') -2 else -1
    }

    private fun findLineBreak(buffer: ByteArray, from: Int, to: Int): SearchIndex {
        for (i in from ..< to - 1) {
            if (buffer[i] == CARRIAGE_RETURN && buffer[i + 1] == LINE_FEED) {
                return SearchIndex(i)
            }
        }
        return if (buffer[to - 1] == CARRIAGE_RETURN) ENDS_WITH_CARRIAGE_RETURN else NOT_FOUND
    }

    private enum class ParsingState {
        REQUEST_LINE, HEADER_LINE, BODY
    }


    @JvmInline
    private value class SearchIndex(val index: Int) {
        companion object {
            val NOT_FOUND = SearchIndex(-1)
            val ENDS_WITH_CARRIAGE_RETURN = SearchIndex(-2)
        }
    }
}