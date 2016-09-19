package com.tealium;

class LibraryContext {

    private final String account;
    private final String profile;
    private final String environment;
    private final Logger logger;

    LibraryContext(String account, String profile, String environment, Logger logger) {
        super();
        this.account = account;
        this.profile = profile;
        this.environment = environment;
        this.logger = logger;
    }

    final String getAccount() {
        return account;
    }

    final String getProfile() {
        return profile;
    }

    final String getEnvironment() {
        return environment;
    }

    final Logger getLogger() {
        return logger;
    }

}
