package com.vocr.web;

import com.vocr.core.SeededGuestSession;

/** Browser tests only: real HTTP/core with a fixed seed, never part of production web output. */
public final class SeededGuestGameServer
{
    private SeededGuestGameServer() { }

    public static void main(String[] args)
    {
        GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 100)
                .start(GuestGameServer.parsePort(args));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}
