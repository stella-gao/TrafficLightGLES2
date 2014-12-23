package com.prosectura;

import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class TrafficLightActivity extends Activity {
	GLSurfaceView mGLView;
	TrafficLightRenderer mRenderer;
	private boolean isPebbleConnected = false;
	
	private final static UUID TLC_UUID = UUID.fromString("10d4fbe9-fdae-407f-8696-80130bafbd92");

	private boolean hasGLES20() {
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		Log.i("Prosectura", "Gles Version: " + (info.reqGlEsVersion >> 16) + "." + (info.reqGlEsVersion & 0xffff));
		return info.reqGlEsVersion >= 0x20000;
	}
	
	private class PebbleConnected extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			isPebbleConnected = true;
		}
	}
	
	private class PebbleDisconnected extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			isPebbleConnected = false;
		}
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
		
		isPebbleConnected = PebbleKit.isWatchConnected(getApplicationContext());
		if (isPebbleConnected) {
			PebbleKit.startAppOnPebble(getApplicationContext(), TLC_UUID);
		}
		PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new PebbleConnected());
		PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new PebbleDisconnected());
		
		final Handler handler = new Handler();
		PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(TLC_UUID) {
			@Override
			public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						switch (data.getInteger(0).intValue())
						{
						case 1:
							mRenderer.GetTL().SetRed();
							break;
						case 2:
							mRenderer.GetTL().SetBlinking();
							break;
						case 3:
							mRenderer.GetTL().SetGreen();
							break;
						default:
							break;
						}
						PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
					}
				});
			}
		});
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
			int iaxisY = 800 >> 2;
			if (event.getAxisValue(MotionEvent.AXIS_Y) <= (iaxisY*1)) {
				mRenderer.GetTL().SetRed();
			} else if (event.getAxisValue(MotionEvent.AXIS_Y) <= (iaxisY*2)) {
				mRenderer.GetTL().SetBlinking();
			} else if (event.getAxisValue(MotionEvent.AXIS_Y) <= (iaxisY*3)) {
				mRenderer.GetTL().SetGreen();
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isPebbleConnected) {
			PebbleKit.closeAppOnPebble(getApplicationContext(), TLC_UUID);
		}
	}
}
