package com.questionpro.cx;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.model.TouchPoint;

public class ExampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_activity);
        //app launch event as this is first screen

        Button sendFeedback=findViewById(R.id.send_feedback);
        sendFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuestionProCX.engageTouchPoint(ExampleActivity.this, new TouchPoint(7657393));
            }
        });
    }
}
