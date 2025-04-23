package com.qpcx.retailapp.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.qpcx.retailapp.R;
import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.model.TouchPoint;
//import com.questionpro.cxlib.model.Type;

public class ShoppingCartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shopping_cart);

        Button button = findViewById(R.id.launch_survey);
        button.setText("Shopping_Cart");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US).build();
                //QuestionProCX.getInstance().init(ShoppingCartActivity.this, touchPoint);
                //QuestionProCX.getInstance().launchFeedbackSurvey(11543913);
                QuestionProCX.getInstance().setScreenVisited("Shopping_Cart");
            }
        });

        Button buttonOne = findViewById(R.id.launch_survey_one);
        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuestionProCX.getInstance().setScreenVisited("Checkout");
            }
        });
    }
}
