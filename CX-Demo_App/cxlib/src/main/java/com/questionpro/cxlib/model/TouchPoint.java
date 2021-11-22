package com.questionpro.cxlib.model;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    private final long touchPointID;
    private final boolean showPrompt;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String transactionLanguage;
    private final String mobile;
    private final String segmentCode;
    private final String transactionDate;

    public TouchPoint(TouchPointBuilder touchPointBuilder) {
        this.touchPointID = touchPointBuilder.touchPointID;
        this.showPrompt = touchPointBuilder.showPrompt;
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

    public boolean isShowPrompt() {
        return showPrompt;
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
        return "User: "+this.touchPointID+", "+this.showPrompt+",  "+this.firstName+", "+this.lastName+", "
                +this.email+", "+this.transactionLanguage+", "+this.transactionDate
                +", "+this.mobile+", "+this.segmentCode;
    }

    public static class TouchPointBuilder{
        private final long touchPointID;
        private final String email;
        private  boolean showPrompt;
        private String firstName;
        private String lastName;
        private String transactionLanguage;
        private String transactionDate;
        private String mobile;
        private String segmentCode;

        public TouchPointBuilder(long surveyId, String emailId){
            this.touchPointID = surveyId;
            this.email = emailId;
        }

        public TouchPointBuilder firstName(String fName){
            this.firstName = fName;
            return this;
        }

        public TouchPointBuilder lastName(String lName){
            this.lastName = lName;
            return this;
        }

        public TouchPointBuilder showPrompt(boolean email){
            this.showPrompt = showPrompt;
            return  this;
        }

        public TouchPointBuilder transactionLanguage(String transactionLanguage){
            this.transactionLanguage = transactionLanguage;
            return this;
        }
         public TouchPointBuilder transactionDate(String transactionDate){
            this.transactionDate = transactionDate;
            return this;
         }

         public TouchPointBuilder mobile(String mobile){
            this.mobile = mobile;
            return this;
         }

         public TouchPointBuilder segmentCode(String segmentCode){
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
