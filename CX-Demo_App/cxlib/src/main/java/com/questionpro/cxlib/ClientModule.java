package com.questionpro.cxlib;

import android.util.Log;

import com.questionpro.cxlib.interfaces.ClientModuleCallback;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class ClientModule{
    private String accessToken; // Initial access token
    private final ClientModuleCallback clientModuleCallback;

    public ClientModule(String accessToken, ClientModuleCallback clientModuleCallback) {
        this.accessToken = accessToken;
        this.clientModuleCallback = clientModuleCallback;
        initModuleTest();
    }

    private void initModuleTest() {
        encryptedModuleData("Test Module data");

        try {
            Thread.sleep(2000); // Simulate some delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            e.printStackTrace();
        }

        getNewAccessToken();

        try {
            Thread.sleep(2000); // Simulate some delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            e.printStackTrace();
        }

        decryptedModuleData();
    }

    private void encryptedModuleData(String dataToEncrypt) {
        Map.Entry<String, Map<String, String>> encryptedData = clientModuleCallback.encryptData(dataToEncrypt);
        Log.d("Datta","ClientModule encrypted data received: " + encryptedData);
    }

    private void getNewAccessToken() {
        String newAccessToken = clientModuleCallback.refreshToken();
        Log.d("Datta","ClientModule new access token received: " + newAccessToken);
    }

    private void decryptedModuleData() {
        String encryptedData = "Encrypted data";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Header-key", "Header value");

        // Using AbstractMap.SimpleEntry to represent Pair in Java
        // Alternatively, you could create a custom data class/record for apiResponse
        Map.Entry<String, Map<String, String>> apiResponse =
                new AbstractMap.SimpleEntry<>(encryptedData, headers);

        String decryptedData = clientModuleCallback.decryptedData(apiResponse);
        Log.d("Datta","API decrypted data received: " + decryptedData);
    }
}
