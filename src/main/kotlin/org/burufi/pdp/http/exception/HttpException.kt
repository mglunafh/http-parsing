package org.burufi.pdp.http.exception

sealed class HttpException: RuntimeException()

sealed class HttpRequestException: HttpException()

class UnrecognizedRequestMethod(val value: String) : HttpRequestException() {
    override val message = "Could not understand the HTTP method: '$value'"
}

class UnrecognizedHttpVersion(val value: String) : HttpRequestException() {
    override val message = "Could not parse HTTP version: '$value'"
}

sealed class IncorrectFieldLineException: HttpRequestException()

data class FieldLineContainsNoValue(val value: String): IncorrectFieldLineException() {
    override val message = "Field line is incomplete: '$value'"
}

data class FieldNameContainsTrailingSpaces(val value: String) : IncorrectFieldLineException() {
    override val message = "Field name contains trailing spaces: '$value'"
}

data class FieldNameContainsForbiddenSymbols(val line: String, val token: String) : IncorrectFieldLineException() {
    override val message = "Field name contains forbidden characters: '$token', line '$line'"
}
