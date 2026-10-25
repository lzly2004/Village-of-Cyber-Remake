package com.vocr.domain;

public record IntPair(int first, int second)
{
    @Override
    public String toString() { return "(" + first + ", " + second + ")"; }
}
