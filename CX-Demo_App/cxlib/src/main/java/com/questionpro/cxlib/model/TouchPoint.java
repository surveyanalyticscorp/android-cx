package com.questionpro.cxlib.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    private final String transactionLanguage;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String mobile;
    private final String segmentCode;
    private final String transactionDate;
    private final String customVariable1;
    private final String customVariable2;
    private final String customVariable3;

    /** Setting related variables */
    private final boolean showAsDialog;
    private final String themeColor;
    private final Type mType;
    private final DataCenter dataCenter;

    public TouchPoint(Builder builder) {
        this.showAsDialog = builder.showAsDialog;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.transactionLanguage = builder.transactionLanguage;
        this.mobile = builder.mobile;
        this.segmentCode = builder.segmentCode;
        this.transactionDate = builder.transactionDate;
        this.customVariable1 = builder.customVariable1;
        this.customVariable2 = builder.customVariable2;
        this.customVariable3 = builder.customVariable3;
        this.themeColor = builder.themeColor;
        this.mType = builder.mType;
        this.dataCenter = builder.dataCenter;
    }

    public boolean showAsDialog() {
        return showAsDialog;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail(){
        return email;
    }

    public String getTransactionLanguage(){
        return transactionLanguage;
    }

    public String getMobile(){
        return mobile;
    }

    public String getSegmentCode(){
        return segmentCode;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public String getCustomVariable1(){
        return customVariable1;
    }
    public String getCustomVariable2(){
        return customVariable2;
    }
    public String getCustomVariable3(){
        return customVariable3;
    }

    public String getThemeColor(){
        return themeColor;
    }

    public Type getType() {
        return mType;
    }

    public DataCenter getDataCenter(){
        return dataCenter;
    }

    @NonNull
    @Override
    public String toString() {
        return "User: "/*+this.touchPointID+",*/ +this.showAsDialog+",  "+this.firstName+", "+this.lastName+", "
                +this.email+", "+this.transactionLanguage+", "+this.transactionDate
                +", "+this.mobile+", "+this.segmentCode;
    }

    /**
     * TouchPoint Builder class
     */
    public static class Builder{
        //private final long touchPointID;
        private String email;
        private String firstName;
        private String lastName;
        private String transactionLanguage;
        private String transactionDate;

        private String customVariable1;
        private String customVariable2;
        private String customVariable3;
        private String mobile;
        private String segmentCode;

        private  boolean showAsDialog;
        private String themeColor = "";
        private Type mType = null;
        private DataCenter dataCenter = null;

        public Builder(Type type, DataCenter dataCenter){
            this.mType = type;
            this.dataCenter = dataCenter;
        }

        public Builder email(String email){
            this.email = email;
            return this;
        }
        public Builder firstName(String fName){
            this.firstName = fName;
            return this;
        }

        public Builder lastName(String lName){
            this.lastName = lName;
            return this;
        }

        public Builder showAsDialog(boolean showAsDialog){
            this.showAsDialog = showAsDialog;
            return  this;
        }

        public Builder transactionLanguage(String transactionLanguage){
            this.transactionLanguage = transactionLanguage;
            return this;
        }
         public Builder transactionDate(String transactionDate){
            this.transactionDate = transactionDate;
            return this;
         }

         public Builder customVariable1(String customVariable1){
            this.customVariable1 = customVariable1;
            return this;
         }

        public Builder customVariable2(String customVariable2){
            this.customVariable2 = customVariable2;
            return this;
        }

        public Builder customVariable3(String customVariable3){
            this.customVariable3 = customVariable3;
            return this;
        }

         public Builder mobile(String mobile){
            this.mobile = mobile;
            return this;
         }

         public Builder segmentCode(String segmentCode){
            this.segmentCode = segmentCode;
            return this;
         }

         public Builder themeColor (String themeColor){
            this.themeColor = themeColor;
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
