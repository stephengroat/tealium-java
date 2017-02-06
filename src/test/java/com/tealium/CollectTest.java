package com.tealium;

import static org.junit.Assert.*;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import com.tealium.Tealium.DispatchCallback;

public class CollectTest {

    // SIMPLE START UP TEST
    @Test
    public void testInit() throws Exception {
        new CollectDispatchService(CollectDispatchService.DEFAULT_URL, TestLibraryContext.newInstance(), 3000);
    }

    
    // INTEGRATION DISPATCH TESTS
    @Test
    public void testDispatch() throws Exception {

        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = new DispatchCallback() {

            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                assertTrue(success);
                barrier.countDown();
            }

        };

        Map<String, Object> data = new HashMap<String, Object>(5);
        data.put("autotracked", "false");
        data.put("tealium_account", "tealiummobile");
        data.put("tealium_profile", "demo");
        data.put("tealium_visitor_id", "612040730c8c11e6b4cbtest329f41b7");
        data.put("tealium_vid", "612040730c8c11e6b4cbtest329f41b7");
        collect.dispatch(data, callBack);

        barrier.await(5, TimeUnit.SECONDS);

    }

    @Test
    public void testFailingDispatch() throws Exception {

        CollectDispatchService collect = new CollectDispatchService("http://url.2.nowhere", TestLibraryContext
                .newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = new DispatchCallback() {

            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                assertFalse(success);
                barrier.countDown();
            }

        };

        Map<String, Object> data = new HashMap<String, Object>(5);
        data.put("autotracked", "false");
        data.put("tealium_account", "tealiummobile");
        data.put("tealium_profile", "demo");
        data.put("tealium_visitor_id", "612040730c8c11e6b4cbtest329f41b7");
        data.put("tealium_vid", "612040730c8c11e6b4cbtest329f41b7");
        collect.dispatch(data, callBack);

        barrier.await(5, TimeUnit.SECONDS);

    }

    
    // ENCODE QUERY STRING PARAMS TESTS

    static String EXPECTED_URL_STRINGSTRINGMAP = "?a+key=a+value&key=value&key1=value1&key_2=value_2&specialKey=%24-_.%2B%21*%27%28%29%2C";
    static String EXPECTED_URL_STRINGARRAYMAP = "?array2=%5B%22123%22%2C%22true%22%2C%22%21%40%23%24%25%5E%26*%28%29_%2B%22%5D&array1=%5B%22foo%22%2C%22bar%2C%0D%0A%09and+some+extra+stuff%22%5D";
    static String EXPECTED_URL_STRINGMIXEDMAP = "?a+key=a+value&array1=%5B%22foo%22%2C%22bar%2C%0D%0A%09and+some+extra+stuff%22%5D&key_2=value_2";

    Map<String, Object> stringStringMap() {
        
        Map<String, Object> map = new HashMap<>();
        
        map.put("key", "value");
        map.put("key1", "value1");
        map.put("key_2", "value_2");
        map.put("a key", "a value");
        map.put("specialKey", "$-_.+!*'(),");
        
        return map;
        
    }
    
    Map<String, ?> stringArrayMap() {
        
        Map<String, Object> map = new HashMap<>();

        map.put("array1", new String[] { "foo", "bar,\r\n\tand some extra stuff" });
        map.put("array2", new String[] { "123", "true", "!@#$%^&*()_+"});

        return map;
        
    }
    
    Map<String, Object> stringMixedAcceptableMap() {
        
        Map<String, Object> map = new HashMap<>();
        
        map.put("key_2", "value_2");
        map.put("a key", "a value");
        map.put("array1", new String[] { "foo", "bar,\r\n\tand some extra stuff" });
        
        return map;
        
    }
    
    Map<String, ?> stringMixedUnacceptableMap() {
        
        Map<String, Object> map = new HashMap<>();
        
        // TODO
        
        return map;
        
    }
    
    @Test
    public void testEncodeQueryStringParamsStringMap() throws Exception {
        
        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);
        
        // String, String maps
        Map<String, Object> map = stringStringMap();
        Map<String, Object> orderedMap = new TreeMap<String, Object>(map);
        String output = CollectDispatchService.Test.encodedQueryStrings(orderedMap, collect);
        assertEquals(EXPECTED_URL_STRINGSTRINGMAP, output);
     
    }
    
    @Test
    public void testEncodeQueryStringParamsStringArrayMap() throws Exception {
        
        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);
        
        // String, [String] maps
        Map<String, ?> map = stringArrayMap();
        String output = CollectDispatchService.Test.encodedQueryStrings(map, collect);

        assertEquals(EXPECTED_URL_STRINGARRAYMAP, output);
     
    }
    
    @Test
    public void testEncodeQueryStringParamsMixedMap() throws Exception {
        
        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);
        
        // String, String & [String] maps
        Map<String, Object> map = stringMixedAcceptableMap();
        Map<String, Object> orderedMap = new TreeMap<String, Object>(map);

        String output = CollectDispatchService.Test.encodedQueryStrings(orderedMap, collect);

        assertEquals(EXPECTED_URL_STRINGMIXEDMAP, output);
        
        
    }
    
    @Test
    public void testSuccessfulSend() throws Exception {
        
        // Mock(s)
        HttpURLConnection testConnection = Mockito.mock(HttpURLConnection.class);
        when(testConnection.getResponseCode()).thenReturn(200);
        when(testConnection.getHeaderField("x-error")).thenReturn(null);
        
        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);

        try {
            Map<String, List<String>> headers = collect.send(testConnection);
            assertTrue(headers != null);
        } catch (Exception e){
            System.out.println("testSuccessfulSend: " + e.toString());
            fail();
        }

    }
    
    @Test
    public void testFailingSendXError() throws Exception {
        
        // Mock(s)
        String errorMessage = "some error detected";
        HttpURLConnection testConnection = Mockito.mock(HttpURLConnection.class);
        when(testConnection.getHeaderField("x-error")).thenReturn(errorMessage);
        
        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);

        try {
            collect.send(testConnection);
            fail();
        } catch (FailedConnectionException e){
            assertTrue(e.getCustomMessage().equals(errorMessage));
        } catch (Exception e) {
            System.out.println("testFailingSendXError" + e.toString());
            fail();
        }

    }
    
    @Test
    public void testFailingResponseCode() throws Exception {
        
        // Mock(s)
        HttpURLConnection testConnection = Mockito.mock(HttpURLConnection.class);
        when(testConnection.getResponseCode()).thenReturn(123);        

        String expectedErrorMessage = "Unexpected response code received: 123";

        CollectDispatchService collect = new CollectDispatchService(CollectDispatchService.DEFAULT_URL,
                TestLibraryContext.newInstance(), 1000);

        try {
            collect.send(testConnection);
            fail();
        } catch (FailedConnectionException e){
            assertTrue(e.getStatusCode() == 123);
            assertTrue(e.getCustomMessage().equals(expectedErrorMessage));
        } catch (Exception e) {
            System.out.println("testFailingResponseCode" + e.toString());
            fail();
        }

    }
    
    @Test
    public void testMock() throws Exception {
    
        HttpURLConnection testConnection = Mockito.mock(HttpURLConnection.class);
        when(testConnection.getResponseCode()).thenReturn(111);
        
        int responseCode = testConnection.getResponseCode();
        assertTrue(responseCode == 111);
        
        verify(testConnection).getResponseCode();
    }
    
    
}
