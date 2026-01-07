package org.burufi.pdp.http

enum class HttpMethod {

    /**
     * Transfer a current representation of the target resource.
     */
    GET,

    /**
     * Transfer a current representation of the target resource
     * (same as [GET]), but only transfer the status line and header section.
     */
    HEAD,

    /**
     * Perform resource-specific processing on the request payload.
     */
    POST,

    /**
     * Replace all current representations of the target resource with the
     * request payload.
     */
    PUT,

    /**
     * Remove all current representations of the target resource.
     */
    DELETE,

    /**
     * Establish a tunnel to the server identified by the target resource.
     */
    CONNECT,

    /**
     * Describe the communication options for the target resource.
     */
    OPTIONS,

    /**
     * Perform a message loop-back test along the path to the target resource.
     */
    TRACE;

    companion object {
        fun fromValue(method: String) = HttpMethod.entries.firstOrNull { it.name == method }
    }
}
