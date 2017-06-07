package com.tealium;

final class TestLibraryContext extends LibraryContext {
    private TestLibraryContext() {
        super("tealiummobile", "demo", "dev", "datasource", new Logger(LogLevel.VERBOSE));
    }

    static TestLibraryContext newInstance() {
        return new TestLibraryContext();
    }

}
