package vital.test;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 *
 */

public class SwipeApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Stetho.initializeWithDefaults(this);
	}
}
