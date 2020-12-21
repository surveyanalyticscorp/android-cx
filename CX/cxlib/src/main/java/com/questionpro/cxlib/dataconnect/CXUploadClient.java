package com.questionpro.cxlib.dataconnect;

import android.content.Context;
import android.util.Log;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.util.CXUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
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
    public static CXHttpResponse uploadforCX(Context context,String payload) {
        HttpURLConnection urlConnection = null;
        CXHttpResponse cxHttpResponse = new CXHttpResponse();
        try {
            java.net.URL uRL = new URL(CXConstants.getCXUploadURL(CXGlobalInfo.apiKey));

            if (!CXUtils.isNetworkConnectionPresent(context)) {
                Log.d(LOG_TAG,"Network unavailable.");
                return cxHttpResponse;
            }
            urlConnection = (HttpURLConnection) uRL.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charSet=UTF-8");
            urlConnection.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(DEFAULT_HTTP_SOCKET_TIMEOUT);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(payload.length());
            OutputStream os = urlConnection.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.close();

            int responseCode = urlConnection.getResponseCode();
            cxHttpResponse.setCode(responseCode);
            cxHttpResponse.setReason(urlConnection.getResponseMessage());
            Log.d(LOG_TAG,"Response Status Line: " + urlConnection.getResponseMessage());

            // Get the Http response header values
            Map<String, String> headers = new HashMap<String, String>();
            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                headers.put(entry.getKey(), entry.getValue().toString());
            }
            cxHttpResponse.setHeaders(headers);

            // Read the response, if available

            if (responseCode >= 200 && responseCode < 300) {
                cxHttpResponse.setContent(getResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.v("Response: %s", cxHttpResponse.getContent());
            } else {
                cxHttpResponse.setContent(getErrorResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.w("Response: %s", cxHttpResponse.getContent());
            }
        } catch (IllegalArgumentException e) {
            Log.w("Error from server.", e);
        } catch (SocketTimeoutException e) {
            Log.w("Timeout from server.", e);
        } catch (final MalformedURLException e) {
            Log.w("MalformedUrlException", e);
        } catch (final IOException e) {
            Log.w("IOException", e);
            // Read the error response.
            try {
                cxHttpResponse.setContent(getErrorResponse(urlConnection, cxHttpResponse.isZipped()));
                Log.w(LOG_TAG,"Response: " + cxHttpResponse.getContent());
            } catch (IOException ex) {
                Log.w("Can't read error .", ex);
            }
        }
        return cxHttpResponse;
    }
    public static String getResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
        if (connection != null) {
            InputStream is = null;

                is = new BufferedInputStream(connection.getInputStream());
                if (is != null) {
                    if (isZipped) {
                        is = new GZIPInputStream(is);
                    }
                    return CXUtils.convertStreamToString(is);
                }

        }
        return null;
    }


    public static String getErrorResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
        if (connection != null) {
            InputStream is = null;

                is = connection.getErrorStream();
                if (is != null) {
                    if (isZipped) {
                        is = new GZIPInputStream(is);
                    }
                }
                return CXUtils.convertStreamToString(is);

        }
        return null;
    }
}
