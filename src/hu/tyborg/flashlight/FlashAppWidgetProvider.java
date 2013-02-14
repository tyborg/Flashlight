package hu.tyborg.flashlight;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceView;
import android.widget.RemoteViews;

public class FlashAppWidgetProvider extends AppWidgetProvider {

	public static final String ACTION_FLASHLIGHT = "hu.tyborg.flashlight.ACTION_FLASHLIGHT";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int n = appWidgetIds.length;

		for (int i = 0; i < n; i++) {
			int appWidgetId = appWidgetIds[i];
			appWidgetManager.updateAppWidget(appWidgetId,
					getRemoteViews(context));
		}
	}

	public static RemoteViews getRemoteViews(Context context) {
		PendingIntent pendingIntent;
		Intent intent = new Intent(context, MainActivity.class);
		intent.setAction(MainActivity.hasFlashLighting(context) ? MainActivity.ACTION_FLASHLIGHT_ON
				: MainActivity.ACTION_SCREENLIGHT_ON);
		pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		RemoteViews views = new RemoteViews(
				context.getPackageName(),
				MainActivity.isFlashLighting()
						|| !MainActivity.hasFlashLighting(context) ? R.layout.light_on_appwidget
						: R.layout.light_off_appwidget);
		views.setOnClickPendingIntent(R.id.appwidget_image, pendingIntent);
		return views;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_FLASHLIGHT)) {
			SurfaceView surface = new SurfaceView(context);
			MainActivity.setFlashLight(context, surface.getHolder());
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			ComponentName thisAppWidget = new ComponentName(
					context.getPackageName(),
					FlashAppWidgetProvider.class.getName());
			int[] appWidgetIds = appWidgetManager
					.getAppWidgetIds(thisAppWidget);
			onUpdate(context, appWidgetManager, appWidgetIds);
		}
		super.onReceive(context, intent);
	}

}
