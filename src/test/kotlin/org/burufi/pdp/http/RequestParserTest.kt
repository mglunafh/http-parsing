package org.burufi.pdp.http

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.burufi.pdp.http.exception.HttpRequestException
import org.burufi.pdp.http.exception.UnrecognizedHttpVersion
import org.burufi.pdp.http.exception.UnrecognizedRequestMethod
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource


class RequestParserTest {

    @Test
    fun someTest() {
        println('\r'.code)
        println('\n'.code)
        println('\t'.code)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("methods")
    fun `Test HTTP method support`(request: String, expected: HttpRequest) {
        val result = RequestParser.parse(request)
        assertThat(result).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    fun `Test HTTP version support`(request: String, expected: HttpRequest) {
        val result = RequestParser.parse(request)
        assertThat(result).isEqualTo(expected)
    }

    @Nested
    inner class IncorrectRequestLine {

        @Test
        fun `Unknown method`() {
            assertThatThrownBy { RequestParser.parse("""TAKE / HTTP/1.1""".withRN()) }
                .isInstanceOf(UnrecognizedRequestMethod::class.java)
                .extracting("value")
                .isEqualTo("TAKE")
        }

        @Test
        fun `Unknown version`() {
            assertThatThrownBy { RequestParser.parse("""GET / HTTP/0.1""".withRN()) }
                .isInstanceOf(UnrecognizedHttpVersion::class.java)
                .extracting("value")
                .isEqualTo("HTTP/0.1")
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                " GET / HTTP/1.0\r\n\r\n",
                "GET /  HTTP/1.0\r\n\r\n",
                "GET  / HTTP/1.0\r\n\r\n"
            ]
        )
        fun `Incorrect space formatting`(request: String) {
            assertThatThrownBy { RequestParser.parse(request) }
                .isInstanceOf(HttpRequestException::class.java)
        }
    }

    companion object {

        @JvmStatic
        fun methods() = listOf<Arguments>(
            Arguments.of("GET / HTTP/1.1\r\n", HttpRequest("GET / HTTP/1.1", HttpMethod.GET, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("HEAD / HTTP/1.1\r\n", HttpRequest("HEAD / HTTP/1.1", HttpMethod.HEAD, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("POST / HTTP/1.1\r\n", HttpRequest("POST / HTTP/1.1", HttpMethod.POST, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("PUT / HTTP/1.1\r\n", HttpRequest("PUT / HTTP/1.1", HttpMethod.PUT, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("DELETE / HTTP/1.1\r\n", HttpRequest("DELETE / HTTP/1.1", HttpMethod.DELETE, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("CONNECT / HTTP/1.1\r\n", HttpRequest("CONNECT / HTTP/1.1", HttpMethod.CONNECT, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("OPTIONS / HTTP/1.1\r\n", HttpRequest("OPTIONS / HTTP/1.1", HttpMethod.OPTIONS, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("TRACE / HTTP/1.1\r\n", HttpRequest("TRACE / HTTP/1.1", HttpMethod.TRACE, HttpVersion.HTTP11, "/", listOf(), null)),
        )

        @JvmStatic
        fun versions() = listOf<Arguments>(
            Arguments.of("GET / HTTP/1.0\r\n", HttpRequest("GET / HTTP/1.0", HttpMethod.GET, HttpVersion.HTTP10, "/", listOf(), null)),
            Arguments.of("GET / HTTP/1.1\r\n", HttpRequest("GET / HTTP/1.1", HttpMethod.GET, HttpVersion.HTTP11, "/", listOf(), null)),
            Arguments.of("GET / HTTP/2\r\n", HttpRequest("GET / HTTP/2", HttpMethod.GET, HttpVersion.HTTP2, "/", listOf(), null)),
            Arguments.of("GET / HTTP/3\r\n", HttpRequest("GET / HTTP/3", HttpMethod.GET, HttpVersion.HTTP3, "/", listOf(), null))
        )

        private fun String.withRN() = this + "\r\n"
    }
}
