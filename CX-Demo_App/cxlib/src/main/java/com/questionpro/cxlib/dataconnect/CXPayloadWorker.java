package com.questionpro.cxlib.dataconnect;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;


public class CXPayloadWorker {

    private static final String LOG_TAG="CXPayloadWorker";
    private static CXPayloadSendThread sPayloadSendThread;

    private static AtomicBoolean appInForeground = new AtomicBoolean(false);
    private static AtomicBoolean threadRunning = new AtomicBoolean(false);

    // A synchronized getter/setter to the static instance of thread object
    public static synchronized CXPayloadSendThread getAndSetPayloadSendThread(boolean expect,
                                                                            boolean createNew,
                                                                            Context context) {
        if (expect && createNew && context != null) {
            sPayloadSendThread = createPayloadSendThread(context);
        } else if (!expect) {
            sPayloadSendThread = null;
        }
        return  sPayloadSendThread;
    }


    private static CXPayloadSendThread createPayloadSendThread(final Context appContext) {
        CXPayloadSendThread newThread = new CXPayloadSendThread(appContext);
        newThread.setName("Apptentive-PayloadSendWorker");
        newThread.start();
        return newThread;
    }

    private static class CXPayloadSendThread extends Thread {
        private WeakReference<Context> contextRef;

        public CXPayloadSendThread(Context appContext) {
            contextRef = new WeakReference<Context>(appContext);
        }

        public void run() {
            try {
                if (appInForeground.get()) {
                    if (contextRef.get() == null) {
                        threadRunning.set(false);
                        return;
                    }

                    CXPayloadSendThread thread = getAndSetPayloadSendThread(true, false, null);
                    if (thread != null && thread != CXPayloadSendThread.this) {
                        Log.i(LOG_TAG, "something wrong");
                        return;
                    }

                    if (!CXUtils.isNetworkConnectionPresent(contextRef.get())) {
                        Log.d(LOG_TAG, "Can't send payloads. No network connection.");
                        return;
                    }
                    Log.v(LOG_TAG, "Checking for payloads to send.");
                    String payload = CXGlobalInfo.getApiPayload((Activity)contextRef.get());
                    //JSONObject payloadObj = new JSONObject(payload);
                    CXHttpResponse response = CXUploadClient.uploadforCX(contextRef.get(), payload);
                    if (response != null) {
                        QuestionProCX questionProCX = new QuestionProCX();
                        if (response.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(response.getContent());
                            if(jsonObject.has(CXConstants.JSONResponseFields.RESPONSE)){
                                JSONObject responseJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                                responseJson.put(CXConstants.JSONResponseFields.IS_DIALOG,CXGlobalInfo.isShowDialog(contextRef.get()));
                                responseJson.put(CXConstants.JSONResponseFields.THEME_COLOR,CXGlobalInfo.getThemeColour(contextRef.get()));
                                CXInteraction cxInteraction = CXInteraction.fromJSON(responseJson);

                                if(!cxInteraction.url.equalsIgnoreCase("Empty") && URI.create(cxInteraction.url).isAbsolute()){
                                    Activity activity = (Activity) contextRef.get();
                                    //long surveyID = CXGlobalInfo.getSurveyIDFromPayload(payload);
                                    //CXGlobalInfo.storeInteraction(activity, surveyID, cxInteraction);
                                    if(!activity.isFinishing()){
                                        QuestionProCX.launchFeedbackScreen(activity, cxInteraction);
                                    }
                                }else{
                                    questionProCX.onError(responseJson);
                                }
                            }
                            //Log.d(LOG_TAG,"Payload submission successful" + response.getContent());

                        } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                            Log.v("Rejected json:", response.getContent());
                            JSONObject jsonObject = new JSONObject(response.getContent());
                            if(jsonObject.has("response")) {
                                questionProCX.onError(jsonObject.getJSONObject("response"));
                            }
                        } else if (response.isRejectedTemporarily()) {
                            Log.d(LOG_TAG,"Unable to send JSON");
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            finally{
                Log.v(LOG_TAG,"Stopping PayloadSendThread.");
                threadRunning.set(false);
            }
        }
    }

    private static void wakeUp() {
        CXPayloadSendThread thread = getAndSetPayloadSendThread(true, false, null);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public static void appWentToForeground(Activity context) {
        appInForeground.set(true);
        if (threadRunning.compareAndSet(false, true)) {
			/* appInForeground was "false", and set to "true"
			*  thread was not running, and set to be running
			*/
            getAndSetPayloadSendThread(true, true, context);
        } else {
            wakeUp();
        }
    }

    public static void appWentToBackground() {
        appInForeground.set(false);
        wakeUp();
    }
}
