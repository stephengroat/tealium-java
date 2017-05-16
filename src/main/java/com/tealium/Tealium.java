package com.tealium;

import java.io.IOException;
import java.util.Map;

import com.tealium.DataManager.Key;
import com.tealium.DataManager.EventType;
import com.tealium.DataManager.InfoKey;

/**
 * Tealium library for conversion and dispatch handling of natively triggered
 * events.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell
 */
public final class Tealium {

    // Public API Fields should never be public; see Effective Java Item 14
    private final DataManager data;
    private final CollectDispatchService collect;
    private final LibraryContext libraryContext;

    // =========================================================================
    // PUBLIC BUILDER
    // =========================================================================

    public static class Builder {

        private final String account;
        private final String profile;
        private String environment;
        private String datasource;
        private LogLevel logLevel = LogLevel.VERBOSE;
        private int timeout = 5000;

        /**
         * Constructor for a new Tealium object.
         * 
         * @param account
         *            Required. Tealium account name.
         * @param profile
         *            Required. Tealium profile name.
         */
        public Builder(String account, String profile) {
            if (Util.isEmpty(this.account = account) || Util.isEmpty(this.profile = profile)) {
                throw new IllegalArgumentException("account & profile must not be empty.");
            }
        }
        
        /**
         * Constructor for a new Tealium object.
         * 
         * The preferred way to set environment is with the "setEnvironment" method.
         * Environment is therefore no longer required as a constructor argument, and this
         * constructor is deprecated.
         * 
         * @deprecated use {@link #Builder(String, String)} ()} instead.
         * 
         * @param account
         *            Required. Tealium account name.
         * @param profile
         *            Required. Tealium profile name.
         * @param environment
         *            Required. Tealium environment. Usually dev, qa, or prod.
         */
        @Deprecated
        public Builder(String account, String profile, String environment) {
            if (Util.isEmpty(this.account = account) || Util.isEmpty(this.profile = profile)
                    || Util.isEmpty(this.environment = environment)) {
                throw new IllegalArgumentException("account, profile, & environment must not be empty.");
            }
        }

        /**
         * Executes actual build.
         * 
         * @return Instance of Tealium.
         */
        public Tealium build() {
            return new Tealium(
                    new LibraryContext(this.account, this.profile, this.environment, this.datasource, new Logger(this.logLevel)),
                    this.timeout);
        }

        public Builder setLogLevel(LogLevel level) {
            if (level == null) {
                throw new IllegalArgumentException("Invalid log level.");
            }
            this.logLevel = level;
            return this;
        }
        
        public Builder setEnvironment(String environment) {
        	if (environment == null) {
                throw new IllegalArgumentException("Invalid environment.");
            }
            this.environment = environment;
            return this;
        }
        
        public Builder setDatasource(String datasource) {
        	if (datasource == null) {
                throw new IllegalArgumentException("Invalid datasource.");
            }
            this.datasource = datasource;
            return this;
        }

        public Builder setTimeout(int timeout) {
            if (timeout == 0) {
                throw new IllegalArgumentException("Timeout should be greater than zero.");
            }
            this.timeout = timeout;
            return this;
        }

    }

    // =========================================================================
    // PUBLIC CALLBACK INTERFACE
    // =========================================================================

    /**
     * Optional interface for a dispatch call-back object that reports when a
     * track event has completed.
     */
    public interface DispatchCallback {
        /**
         * Required method to handle callback.
         * 
         * @param success
         * @param info
         *            Map containing call info, typical keys: dispatch_service, payload
         * @param error
         *            null or string whether an occurred.
         * 
         * @return String encoded URL string
         */
        public void dispatchComplete(boolean success, Map<String, Object> info, String error);
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    public String getAccount() {
        return this.libraryContext.getAccount();
    }

    public DataManager getDataManager() {
        return this.data;
    }

    public String getProfile() {
        return this.libraryContext.getProfile();
    }

    public String getEnvironment() {
        return this.libraryContext.getEnvironment();
    }
    
    public String getDatasource() {
    	return this.libraryContext.getDatasource();
    }

    /**
     * Convenient basic track event.
     * 
     * @param eventTitle
     *            Required title of event.
     */
    public void track(String eventTitle) {
        track(eventTitle, null, null);
    }

    /**
     * Convenient tracking event with optional data.
     * 
     * @param eventTitle
     *            Required title of event.
     * @param data
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     */
    public void track(String eventTitle, Map<String, ?> data) {
        track(eventTitle, data, null);
    }

    /**
     * Convenient Track method for activities.
     * 
     * @param eventTitle
     *            Required title of event.
     * @param data
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventTitle, Map<String, ?> data, Tealium.DispatchCallback callback) {

        track(EventType.ACTIVITY, eventTitle, data, callback);
    }

        /**
     * Primary Track method.
     * 
     * @param eventType
     *            Optional track type (VIEW, ACTIVITY, INTERACTION, DERIVED, CONVERSION). Defaults
     *            to ACTIVITY if nil.
     * @param eventTitle
     *            Required title of event.
     * @param data
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventType, String eventTitle, Map<String, ?> data, Tealium.DispatchCallback callback) {

        Map<String, Object> contextData = this.data.getPersistentData();

        if (eventType == null) {
            eventType = EventType.ACTIVITY;
        }
        contextData.put(Key.TEALIUM_EVENT_TYPE, eventType);

        if (eventTitle != null) {
            //Legacy - will be deprecated
            contextData.put(Key.EVENT_NAME, eventTitle);
            contextData.put(Key.TEALIUM_EVENT, eventTitle);
        }
        
        if (this.getDatasource() != null) {
        	contextData.put(Key.TEALIUM_DATASOURCE, this.getDatasource());
        }
        
        contextData.putAll(this.data.getVolatileData());
        if (data != null) {
            contextData.putAll(Util.copySanitized(data));
        }

        try {
            this.collect.dispatch(contextData, callback);
        } catch (IOException e) {
            this.libraryContext.getLogger().log(e, LogLevel.ERRORS);
        }
    }

    /**
     * Convenient basic track method for failed dispatches. Method used as a
     * re-try
     *
     * @deprecated use {@link #track(String, Map, DispatchCallback)} ()} instead.
     *
     * @param encodedUrl
     *            Encoded URL string returned from failed dispatch callback
     */
    @Deprecated
    public void track(String encodedUrl, Tealium.DispatchCallback callback) throws IOException {
        // No longer supported
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private Tealium(LibraryContext libraryContext, int timeout) {
        super();
        this.libraryContext = libraryContext;
        this.data = new DataManager(this.libraryContext);
        // Is the URL in the constructor future proofing?
        this.collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL, this.libraryContext, timeout);
    }

}
