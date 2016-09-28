package com.tealium;

import java.io.IOException;
import java.util.Map;

import com.tealium.DataManager.Key;

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
        private final String environment;
        private LogLevel logLevel = LogLevel.VERBOSE;
        private int timeout = 5000;

        /**
         * Constructor for a new Tealium object.
         * 
         * @param account
         *            Required. Tealium account name.
         * @param profile
         *            Required. Tealium profile name.
         * @param environment
         *            Required. Tealium environment. Usually dev, qa, or prod.
         */
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
                    new LibraryContext(this.account, this.profile, this.environment, new Logger(this.logLevel)),
                    this.timeout);
        }

        public Builder setLogLevel(LogLevel level) {
            if (level == null) {
                throw new IllegalArgumentException("Invalid log level.");
            }
            this.logLevel = level;
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
         * @param encodedUrl
         * @param error
         *            null or string whether an occurred.
         * 
         * @return String encoded URL string
         */
        public void dispatchComplete(boolean success, String encodedUrl, String error);
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
     * Primary Track method.
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

        Map<String, Object> contextData = this.data.getPersistentData();
        if (eventTitle != null) {
            //Legacy - will be deprecated
            contextData.put(Key.EVENT_NAME, eventTitle);
            contextData.put(Key.TEALIUM_EVENT, eventTitle);
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
     * @param encodedUrl
     *            Encoded URL string returned from failed dispatch callback
     */
    public void track(String encodedUrl, Tealium.DispatchCallback callback) throws IOException {
        collect.send(encodedUrl, callback);
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
