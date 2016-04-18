package com.questionpro.cx;

import android.app.Activity;
import android.os.Bundle;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.model.TouchPoint;

public class ExampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_activity);
        //app launch event as this is first screen
        QuestionProCX.engageTouchPoint(this, new TouchPoint(118));

    }

}
