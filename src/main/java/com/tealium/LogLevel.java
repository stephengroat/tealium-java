package com.tealium;

public enum LogLevel {
    NONE(3), ERRORS(2), WARNINGS(1), VERBOSE(0);

    private final int priority;

    private LogLevel(int priority) {
        this.priority = priority;
    }

    int getPriority() {
        return priority;
    }
}
