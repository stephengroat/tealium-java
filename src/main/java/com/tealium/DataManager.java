package com.tealium;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Tealium data manager object for processing generation of standardized
 * universal data points.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell
 */
public final class DataManager {

    private static final int DEFAULT_BASELINE_SIZE = 5;
    private static final long RANDOM_MAX_PLUS_ONE = 10000000000000000L;

    private final LibraryContext context;
    private final Random randomGenerator;
    private final Map<String, Object> volatileData;
    private String sessionId;
    private Map<String, Object> persistentCache;

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Constructor for new DataManager Object
     */
    public DataManager(LibraryContext libraryContext) {
        super();
        this.context = libraryContext;
        this.sessionId = getTimestampInMilliseconds();
        this.randomGenerator = new SecureRandom();
        this.volatileData = new HashMap<String, Object>(DEFAULT_BASELINE_SIZE);

        try {
            final File file = FileUtils.getPersistentFile(this.context);
            if (file.exists()) {
                this.persistentCache = FileUtils.readMap(file);
            } else {
                this.persistentCache = createNewPersistentData();
                FileUtils.writeMap(file, this.persistentCache);
            }
        } catch (IOException e) {
            // File must be corrupt/unreadable etc
            this.persistentCache = new HashMap<>();
            this.context.getLogger().log(e, LogLevel.ERRORS);
        }
    }

    /**
     * Retrieve Session ID
     * 
     * @return sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Retrieve persistent data map
     * 
     * @return persistent data map
     */
    public Map<String, Object> getPersistentData() {
        return this.persistentCache;

    }

    /**
     * Convenience to add to persistent data map
     * 
     * @param string
     *            array
     * @throws IOException
     */
    public void addPersistentData(Map<String, Object> data) throws IOException {
        Map<String, Object> persistent = getPersistentData();
        persistent.putAll(data);
        FileUtils.writeMap(FileUtils.getPersistentFile(this.context), persistent);
    }

    /**
     * Convenience to reset session ID
     * 
     */
    public String resetSessionId() {
        String sessionId = getTimestampInMilliseconds();
        this.sessionId = sessionId;
        return sessionId;

    }

    // =========================================================================
    // PROTECTED
    // =========================================================================

    protected Map<String, Object> getVolatileData() {
        volatileData.put(Key.TEALIUM_TIMESTAMP_EPOCH, getTimestampInSeconds());
        volatileData.put(Key.TEALIUM_RANDOM, getRandom());
        volatileData.put(Key.TEALIUM_SESSION_ID, getSessionId());
        return volatileData;
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private Map<String, Object> createNewPersistentData() {
        Map<String, Object> data = new HashMap<>();
        data.put(Key.TEALIUM_LIBRARY_NAME, "java");
        data.put(Key.TEALIUM_LIBRARY_VERSION, "1.1.0");
        data.put(Key.TEALIUM_ACCOUNT, this.context.getAccount());
        data.put(Key.TEALIUM_PROFILE, this.context.getProfile());
        data.put(Key.TEALIUM_ENVIRONMENT, this.context.getEnvironment());
        String vid = createNewVisitorId();
        // NOTE Migratory vids
        data.put(Key.TEALIUM_VISITOR_ID, vid);
        data.put("tealium_vid", vid);
        return data;
    }

    private static String getTimestampInSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private static String getTimestampInMilliseconds() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String createNewVisitorId() {
        final String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid;
    }

    private String getRandom() {
        long randomNumber = randomGenerator.nextLong() % RANDOM_MAX_PLUS_ONE;
        String stringRandom = String.format("%016d", Math.abs(randomNumber));
        return stringRandom;
    }

    public static class Key {
        private Key() {
        }

        public static final String EVENT_NAME = "event_name";
        public static final String TEALIUM_ACCOUNT = "tealium_account";
        public static final String TEALIUM_ENVIRONMENT = "tealium_environment";
        public static final String TEALIUM_EVENT = "tealium_event";
        public static final String TEALIUM_EVENT_TYPE = "tealium_event_type";
        public static final String TEALIUM_LIBRARY_NAME = "tealium_library_name";
        public static final String TEALIUM_LIBRARY_VERSION = "tealium_library_version";
        public static final String TEALIUM_PROFILE = "tealium_profile";
        public static final String TEALIUM_RANDOM = "tealium_random";
        public static final String TEALIUM_SESSION_ID = "tealium_session_id";
        public static final String TEALIUM_TIMESTAMP_EPOCH = "tealium_timestamp_epoch";
        public static final String TEALIUM_VISITOR_ID = "tealium_visitor_id";

    }

    public static class EventType {
        private EventType() {
        }

        public static final String ACTIVITY = "activity";
        public static final String CONVERSION = "conversion";
        public static final String DERIVED = "derived";
        public static final String INTERACTION = "interaction";
        public static final String VIEW = "view";

    }

    public static class InfoKey {
        private InfoKey() {
        }
        public static final String DISPATCH_SERVICE = "dispatch_service";
        public static final String ENCODED_URL = "encoded_url";
        public static final String RESPONSE_HEADERS = "response_headers";
        public static final String PAYLOAD = "payload";

    }
}
