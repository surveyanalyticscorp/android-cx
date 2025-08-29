package com.qpcx.retailapp.view.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.qpcx.retailapp.R;
import com.questionpro.cxlib.CXConstants;
import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.interfaces.IQuestionProCallback;
import com.questionpro.cxlib.model.TouchPoint;
//import com.questionpro.cxlib.model.Type;

public class ShoppingCartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shopping_cart);

        Button button = findViewById(R.id.launch_survey);
        button.setText("Launch Survey");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TouchPoint touchPoint = new TouchPoint.Builder(ConfigType.INTERCEPT, DataCenter.US).build();
                //QuestionProCX.getInstance().init(ShoppingCartActivity.this, touchPoint);
                //QuestionProCX.getInstance().launchFeedbackSurvey(13026667);
                QuestionProCX.getInstance().setScreenVisited("screen1");
            }
        });

        Button buttonOne = findViewById(R.id.launch_survey_one);
        buttonOne.setText("Checkout");
        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuestionProCX.getInstance().setScreenVisited("Checkout");
            }
        });

        QuestionProCX.getInstance().gerSurveyUrl(new IQuestionProCallback() {
            @Override
            public void getSurveyUrl(String surveyUrl) {
                Log.d("Datta","Survey URL: "+surveyUrl);
            }
        });
    }
}
