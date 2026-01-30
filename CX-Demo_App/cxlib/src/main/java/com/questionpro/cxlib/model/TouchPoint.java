package com.questionpro.cxlib.model;

import androidx.annotation.NonNull;

import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.enums.Platform;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TouchPoint implements Serializable{

    private final String transactionDate;

    /** Setting related variables */
    private final DataCenter dataCenter;
    private  final Platform platform;

    public TouchPoint(Builder builder) {
        this.transactionDate = builder.transactionDate;
        this.dataCenter = builder.dataCenter;
        this.platform = builder.platform;
    }

    private String getTransactionDate() {
        return transactionDate;
    }

    public DataCenter getDataCenter(){
        return dataCenter;
    }

    public Platform getPlatform(){
        return platform;
    }

    @NonNull
    @Override
    public String toString() {
        return "User: "+this.transactionDate
                +", ";
    }

    /**
     * TouchPoint Builder class
     */
    public static class Builder{

        private String transactionDate;

        private DataCenter dataCenter = null;

        private Platform platform = Platform.ANDROID;
        //private Map<Integer, String> customVariables;

        public Builder(DataCenter dataCenter){
            this.dataCenter = dataCenter;
        }

        public Builder setPlatform(Platform platform){
            this.platform = platform;
            return this;
        }

        //Return the finally constructed object
        public TouchPoint build(){
            TouchPoint touchPoint = new TouchPoint(this);
            validateTouchPoint(touchPoint);
            return touchPoint;
        }

        private void validateTouchPoint(TouchPoint touchPoint){

        }
    }
}
