package com.questionpro.cxlib.model;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    private long touchPointID;
    private boolean showPrompt = false;

    public boolean isDialog() {
        return isDialog;
    }

    public void setIsDialog(boolean isDialog) {
        this.isDialog = isDialog;
    }

    private boolean isDialog = false;
    private String dialogPromptTitleText=null;
    private String dialogPromptMessageText=null;
    private String dialogPromptPositiveButtonText =null;
    private String dialogPromptNegativeButtonText =null;

    public TouchPoint(long touchPointID) {
        this.touchPointID = touchPointID;
        this.showPrompt = false;
    }

    public long getTouchPointID() {
        return touchPointID;
    }

    public void setTouchPointID(long touchPointID) {
        this.touchPointID = touchPointID;
    }


    public boolean isShowPrompt() {
        return showPrompt;
    }

    public void setShowPrompt(boolean showPrompt) {
        this.showPrompt = showPrompt;
    }

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

    public String getDialogPromptPositiveButtonText() {
        return dialogPromptPositiveButtonText;
    }

    public void setDialogPromptPositiveButtonText(String dialogPromptPositiveButtonText) {
        this.dialogPromptPositiveButtonText = dialogPromptPositiveButtonText;
    }

    public String getDialogPromptNegativeButtonText() {
        return dialogPromptNegativeButtonText;
    }

    public void setDialogPromptNegativeButtonText(String dialogPromptNegativeButtonText) {
        this.dialogPromptNegativeButtonText = dialogPromptNegativeButtonText;
    }
}
