package com.prosectura;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class TrafficLightActivity extends Activity {
	GLSurfaceView mGLView;
	TrafficLightRenderer mRenderer;

	private boolean hasGLES20() {
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		Log.i("Prosectura", "Gles Version: " + (info.reqGlEsVersion >> 16) + "." + (info.reqGlEsVersion & 0xffff));
		return info.reqGlEsVersion >= 0x20000;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if (hasGLES20()) {
			mGLView = new GLSurfaceView(this);
			mGLView.setEGLContextClientVersion(2);
			mGLView.setPreserveEGLContextOnPause(true);
			mGLView.setRenderer(mRenderer = new TrafficLightRenderer(getAssets()));
		} else {
			Log.e("Prosectura", "Gles 2.0 not supported.");
			return;
		}

		setContentView(mGLView);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
		{
			Log.w("prosectura_action", "ACTION_POINTER_DOWN");
		} else if (event.getAction() == MotionEvent.ACTION_UP)
		{
			Log.w("prosectura_action", "ACTION_POINTER_UP");
			mRenderer.GetTL().SetNextTargetState();
		}
		return super.onTouchEvent(event);
	}

}
