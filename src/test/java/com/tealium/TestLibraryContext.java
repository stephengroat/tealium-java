package com.tealium;

final class TestLibraryContext extends LibraryContext {
    private TestLibraryContext() {
        super("tealiummobile", "demo", "dev", new Logger(Logger.Level.VERBOSE));
    }

    static TestLibraryContext newInstance() {
        return new TestLibraryContext();
    }

}
