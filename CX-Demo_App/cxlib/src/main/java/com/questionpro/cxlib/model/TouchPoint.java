package com.questionpro.cxlib.model;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    private final long touchPointID;
    private final String transactionLanguage;
    private final boolean showAsDialog;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String mobile;
    private final String segmentCode;
    private final String transactionDate;

    public TouchPoint(TouchPointInit touchPointBuilder) {
        this.touchPointID = touchPointBuilder.touchPointID;
        this.showAsDialog = touchPointBuilder.showAsDialog;
        this.firstName = touchPointBuilder.firstName;
        this.lastName = touchPointBuilder.lastName;
        this.email = touchPointBuilder.email;
        this.transactionLanguage = touchPointBuilder.transactionLanguage;
        this.mobile = touchPointBuilder.mobile;
        this.segmentCode = touchPointBuilder.segmentCode;
        this.transactionDate = touchPointBuilder.transactionDate;
    }

    public long getTouchPointID() {
        return touchPointID;
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

    @Override
    public String toString() {
        return "User: "+this.touchPointID+", "+this.showAsDialog+",  "+this.firstName+", "+this.lastName+", "
                +this.email+", "+this.transactionLanguage+", "+this.transactionDate
                +", "+this.mobile+", "+this.segmentCode;
    }

    /**
     * TouchPoint Builder class
     */
    public static class TouchPointInit{
        private final long touchPointID;
        private final String email;
        private  boolean showAsDialog;
        private String firstName;
        private String lastName;
        private String transactionLanguage;
        private String transactionDate;
        private String mobile;
        private String segmentCode;

        public TouchPointInit(long surveyId, String emailId){
            this.touchPointID = surveyId;
            this.email = emailId;
        }

        public TouchPointInit firstName(String fName){
            this.firstName = fName;
            return this;
        }

        public TouchPointInit lastName(String lName){
            this.lastName = lName;
            return this;
        }

        public TouchPointInit showAsDialog(boolean showAsDialog){
            this.showAsDialog = showAsDialog;
            return  this;
        }

        public TouchPointInit transactionLanguage(String transactionLanguage){
            this.transactionLanguage = transactionLanguage;
            return this;
        }
         public TouchPointInit transactionDate(String transactionDate){
            this.transactionDate = transactionDate;
            return this;
         }

         public TouchPointInit mobile(String mobile){
            this.mobile = mobile;
            return this;
         }

         public TouchPointInit segmentCode(String segmentCode){
            this.segmentCode = segmentCode;
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
