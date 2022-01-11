# android-cx
	Minimum SDK Version: 16
	Compile SDK Version: 30 (But you should generally compile with the latest version)

### Including CX Lib into your project

#### Integration From Maven Repository
	Our SDK is available from the central Maven repo. Add it in your root build.gradle.
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

	Add the dependancy of the latest version of QuestionPro CX in your app's build.gradle.
	dependencies {
		...
		implementation 'com.github.surveyanalyticscorp:android-cx:1.2.7'
	}


#### Modify your manifest: 

	Add following permissions:
  	<uses-permission android:name="android.permission.INTERNET"/>
  	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

	Following is required:
	Add the api key obtained from CX portal:
		<meta-data 
			android:name="cx_manifest_api_key" 
			android:value="<your api key here>"/>

	Add the following activity to show the feedback screen:
		<activity android:name="com.questionpro.cxlib.interaction.InteractionActivity"
  	 		android:theme="@android:style/Theme.Translucent.NoTitleBar"
  	 		android:configChanges="keyboardHidden"
   			android:windowSoftInputMode="adjustPan" >
		</activity>

### Initializing and Adding TouchPoint hook in app codebase
	Initialization and configuration:
		TouchPoint touchPoint = new TouchPoint.Builder().build()
		QuestionProCX.init(this, touchPoint);
	
	You can add touchpoint hook wherever you want to show the feedback screen.
		QuestionProCX.launchFeedbackSurvey(110);
>here, the number “110” is the touchpoint ID(Feedback Survey ID) that is obtained from QuestionPro Cx Portal.



