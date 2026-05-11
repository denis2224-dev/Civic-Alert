package com.civicalert.util;

public final class TrackingCodeGenerator {

    private TrackingCodeGenerator() {
    }

    public static String generate(int year, long sequence) {
        return String.format("CA-%d-%05d", year, sequence);
    }
}

