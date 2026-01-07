package org.burufi.pdp.http

data class HttpRequest(
    val requestLine: String,
    val method: HttpMethod,
    val version: HttpVersion,
    val url: String,
    val headerLines: List<String>,
    val body: String?
)
