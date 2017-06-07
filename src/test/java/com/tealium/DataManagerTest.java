package com.tealium;

import com.tealium.DataManager.Key;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class DataManagerTest {
    @Test
    public void testInit() throws Exception {
        new DataManager(TestLibraryContext.newInstance());
    }

    @Test
    public void testRandom() throws Exception {
        DataManager data = new DataManager(TestLibraryContext.newInstance());
        ArrayList<String> randomArray = new ArrayList<String>();
        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("getRandom");
        method.setAccessible(true);
        for (int i = 0; i < 100; i++) {
            String random = (String) method.invoke(data);
            if (!randomArray.contains(random)) {
                randomArray.add(random);
                // check length
                if (!random.matches("^[0-9]{16}$")){
                    fail("Random value is not 16 digits");
                }
            } else {
                fail("Random value has been repeated");
            }
        }
    }

    @Test
    public void testGetPersistentData() throws Exception {

        LibraryContext ctx = TestLibraryContext.newInstance();
        FileUtils.deletePersistentFile(ctx);

        DataManager data = new DataManager(ctx);
        Map<String, Object> map = new HashMap<>();

        String key = "testKey";
        String value = "testValue";

        map.put(key, value);

        // Write to disk first
        data.addPersistentData(map);

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("getPersistentData");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> savedData = (Map<String, Object>) method.invoke(data);

        assertTrue(savedData.containsKey(key));
        assertTrue(savedData.containsValue(value));
    }

    @Test
    public void testCreateNewVisitorId() throws Exception {
        LibraryContext ctx = TestLibraryContext.newInstance();
        FileUtils.deletePersistentFile(ctx);

        DataManager data = new DataManager(ctx);

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("createNewVisitorId");
        method.setAccessible(true);

        String visitorID = (String) method.invoke(data);
        String visitorID2 = (String) method.invoke(data);
        assertFalse(visitorID.equals(visitorID2));

    }

    @Test
    public void testNewPersistentData() throws Exception {

        String account = "account";
        String profile = "profile";
        String env = "env";
        String datasource = "datasource";
        LibraryContext ctx = new LibraryContext(account, profile, env, datasource, new Logger(LogLevel.VERBOSE));
        FileUtils.deletePersistentFile(ctx);

        DataManager data = new DataManager(ctx);

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("createNewPersistentData");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> persistentData = (Map<String, Object>) method.invoke(data);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("tealium_account", account);
        expectedData.put("tealium_profile", profile);
        expectedData.put("tealium_environment", env);

        String notTestingThisHere = (String) persistentData.get(Key.TEALIUM_VISITOR_ID);
        expectedData.put("tealium_visitor_id", notTestingThisHere);
        expectedData.put("tealium_vid", notTestingThisHere);
        expectedData.put("tealium_library_name", "java");
        expectedData.put("tealium_library_version", "1.2.0");

        assertTrue(mapContainsMap(persistentData, expectedData));

    }

    @Test
    public void testResetSessionId() throws InterruptedException {

        LibraryContext ctx = TestLibraryContext.newInstance();

        DataManager data = new DataManager(ctx);
        String sessionId = data.resetSessionId();
        Thread.sleep(100);
        String sessionId2 = data.resetSessionId();

        assertFalse(sessionId.equals(sessionId2));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    boolean mapContainsMap(Map<String, Object> sourceMap, Map<String, Object> subSetMap) {

        Set<String> sourceKeys = sourceMap.keySet();
        Set<String> subSetKeys = subSetMap.keySet();

        for (String k : subSetKeys) {
            if (!sourceKeys.contains(k)) {
                System.out.println("Source map does not contain key from subSetMap: " + k);
                return false;
            }

            Object sourceValue = sourceMap.get(k);
            Object subSetValue = subSetMap.get(k);
            if (!sourceValue.equals(subSetValue)) {
                System.out.println("Source map key-value: " + k + ":" + sourceValue.toString()
                        + " does not equal subSet's: " + k + ":" + subSetValue.toString());
                return false;
            }
        }

        return true;
    }

}