package com.tealium;

import java.io.PrintStream;

// Java convention for loggers is to expose methods based off of level, so logger.error(...), logger.warn(...), logger.debug(...)
/**
 * Tealium logger for debugging.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell
 */
final class Logger {

    static enum Level {
        NONE(3), ERRORS(2), WARNINGS(1), VERBOSE(0);

        private final int priority;

        private Level(int priority) {
            this.priority = priority;
        }

        int getPriority() {
            return priority;
        }
    };

    private final Level level;
    private final PrintStream out;

    public Level getLevel() {
        return level;
    }

    public Logger(Level level) {
        super();
        this.level = level;
        out = System.out;
    }

    public boolean isLogging(Level level) {
        return level.getPriority() >= this.level.getPriority();
    }

    /**
     * For printing at console logs related to just the Tealium object.
     * 
     * @param message
     *            String description.
     * @param level
     *            Level of message.
     */
    public void log(String message, Level level) {
        if (isLogging(level)) {
            out.println(message);
        }
    };

    public void log(Throwable t, Level level) {
        if (isLogging(level)) {
            out.println(t);
            t.printStackTrace(out);
        }
    };
}
