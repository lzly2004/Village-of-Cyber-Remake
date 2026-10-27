package com.vocr.logging;

import java.util.Objects;

/** Standalone Web operational diagnostics: fixed events, no request data or Throwable input. */
public final class WebDiagnostics
{
    private WebDiagnostics() { }

    public enum Event
    {
        REQUEST_FAILED("[HTTP] REQUEST_FAILED"),
        SERIALIZATION_FAILED("[HTTP] SERIALIZATION_FAILED"),
        REAP_FAILED("[HTTP] REAP_FAILED"),
        START_CLEANUP_FAILED("[HTTP] START_CLEANUP_FAILED"),
        INVALID_PORT("端口参数无效；用法：runWeb --args=\"7070\"（可省略端口）"),
        START_FAILED("无法启动游客服务，请检查端口是否被占用及监听权限");

        private final String message;
        Event(String message) { this.message = message; }
    }

    public static void write(Event event) {
        System.err.println(Objects.requireNonNull(event).message);
    }

    public static void listening(int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("端口必须在1—65535之间");
        System.out.println("[HTTP] 游客 API: http://127.0.0.1:" + port + "/api/v1/games");
    }
}
