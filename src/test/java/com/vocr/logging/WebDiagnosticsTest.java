package com.vocr.logging;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class WebDiagnosticsTest
{
    @Test void operationalFailureRemainsVisibleWhilePrivateDebugIsDisabled() {
        var bytes=new ByteArrayOutputStream();PrintStream previous=System.err;
        boolean enabled=DebugLogger.getInstance().isEnabled();
        try(var capture=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setErr(capture);DebugLogger.getInstance().setEnabled(false);
            DebugLogger.error("PRIVATE_DEBUG_PROBE");
            WebDiagnostics.write(WebDiagnostics.Event.REQUEST_FAILED);
        } finally {System.setErr(previous);DebugLogger.getInstance().setEnabled(enabled);}
        assertEquals("[HTTP] REQUEST_FAILED"+System.lineSeparator(),bytes.toString(StandardCharsets.UTF_8));
    }

    @Test void publicInputSurfaceAcceptsOnlyFixedEventsAndValidatedNumericPort() {
        // Intentional security boundary: adding a String/Throwable payload requires a new review.
        var methods=Arrays.stream(WebDiagnostics.class.getDeclaredMethods()).filter(m->Modifier.isPublic(m.getModifiers())).toList();
        assertEquals(2,methods.size());
        assertTrue(methods.stream().allMatch(m->Arrays.equals(m.getParameterTypes(),new Class<?>[]{WebDiagnostics.Event.class})
                || Arrays.equals(m.getParameterTypes(),new Class<?>[]{int.class})));
        for(int port:new int[]{-1,0,65536,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->WebDiagnostics.listening(port));
        assertThrows(NullPointerException.class,()->WebDiagnostics.write(null));
    }

    @Test void listeningOutputContainsOnlyFixedLoopbackRouteAndValidatedPort() {
        var bytes=new ByteArrayOutputStream();PrintStream previous=System.out;
        try(var capture=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(capture);WebDiagnostics.listening(7070);
        } finally {System.setOut(previous);}
        assertEquals("[HTTP] 游客 API: http://127.0.0.1:7070/api/v1/games"+System.lineSeparator(),bytes.toString(StandardCharsets.UTF_8));
    }
}
