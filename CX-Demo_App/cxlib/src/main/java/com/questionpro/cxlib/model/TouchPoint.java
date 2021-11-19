package com.questionpro.cxlib.model;

import java.io.Serializable;

public class TouchPoint implements Serializable{
    private final long touchPointID;
    private final boolean showPrompt;
    private final String firstName = "";
    private final String lastName = "";
    private final String email = "";

    public TouchPoint(TouchPointBuilder touchPointBuilder) {
        this.touchPointID = touchPointBuilder.touchPointID;
        this.showPrompt = touchPointBuilder.showPrompt;
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

    @Override
    public String toString() {
        return "User: "+this.touchPointID+", "+this.showPrompt+",  "+this.firstName+", "+this.lastName+", "+this.email;
    }

    public static class TouchPointBuilder{
        private final long touchPointID;
        private final boolean showPrompt;
        private String firstName;
        private String lastName;
        private String email;

        public TouchPointBuilder(long touchPointId, boolean showPrompt){
            this.touchPointID = touchPointId;
            this.showPrompt = showPrompt;
        }

        public TouchPointBuilder firstName(String fName){
            this.firstName = fName;
            return this;
        }

        public TouchPointBuilder lastName(String lName){
            this.lastName = lName;
            return this;
        }

        public TouchPointBuilder email(String email){
            this.email = email;
            return  this;
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
