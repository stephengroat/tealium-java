package com.tealium;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tealium.Tealium.DispatchCallback;
import com.tealium.DataManager.InfoKey;

import javax.xml.ws.http.HTTPException;


/**
*   Internal convenience exception for handling failed URL connections.
*/
class FailedConnectionException extends HTTPException {

    public Map<String, List<String>> headers;
    public String customMessage;

    public String getCustomMessage() {
        return customMessage;
    }

    public FailedConnectionException(int responseCode) { super(responseCode); }
    public FailedConnectionException(int responseCode, String message, Map<String, List<String>> headers) {

        super(responseCode);
        this.customMessage = message;
        this.headers = headers;
    }
}

/**
 * Tealium Collect Dispatch service for delivering tracked data to Tealium's
 * Collect Service
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell
 */
final class CollectDispatchService {

    public final static String DEFAULT_URL = "https://collect.tealiumiq.com/vdata/i.gif";

    private final String baseURL;
    private final Logger logger;
    private final int timeout;

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Constructor for creating an instance of the Tealium Collect Dispatch
     * service
     * 
     * @param baseURL
     *            The target url to send track dispatches to. Should NOT include
     *            any ending query strings or "?" suffix.
     */
    public CollectDispatchService(String baseURL, LibraryContext context, int timeout) {
        super();
        this.baseURL = baseURL;
        this.logger = context.getLogger();
        this.timeout = timeout;
    }

    /**
     * Packages data sources into expected URL call format and sends.
     * 
     * @param data
     *            Map of all key-values to be sent with dispatch.
     * @param callback
     *            Optional callback object implementing the CollectCallback
     *            interface.
     * @throws IOException
     * @see{@link #CollectCallback}
     */
    public void dispatch(Map<String, Object> data, DispatchCallback callback) throws IOException {

        String finalUrl = this.baseURL + encodedQueryStringParamsFrom(data);

        try {
            HttpURLConnection connection = collectConnection(this, finalUrl);

            Map<String, List<String>> responseHeaders = send(connection);
            safeCallback(callback,
                    true,
                    finalUrl,
                    responseHeaders,
                    data,
                    null);
        } catch (FailedConnectionException e) {
            String errorMessage = e.toString();
            Map<String, List<String>> headers = e.headers;
            safeCallback(callback,
                    false,
                    finalUrl,
                    headers,
                    data,
                    errorMessage);
        } catch (UnknownHostException e) {
            String errorMessage = e.toString();
            safeCallback(callback,
                    false,
                    finalUrl,
                    null,
                    data,
                    errorMessage);
        } catch (MalformedURLException e) {
            safeCallback(callback,
                    false,
                    finalUrl,
                    null,
                    data,
                    e.toString());
            new RuntimeException(e);
        }

    }

    /**
     * Base URL string target for dispatches.
     * 
     * @return String
     */
    public String getBaseURL() {
        return baseURL;
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private String encodedQueryStringParamsFrom(Map<String, ?> data) throws UnsupportedEncodingException {

        // No query string needed
        if (data == null) {
            return "";
        }

        // Param suffix to base URL
        String queryString = "?";

        // Iterate through each entry and convert to query string params
        for (Map.Entry<String, ?> entry : data.entrySet()) {

            final String key = entry.getKey();
            final Object value = entry.getValue();

            // Filter out any grossly invalid key-value pairs
            if (Util.isEmpty(key) || value == null) {
                continue;
            }

            if (queryString.length() > 1) {
                // if more than "?", append &
                queryString += "&";
            }

            queryString += URLEncoder.encode(key.toString(), StandardCharsets.UTF_8.name()) + "=";

            // Attempt to coerce possible array to String[] collection
            String[] arrayValue;
            if ((arrayValue = Util.coerceIntoStringArray(value)) != null) {
                queryString += encode(arrayValue);
            } else {
                // TODO: Generate warning if unexpected value
                queryString += URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name());
            }
        }

        return queryString;
    }

    /**
     * Encode String[] according to vData spec.
     *
     * @param value of String[]
     * @return encoded String
     * @throws UnsupportedEncodingException
     */
    private String encode(String[] value) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < value.length; i++) {
            // Ensures that arrays will be JSON encoded, and performs the
            // necessary escape
            builder.append('"').append(value[i].replace("\"", "\\\"")).append('"');
            if (i < (value.length - 1)) {
                builder.append(',');
            }
        }
        return URLEncoder.encode(builder.append(']').toString(), StandardCharsets.UTF_8.name());
    }

    // =========================================================================
    // PROTECTED HELPERS
    // =========================================================================

    protected Map<String, List<String>> send(HttpURLConnection connection) throws FailedConnectionException, IOException {

        try {

            connection.connect();
            int responseCode = connection.getResponseCode();
            String responseError = connection.getHeaderField("x-error");
            Map<String, List<String>> headers = connection.getHeaderFields();

            if (responseError != null ){
                throw new FailedConnectionException(responseCode, responseError, headers);
            }

            if (responseCode != 200) {
                responseError = "Unexpected response code received: " + Integer.toString(responseCode);
                throw new FailedConnectionException(responseCode, responseError, headers);
            }

            return (headers);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void safeCallback(Tealium.DispatchCallback callback,
                              boolean success,
                              String encodedUrl,
                              Map<String, List<String>> headerFields,
                              Map<String, Object> data,
                              String errorMessage) {

        if (callback == null) {
            return;
        }

        // General
        Map<String, Object> info = new HashMap<>();
        info.put(InfoKey.DISPATCH_SERVICE, "collect");

        if (encodedUrl != null) {
            info.put(InfoKey.ENCODED_URL, encodedUrl);
        }

        if (data != null) {
            info.put(InfoKey.PAYLOAD, data);
        }

        // Headers
        if (headerFields != null) {
            Map<String, Object> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> k : headerFields.entrySet()) {
                for (String v : k.getValue()) {
                    headers.put(k.getKey(), v);
                }
            }
            info.put(DataManager.InfoKey.RESPONSE_HEADERS, headers);
        }

        callback.dispatchComplete(success, info, errorMessage);

    }

    private static HttpURLConnection collectConnection(CollectDispatchService collect, String encodedURLString) throws IOException {

        URL url = new URL(encodedURLString);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");
        con.setReadTimeout(collect.timeout);

        return con;
    }

    // Using logger
//    private void printResponse(HttpURLConnection connection) throws IOException {
//
//        if (!this.logger.isLogging(Logger.Level.VERBOSE)) {
//            return;
//        }
//
//        int responseCode = connection.getResponseCode();
//        String urlString = connection.getURL().toString();
//        this.logger.log("\nSending 'GET' request to URL : " + urlString, Logger.Level.VERBOSE);
//        this.logger.log("Response Code : " + responseCode, Logger.Level.VERBOSE);
//
//        Map<String, List<String>> headers = connection.getHeaderFields();
//        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
//            this.logger.log(entry.getKey() + " : " + entry.getValue(), Logger.Level.VERBOSE);
//        }
//
//    }
    
    // =========================================================================
    // TESTING
    // =========================================================================
    
    static class Test {
        
        static String encodedQueryStrings(Map<String,?> map, CollectDispatchService instance) throws UnsupportedEncodingException{
            
            return instance.encodedQueryStringParamsFrom(map);
            
        }
        
    }

}
