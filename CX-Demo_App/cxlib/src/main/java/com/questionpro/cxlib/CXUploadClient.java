package com.questionpro.cxlib;

import android.content.Context;
import android.util.Log;

import com.questionpro.cxlib.dataconnect.CXHttpResponse;
import com.questionpro.cxlib.model.Type;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

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

    protected static CXHttpResponse uploadApiCallForCX(Context context, String payload) {
        HttpURLConnection urlConnection = null;
        CXHttpResponse cxHttpResponse = new CXHttpResponse();
        try {
            Map.Entry<String, Map<String, String>> encrypted = QuestionProCX.getInstance().getEncryptedData(payload);
            String encryptedPayload = encrypted.getKey();
            CXUtils.printLogs("Encrypted Payload: "+encryptedPayload);

            java.net.URL mURL = new URL(CXConstants.getUrl());
            Log.d("Datta","URL: "+mURL);
            if (!CXUtils.isNetworkConnectionPresent(context)) {
                Log.d(LOG_TAG,"Network unavailable.");
                return cxHttpResponse;
            }
            urlConnection = (HttpURLConnection) mURL.openConnection();
            /*urlConnection.setRequestProperty("Content-Type", "application/json; charSet=UTF-8");
            urlConnection.setRequestProperty("api-key", CXGlobalInfo.getInstance().getApiKey());*/
            setRequestProperty(urlConnection, encrypted.getValue());
            urlConnection.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(DEFAULT_HTTP_SOCKET_TIMEOUT);
            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);

            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(encryptedPayload.length());
            OutputStream os = urlConnection.getOutputStream();
            os.write(encryptedPayload.getBytes(StandardCharsets.UTF_8));
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
                //Log.v("Response: ", cxHttpResponse.getContent());
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
        }
        return cxHttpResponse;
    }

    private static void setRequestProperty(HttpURLConnection urlConnection, Map<String, String> headers){
        for (Map.Entry<String, String> innerEntry : headers.entrySet()) {
            urlConnection.setRequestProperty(innerEntry.getKey(), innerEntry.getValue());
        }
    }

    private static String getResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
        if (connection != null) {
            InputStream is = null;
                is = new BufferedInputStream(connection.getInputStream());
            if (isZipped) {
                is = new GZIPInputStream(is);
            }
            return CXUtils.convertStreamToString(is);
        }
        return null;
    }


    private static String getErrorResponse(HttpURLConnection connection, boolean isZipped) throws IOException {
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
