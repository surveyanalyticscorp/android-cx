package com.questionpro.cxlib.interfaces;

import android.util.Pair;

import java.util.Map;

public interface ClientModuleCallback {
    //String encryptData(String dataToEncrypt);
    Map.Entry<String, Map<String, String>> encryptData(String data);
    String refreshToken();
    String decryptedData(Map.Entry<String, Map<String, String>> apiResponse);
}
