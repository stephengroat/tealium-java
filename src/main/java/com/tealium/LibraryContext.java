package com.tealium;

class LibraryContext {

    private final String account;
    private final String profile;
    private final String environment;
    private final String datasource;
    private final Logger logger;

    LibraryContext(String account, String profile, String environment, String datasource, Logger logger) {
        super();
        this.account = account;
        this.profile = profile;
        this.environment = environment;
        this.datasource = datasource;
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
    
    final String getDatasource() {
    	return datasource;
    }

    final Logger getLogger() {
        return logger;
    }

}
