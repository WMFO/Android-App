package com.tufts.wmfo;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dFB1UllnVmlnTEp0ajNWcldpektydnc6MQ", 
customReportContent = { ReportField.APP_VERSION_CODE, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.STACK_TRACE, ReportField.LOGCAT },
logcatArguments = { "-t", "1000", "-v", "time"})
public class WMFOApp extends Application {
	
	@Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }

}
