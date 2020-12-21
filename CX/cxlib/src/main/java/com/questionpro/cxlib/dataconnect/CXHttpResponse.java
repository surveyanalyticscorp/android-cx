package com.questionpro.cxlib.dataconnect;

import java.util.Map;

/**
 * Created by sachinsable on 14/04/16.
 */
public class CXHttpResponse {
    private String content;
    private String reason;
    private Map<String, String> headers;
    private int code;
    private boolean badPayload;

    public CXHttpResponse() {
        content = null;
        reason = null;
        code = -1;
        badPayload = false;
    }

    public boolean isException() {
        return code < 0;
    }

    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public boolean isRejectedPermanently() {
        return code >= 400 && code < 500;
    }

    public boolean isRejectedTemporarily() {
        // Not successful and not in the range of [400, 500)
        return !(isSuccessful() || isRejectedPermanently());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isBadPayload() {
        return badPayload;
    }

    public void setBadPayload(boolean badPayload) {
        this.badPayload = badPayload;
    }

    public boolean isZipped() {
        if (headers != null) {
            String contentEncoding = headers.get("Content-Encoding");
            return contentEncoding != null && contentEncoding.equalsIgnoreCase("[gzip]");
        }
        return false;
    }
}
