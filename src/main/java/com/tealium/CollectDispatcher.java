package com.tealium;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tealium.Tealium.DispatchCallback;
import com.tealium.DataManager.InfoKey;


/**
 * Tealium Collect Dispatch service for delivering tracked data to Tealium's
 * Collect Service
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
final class CollectDispatcher {

    public final static String DEFAULT_URL = "https://collect.tealiumiq.com/event";

    private final String endpoint;
    private final Logger logger;
    private final int timeout;

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Constructor for creating an instance of the Tealium Collect Dispatch
     * service
     * 
     * @param endpoint
     *            The target url to send track dispatches to. Should NOT include
     *            any ending query strings or "?" suffix.
     */
    public CollectDispatcher(String endpoint, LibraryContext context, int timeout) {
        super();
        this.endpoint = endpoint;
        this.logger = context.getLogger();
        this.timeout = timeout;
    }

    /**
     * Packages data sources into expected URL call format and sends.
     * 
     * @param udo
     *            Map of all key-values to be sent with dispatch.
     * @param callback
     *            Optional callback object implementing the CollectCallback
     *            interface.
     * @throws CollectDispatchException
     * @see{@link #CollectCallback}
     */
    public void dispatch(Udo udo, DispatchCallback callback) throws CollectDispatchException {
        String payloadJson;
        HttpURLConnection connection;

        // Determine request url
        try {
            payloadJson = udo.toJson();
        } catch (UdoSerializationException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because of udo serialization error", e);

            callCallback(callback,
                    false,
                    null,
                    null,
                    udo,
                    err.toString());
            throw err;
        }

        // create connection
        try {
            URL url = new URL(this.endpoint);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setReadTimeout(this.timeout);

        } catch (MalformedURLException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection url is malformed", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    udo,
                    err.toString());
            throw err;
        } catch (ProtocolException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection does not support specified http protocol", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    udo,
                    err.toString());
            throw err;
        } catch (IOException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection could not be created", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    udo,
                    err.toString());
            throw err;
        }

        // Send data and get response
        try {
            Map<String, List<String>> headers;

            int responseCode;

            try {
                // send the data
                connection.connect();
                OutputStream os = connection.getOutputStream();
                os.write(payloadJson.getBytes(StandardCharsets.UTF_8));
                os.close();
            } catch(IOException e) {
                throw new FailedConnectionException("Could not open connection with server.", e);
            }

            try {
                // get result
                responseCode = connection.getResponseCode();
            } catch(IOException e) {
                throw new FailedConnectionException("Could not get response from server.", e);
            }

            try {
                // see if there was an error defined with the "x-error" header in the response
                String responseError = connection.getHeaderField("x-error");
                headers = connection.getHeaderFields();

                if (responseError != null ){
                    // there was an error defined by the "x-error" header
                    throw new FailedRequestException(responseCode, responseError, headers);
                }

                if (responseCode != 200) {
                    // the response code was something besides a successful 200
                    responseError = "Unexpected response code received: " + Integer.toString(responseCode);
                    throw new FailedRequestException(responseCode, responseError, headers);
                }

            } finally {
                if (connection != null) {
                    // disconnect if there is still a connection
                    connection.disconnect();
                }
            }

            // call the dispatch callback
            callCallback(callback,
                    true,
                    this.endpoint,
                    headers,
                    udo,
                    null);
        } catch (FailedRequestException e) {
            callCallback(callback,
                    false,
                    this.endpoint,
                    e.headers,
                    udo,
                    e.toString());
        } catch (FailedConnectionException e) {
            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    udo,
                    e.toString());
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Call the dispatch callback with the results of the collect call
     *
     * @param callback The callback to call
     * @param success true if successful, false otherwise
     * @param encodedUrl The url endpoint
     * @param headerFields Response headers
     * @param data data sent to endpoint
     * @param errorMessage message of what went wrong, null if nothing went wrong
     */
    private void callCallback(Tealium.DispatchCallback callback,
                              boolean success,
                              String encodedUrl,
                              Map<String, List<String>> headerFields,
                              Udo data,
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
}
