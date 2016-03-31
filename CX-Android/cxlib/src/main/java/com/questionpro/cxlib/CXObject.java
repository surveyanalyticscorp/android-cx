package com.questionpro.cxlib;

/**
 * Created by sachinsable on 29/03/16.
 */
public class CXObject {
    public long touchPointID;
    public String apiKey;

    public String getDialogPromptTitleText() {
        return dialogPromptTitleText;
    }

    public void setDialogPromptTitleText(String dialogPromptTitleText) {
        this.dialogPromptTitleText = dialogPromptTitleText;
    }

    public String getDialogPromptMessageText() {
        return dialogPromptMessageText;
    }

    public void setDialogPromptMessageText(String dialogPromptMessageText) {
        this.dialogPromptMessageText = dialogPromptMessageText;
    }

    public String getDialogPromptPositiveText() {
        return dialogPromptPositiveText;
    }

    public void setDialogPromptPositiveText(String dialogPromptPositiveText) {
        this.dialogPromptPositiveText = dialogPromptPositiveText;
    }

    public String getDialogPromptNegativeText() {
        return dialogPromptNegativeText;
    }

    public void setDialogPromptNegativeText(String dialogPromptNegativeText) {
        this.dialogPromptNegativeText = dialogPromptNegativeText;
    }

    private String dialogPromptTitleText=null;
    private String dialogPromptMessageText=null;
    private String dialogPromptPositiveText=null;
    private String dialogPromptNegativeText=null;
    public CXObject(long touchPointID, String apiKey) {
        this.touchPointID = touchPointID;
        this.apiKey = apiKey;
    }
}
