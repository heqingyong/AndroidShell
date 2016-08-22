package club.younge.demo;

import android.app.Application;
import android.util.Log;

/**
 * 
 * 本类是被加壳应用的 Application
 * 
 * @author younge
 * 
 */
public class DemoApplication extends Application {

	private static DemoApplication context;

	public static DemoApplication getApplication() {
		return context;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("Demo", "DomeApplication");
	}

	public DemoApplication() {
		super();
		context = this;
	}
	@Override
	public void onLowMemory() {
		context = null;
		super.onLowMemory();
	}
	@Override
	public void onTerminate() {
		context = null;
		super.onTerminate();
	}

}