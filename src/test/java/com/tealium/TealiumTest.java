package com.tealium;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.tealium.Tealium.DispatchCallback;

public class TealiumTest {

    @Test
    public void testTealiumInit() throws Exception {
        new Tealium.Builder("tealiummobile", "main", "dev").build();
    }

    @Test
    public void testTealiumInitWithLogLevel() throws Exception {
        new Tealium.Builder("tealiummobile", "main", "dev")
                .setLogLevel(LogLevel.WARNINGS)
                .build();

    }

    @Test
    public void name() throws Exception {

    }

    @Test
    public void testTrack() throws InterruptedException {

        FileUtils.deletePersistentFile(TestLibraryContext.newInstance());

        Tealium tealium = new Tealium.Builder("tealiummobile", "demo")
        		.setEnvironment("env").setDatasource("datasource").build();

        final CountDownLatch barrier = new CountDownLatch(3);

        // Keys and values should have been checked by other class tests - just double checking keys here
        Map<String, Object> expectedKeys = new HashMap<String, Object>();
        expectedKeys.put("event_name", "value");
        expectedKeys.put("tealium_account", "value");
        expectedKeys.put("tealium_environment", "value");
        expectedKeys.put("tealium_datasource", "value");
        expectedKeys.put("tealium_event", "value");
        expectedKeys.put("tealium_event_type", "value");
        expectedKeys.put("tealium_library_name", "value");
        expectedKeys.put("tealium_library_version", "value");
        expectedKeys.put("tealium_profile", "value");
        expectedKeys.put("tealium_random", "value");
        expectedKeys.put("tealium_session_id", "value");
        expectedKeys.put("tealium_timestamp_epoch", "value");
        expectedKeys.put("tealium_visitor_id", "value");
        
        final Map<String, Object> expectedKeysImmutable = new HashMap<String, Object>(expectedKeys);

        DispatchCallback callBack = new DispatchCallback() {
            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                Map<String, Object> payload = (HashMap<String, Object>)info.get("payload");

                assertTrue(success);
                assertTrue(mapContainsKeysFromMap(payload, expectedKeysImmutable));

                barrier.countDown();
            }
        };

        tealium.track("test", null, callBack);
        barrier.await(1, TimeUnit.SECONDS);

    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    boolean mapContainsKeysFromMap(Map<String, Object> sourceMap, Map<String, Object> subSetMap) {

        Set<String> sourceKeys = sourceMap.keySet();
        Set<String> subSetKeys = subSetMap.keySet();

        for (String k : subSetKeys) {
            if (!sourceKeys.contains(k)) {
                System.out.println("Source map does not contain key from subSetMap: " + k);
                return false;
            }

        }

        return true;
    }

}
