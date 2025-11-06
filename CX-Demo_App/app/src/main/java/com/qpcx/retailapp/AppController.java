package com.qpcx.retailapp;

import static com.google.common.io.Resources.getResource;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.qpcx.retailapp.util.PreferenceHelper;
import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.interfaces.IQuestionProInitCallback;
import com.questionpro.cxlib.model.TouchPoint;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.HashMap;

@ReportsCrashes(mailTo = "dattakunde@questionpro.com",
		customReportContent = {
			ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
			ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
			ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT
		},
		mode = ReportingInteractionMode.TOAST)

public class AppController extends Application {

	public static final String TAG = AppController.class.getSimpleName();

	private RequestQueue mRequestQueue;

	private static AppController mInstance;

	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;

		// The following line triggers the initialization of ACRA for crash Log Reposrting
		if (PreferenceHelper.getPrefernceHelperInstace().getBoolean(
				this, PreferenceHelper.SUBMIT_LOGS, true)) {
			//ACRA.init(this);
			//ACRA.getConfig().setResToastText(R.string.crash_toast_text);
		}

		Log.d("Datta","Application onCreate:"+getPackageName().equals(getProcessName(this)));
		if (getPackageName().equals(getProcessName(this))) {
			initialiseQpSdk(this);
		}
	}

	public static String getProcessName(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int pid = android.os.Process.myPid();
		for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
			if (processInfo.pid == pid) {
				return processInfo.processName;
			}
		}
		return null;
	}

	private void initialiseQpSdk(AppController appController){
		TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.EU)
				.build();
		QuestionProCX.getInstance().init(this, touchPoint, new IQuestionProInitCallback() {
			@Override
			public void onInitializationSuccess(String message) {
				Log.d("Datta", "onInitializationSuccess: "+message);
			}

			@Override
			public void onInitializationFailure(String error) {
				Log.d("Datta", "onInitializationFailure: "+error);
			}
		});

		//QuestionProCX.getInstance().init(getApplication(), touchPoint);
	}

	public static synchronized AppController getInstance() {
		return mInstance;
	}
}