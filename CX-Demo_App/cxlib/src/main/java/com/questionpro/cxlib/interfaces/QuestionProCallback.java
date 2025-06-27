package com.questionpro.cxlib.interfaces;

import java.util.Map;

public interface QuestionProCallback {
    String refreshToken();
    Map.Entry<String, Map<String, String>> encryptData(String data);
    String decryptedData(Map.Entry<String, Map<String, String>> apiResponse);
}
