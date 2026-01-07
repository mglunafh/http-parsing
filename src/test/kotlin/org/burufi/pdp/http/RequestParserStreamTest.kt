package org.burufi.pdp.http;

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.burufi.pdp.http.exception.FieldLineContainsNoValue
import org.burufi.pdp.http.exception.FieldNameContainsForbiddenSymbols
import org.burufi.pdp.http.exception.FieldNameContainsTrailingSpaces
import org.burufi.pdp.http.exception.IncorrectFieldLineException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class RequestParserStreamTest {

    @ParameterizedTest
    @MethodSource("methods")
    fun `Test HTTP method support`(resource: String, expected: HttpRequest) {
        val inputStream = javaClass.classLoader.getResourceAsStream(resource)
        val request = inputStream?.use { RequestParser.parse(inputStream) }
        assertThat(request).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("versions")
    fun `Test HTTP version support`(resource: String, expected: HttpRequest) {
        val inputStream = javaClass.classLoader.getResourceAsStream(resource)
        val request = inputStream?.use { RequestParser.parse(inputStream) }
        assertThat(request).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("headers")
    fun `Test headers`(resource: String, expected: HttpRequest) {
        val inputStream = javaClass.classLoader.getResourceAsStream(resource)
        val request = inputStream?.use { RequestParser.parse(inputStream) }
        assertThat(request).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("malformedHeaders")
    fun `Test malformed headers`(resource: String, expected: IncorrectFieldLineException) {
        val inputStream = javaClass.classLoader.getResourceAsStream(resource)
        assertThatThrownBy { inputStream?.use { RequestParser.parse(inputStream) } }
            .isEqualTo(expected)
    }

    companion object {

        @JvmStatic
        fun methods() = listOf<Arguments>(
            Arguments.of("request/request-line-only/get.txt", HttpRequest("GET / HTTP/1.1", HttpMethod.GET, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/head.txt", HttpRequest("HEAD / HTTP/1.1", HttpMethod.HEAD, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/post.txt", HttpRequest("POST / HTTP/1.1", HttpMethod.POST, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/put.txt", HttpRequest("PUT / HTTP/1.1", HttpMethod.PUT, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/delete.txt", HttpRequest("DELETE / HTTP/1.1", HttpMethod.DELETE, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/connect.txt", HttpRequest("CONNECT / HTTP/1.1", HttpMethod.CONNECT, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/options.txt", HttpRequest("OPTIONS / HTTP/1.1", HttpMethod.OPTIONS, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/trace.txt", HttpRequest("TRACE / HTTP/1.1", HttpMethod.TRACE, HttpVersion.HTTP11, "/", listOf(), null)),
        )

        @JvmStatic
        fun versions() = listOf<Arguments>(
            Arguments.of("request/request-line-only/get-1.0.txt", HttpRequest("GET / HTTP/1.0", HttpMethod.GET, HttpVersion.HTTP10, "/", listOf(), null)),
            Arguments.of("request/request-line-only/get-1.1.txt", HttpRequest("GET / HTTP/1.1", HttpMethod.GET, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("request/request-line-only/get-2.txt", HttpRequest("GET / HTTP/2", HttpMethod.GET, HttpVersion.HTTP2, "/", listOf(), null)),
            Arguments.of("request/request-line-only/get-3.txt", HttpRequest("GET / HTTP/3", HttpMethod.GET, HttpVersion.HTTP3, "/", listOf(), null))
        )

        @JvmStatic
        fun headers() = listOf<Arguments>(
            Arguments.of(
                "request/without-body/get.txt",
                HttpRequest(
                    requestLine = "GET / HTTP/1.1",
                    method = HttpMethod.GET,
                    version = HttpVersion.HTTP11,
                    url = "/",
                    headerLines = listOf("Host: localhost:8080", "User-Agent: curl/8.2.1", "Accept: */*", "Content-Type: application/json"),
                    body = null
                )
            )
        )

        @JvmStatic
        fun malformedHeaders() = listOf<Arguments>(
            Arguments.of("request/without-body/get-host-no-colon.txt",
                FieldNameContainsForbiddenSymbols(line = "Host localhost:8080", token = "Host localhost")
            ),
            Arguments.of("request/without-body/get-host.txt",
                FieldNameContainsTrailingSpaces("Host : localhost:8080")),
            Arguments.of("request/without-body/get-no-host.txt",
                FieldLineContainsNoValue("Host:")),
            Arguments.of("request/without-body/get-empty-host.txt",
                FieldLineContainsNoValue("Host:   ")),
        )
    }
}
