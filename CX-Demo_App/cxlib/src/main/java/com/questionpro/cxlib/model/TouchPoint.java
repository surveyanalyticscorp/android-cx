package com.questionpro.cxlib.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    //private final long touchPointID;
    private final String transactionLanguage;
    private final boolean showAsDialog;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String mobile;
    private final String segmentCode;
    private final String transactionDate;
    private final String themeColor;

    public TouchPoint(Builder builder) {
        //this.touchPointID = builder.touchPointID;
        this.showAsDialog = builder.showAsDialog;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.transactionLanguage = builder.transactionLanguage;
        this.mobile = builder.mobile;
        this.segmentCode = builder.segmentCode;
        this.transactionDate = builder.transactionDate;
        this.themeColor = builder.themeColor;
    }

    /*public long getTouchPointID() {
        return touchPointID;
    }*/

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

    public String getThemeColor(){
        return themeColor;
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
        private  boolean showAsDialog;
        private String firstName;
        private String lastName;
        private String transactionLanguage;
        private String transactionDate;
        private String mobile;
        private String segmentCode;
        private String themeColor = "";

        public Builder(/*long surveyId, String emailId*/){
            //this.touchPointID = surveyId;
            //this.email = emailId;
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
