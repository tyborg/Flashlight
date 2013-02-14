package hu.tyborg.flashlight;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

	public static final String ACTION_SCREENLIGHT_ON = "hu.tyborg.flashlight.ACTION_SCREENLIGHT_ON";
	public static final String ACTION_FLASHLIGHT_ON = "hu.tyborg.flashlight.ACTION_FLASHLIGHT_ON";
	private static final String TAG = "MainActivity";
	private boolean mIsScreenLight;
	private View mMain;
	private static Camera mCamera;
	private TextView mFlashLightState;
	private TextView mScreenState;
	private SurfaceHolder mHolder;
	private boolean mFlashLightFromWidget;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prepareScreen(getIntent());
	}

	private void prepareScreen(Intent intent) {
		mFlashLightFromWidget = intent != null && ACTION_FLASHLIGHT_ON.equals(intent.getAction());
		if (mFlashLightFromWidget) {
			setContentView(R.layout.flash_light_from_widget);
		} else {
			getWindow()
					.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.activity_main);
			// Init the values
			mMain = findViewById(R.id.main);
			mFlashLightState = (TextView) findViewById(R.id.flashlight_state);
			mScreenState = (TextView) findViewById(R.id.screen_state);
			View screenButton = findViewById(R.id.screen_button);
			screenButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					setScreenLight(!mIsScreenLight);
				}
			});
			if (hasFlashLighting(this)) {
				findViewById(R.id.line).setVisibility(View.VISIBLE);
				View flashLightButton = findViewById(R.id.flashlight_button);
				flashLightButton.setVisibility(View.VISIBLE);
				flashLightButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						setFlashLight();
					}
				});
			}
		}
		SurfaceView surface = (SurfaceView) findViewById(R.id.surface);
		mHolder = surface.getHolder();
		mHolder.addCallback(this);
		if (getIntent() != null && ACTION_SCREENLIGHT_ON.equals(intent.getAction())) {
			setScreenLight(true);
		}
	}

	public void setFlashLight() {
		setFlashLight(this, mHolder);
		mFlashLightState.setText(isFlashLighting() ? R.string.on : R.string.off);
	}

	@SuppressLint("NewApi")
	public static void setFlashLight(Context context, SurfaceHolder surfaceHolder) {
		try {
			if (mCamera == null) {
				try {
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
						for (int cameraId = 0; mCamera == null && cameraId < Camera.getNumberOfCameras(); cameraId++) {
							CameraInfo cameraInfo = new CameraInfo();
							Camera.getCameraInfo(cameraId, cameraInfo);
							if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
								mCamera = Camera.open(cameraId);
							}
						}
					} else {
						mCamera = Camera.open();
					}
				} catch (Throwable ex) {
					Log.w(TAG, "setFlashLight - Open camera error! Message: " + ex.getMessage(), ex);
					Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show();
					return;
				}
				try {
					mCamera.setPreviewDisplay(surfaceHolder);
				} catch (IOException ex) {
					Log.w(TAG, "Camera setPreviewDisplay setting error!", ex);
				}
				Parameters params = mCamera.getParameters();
				params.setFlashMode(Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(params);
				mCamera.startPreview();
			} else {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		} catch (Throwable ex) {
			Log.w(TAG, "setFlashLight error! Message: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		prepareScreen(intent);
	}

	private void setScreenLight(boolean enabled) {
		if (enabled) {
			// Set screen brightness to max
			WindowManager.LayoutParams params = getWindow().getAttributes();
			params.screenBrightness = 1.0f;
			getWindow().setAttributes(params);
		}

		mIsScreenLight = enabled;
		mMain.setBackgroundColor(mIsScreenLight ? Color.WHITE : Color.BLACK);
		mScreenState.setText(mIsScreenLight ? R.string.on : R.string.off);
	}

	@Override
	protected void onDestroy() {
		if (mCamera != null && !mFlashLightFromWidget) {
			setFlashLight();
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		ComponentName thisWidget = new ComponentName(this, FlashAppWidgetProvider.class);
		appWidgetManager.updateAppWidget(thisWidget, FlashAppWidgetProvider.getRemoteViews(this));
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		if (mFlashLightState != null) {
			mFlashLightState.setText(isFlashLighting() ? R.string.on : R.string.off);
		}
		if (mScreenState != null) {
			setScreenLight(mIsScreenLight);
		}
		super.onResume();
		if (mFlashLightFromWidget) {
			setFlashLight(this, mHolder);
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					finish();
				}
			}, 300);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException ex) {
				Log.w(TAG, "Camera perview setting error!", ex);
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null && !mFlashLightFromWidget) {
			setFlashLight();
		}
	}

	public static boolean isFlashLighting() {
		return mCamera != null;
	}

	public static boolean hasFlashLighting(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
				&& !Build.MODEL.equals("GT-P1000");
	}
}
