package com.questionpro.cxlib.model;

import androidx.annotation.NonNull;

import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.enums.DataCenter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TouchPoint implements Serializable{
    private final String transactionLanguage;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String mobile;
    private final String segmentCode;
    private final String transactionDate;

    /** Setting related variables */
    private final boolean showAsDialog;
    private final String themeColor;
    private final DataCenter dataCenter;
    private final ConfigType configType;

    //private final Map<Integer, String> customVariables;

    public TouchPoint(Builder builder) {
        this.showAsDialog = builder.showAsDialog;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.transactionLanguage = builder.transactionLanguage;
        this.mobile = builder.mobile;
        this.segmentCode = builder.segmentCode;
        this.transactionDate = builder.transactionDate;
        //this.customVariables = builder.customVariables;
        this.themeColor = builder.themeColor;
        this.dataCenter = builder.dataCenter;
        this.configType = builder.configType;
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
    /*public Map<Integer, String> getCustomVariables(){
        return customVariables;
    }*/
    public String getThemeColor(){
        return themeColor;
    }

    public DataCenter getDataCenter(){
        return dataCenter;
    }
    public ConfigType getConfigType(){return configType;}

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

        private String mobile;
        private String segmentCode;

        private  boolean showAsDialog;
        private String themeColor = "";
        private DataCenter dataCenter = null;
        private ConfigType configType;

        //private Map<Integer, String> customVariables;

        public Builder(DataCenter dataCenter){
            this.dataCenter = dataCenter;
            this.configType = configType;
        }

        private Builder email(String email){
            this.email = email;
            return this;
        }
        private Builder firstName(String fName){
            this.firstName = fName;
            return this;
        }

        private Builder lastName(String lName){
            this.lastName = lName;
            return this;
        }

        private Builder showAsDialog(boolean showAsDialog){
            this.showAsDialog = showAsDialog;
            return  this;
        }

        private Builder transactionLanguage(String transactionLanguage){
            this.transactionLanguage = transactionLanguage;
            return this;
        }
         private Builder transactionDate(String transactionDate){
            this.transactionDate = transactionDate;
            return this;
         }

        /*private Builder customVariables(HashMap<Integer, String> customVars){
            this.customVariables = customVars;
            return this;
        }*/

        private Builder mobile(String mobile){
            this.mobile = mobile;
            return this;
         }

         private Builder segmentCode(String segmentCode){
            this.segmentCode = segmentCode;
            return this;
         }

         private Builder themeColor (String themeColor){
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
