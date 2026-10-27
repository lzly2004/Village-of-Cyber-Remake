package com.vocr.web;

import com.vocr.core.SeededGuestSession;

/** Browser tests only: real HTTP/core with a fixed seed, never part of production web output. */
public final class SeededGuestGameServer
{
    private SeededGuestGameServer() { }

    public static void main(String[] args)
    {
        boolean baseline = args.length == 2 && args[1].equals("--release-baseline");
        int port = GuestGameServer.parsePort(baseline ? new String[] { args[0] } : args);
        // Baseline quiet mode is test-only; GuestGameServer.start also disables private diagnostics in Web.
        // Desktop defaults and buttons remain unchanged.
        if (baseline) com.vocr.logging.DebugLogger.getInstance().setEnabled(false);
        GuestGameServer server = new GuestGameServer(
                baseline ? SeededGuestSession::createReleaseBaseline
                         : id -> SeededGuestSession.create(id, 4202), 100).start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}
