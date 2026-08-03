package com.questionpro.cxlib.dataconnect;

import android.util.Log;

import com.questionpro.cxlib.util.CXUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by sachinsable on 14/04/16.
 */
public class CXUploadClient {
    public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 30000;
    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 30000;
    private static final String LOG_TAG = "CXUploadClient";

    public static CXHttpResponse  uploadCXApi(URL url, HashMap<String, String> requestHeaders, String payload) {
        HttpURLConnection urlConnection = null;
        CXHttpResponse cxHttpResponse = new CXHttpResponse();
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            setHeadersToHttpConnection(urlConnection,requestHeaders);

            urlConnection.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(DEFAULT_HTTP_SOCKET_TIMEOUT);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            urlConnection.setFixedLengthStreamingMode(payloadBytes.length);

            OutputStream os = urlConnection.getOutputStream();
            os.write(payloadBytes);
            os.close();

            int responseCode = urlConnection.getResponseCode();
            cxHttpResponse.setCode(responseCode);
            cxHttpResponse.setReason(urlConnection.getResponseMessage());
            CXUtils.printLog(LOG_TAG,"Response Status Line: " + urlConnection.getResponseMessage());

            // Get the Http response header values (normalize keys to lowercase for consistent lookup)
            Map<String, String> headers = new HashMap<String, String>();
            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getKey() != null && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
                }
            }
            cxHttpResponse.setHeaders(headers);

            // Read the response, if available
            if (responseCode >= 200 && responseCode < 300) {
                cxHttpResponse.setContent(getResponse(urlConnection, cxHttpResponse.isZipped()));
                CXUtils.printLog("Response: ", cxHttpResponse.getContent());
            } else {
                cxHttpResponse.setContent(getErrorResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.w("Response: ", cxHttpResponse.getContent());
            }
        } catch (IllegalArgumentException e) {
            Log.w("IllegalArgument: ", e);
        } catch (SocketTimeoutException e) {
            Log.w("SocketTimeoutException:", e);
        } catch (final MalformedURLException e) {
            Log.w("MalformedUrlException", e);
        } catch (final Exception e) {
            Log.w("Exception", e);
            // Read the error response.
            try {
                cxHttpResponse.setContent(getErrorResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.w(LOG_TAG,"Response: " + cxHttpResponse.getContent());
            } catch (IOException ex) {
                Log.w("IOException:", ex);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return cxHttpResponse;
    }

    public static CXHttpResponse getCxApi(URL url, HashMap<String, String> requestHeaders) {
        HttpURLConnection urlConnection = null;
        CXHttpResponse cxHttpResponse = new CXHttpResponse();
        try{
            urlConnection = (HttpURLConnection) url.openConnection();
            setHeadersToHttpConnection(urlConnection, requestHeaders);
            urlConnection.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(DEFAULT_HTTP_SOCKET_TIMEOUT);
            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);

            int responseCode = urlConnection.getResponseCode();
            cxHttpResponse.setCode(responseCode);
            cxHttpResponse.setReason(urlConnection.getResponseMessage());
            //Log.d(LOG_TAG,"Response Status Line: " + urlConnection.getResponseMessage());

            // Get the Http response header values (normalize keys to lowercase for consistent lookup)
            Map<String, String> headers = new HashMap<String, String>();
            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getKey() != null && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
                }
            }
            cxHttpResponse.setHeaders(headers);
            if (responseCode >= 200 && responseCode < 300) {
                cxHttpResponse.setContent(getResponse(urlConnection, cxHttpResponse.isZipped()));
                CXUtils.printLog("Get Api Response: ", cxHttpResponse.getContent());
            } else {
                cxHttpResponse.setContent(getErrorResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.w("Get Api Response: ", cxHttpResponse.getContent());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "GET request failed", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return cxHttpResponse;
    }

    private static String getResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
        if (connection == null) return null;
        InputStream is = null;
        try {
            is = new BufferedInputStream(connection.getInputStream());
            if (isZipped) {
                is = new GZIPInputStream(is);
            }
            return CXUtils.convertStreamToString(is);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static String getErrorResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
        if (connection == null) return null;
        InputStream is = null;
        try {
            is = connection.getErrorStream();
            if (is == null) return null;
            if (isZipped) {
                is = new GZIPInputStream(is);
            }
            return CXUtils.convertStreamToString(is);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static void setHeadersToHttpConnection(HttpURLConnection urlConnection, HashMap<String, String> headers){
        urlConnection.setRequestProperty("Content-Type", "application/json; charSet=UTF-8");
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            urlConnection.setRequestProperty(key, value);
        }
    }
}
