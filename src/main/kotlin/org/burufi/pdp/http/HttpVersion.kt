package org.burufi.pdp.http

enum class HttpVersion(val version: String) {

    HTTP10("HTTP/1.0"),
    HTTP11("HTTP/1.1"),
    HTTP2("HTTP/2"),
    HTTP3("HTTP/3");

    companion object {
        fun fromValue(version: String) = HttpVersion.entries.firstOrNull { it.version == version }
    }
}
