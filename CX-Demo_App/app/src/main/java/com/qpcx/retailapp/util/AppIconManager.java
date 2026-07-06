package com.qpcx.retailapp.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Switches the launcher icon between the activity-aliases declared in AndroidManifest.xml.
 * Exactly one alias is enabled at a time; PackageManager persists that enabled/disabled
 * state across app restarts and reboots, so no separate persistence layer is needed.
 */
public class AppIconManager {

	public enum IconVariant {
		DEFAULT("com.qpcx.retailapp.AppIconDefault"),
		ICON_ONE("com.qpcx.retailapp.AppIconOne"),
		ICON_TWO("com.qpcx.retailapp.AppIconTwo");

		final String alias;

		IconVariant(String alias) {
			this.alias = alias;
		}
	}

	public static void switchTo(Context context, IconVariant target) {
		PackageManager pm = context.getPackageManager();
		String packageName = context.getPackageName();

		for (IconVariant variant : IconVariant.values()) {
			int state = variant == target
					? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
					: PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

			pm.setComponentEnabledSetting(
					new ComponentName(packageName, variant.alias),
					state,
					PackageManager.DONT_KILL_APP);
		}
	}

	public static IconVariant getCurrentIcon(Context context) {
		PackageManager pm = context.getPackageManager();
		String packageName = context.getPackageName();

		for (IconVariant variant : IconVariant.values()) {
			int state = pm.getComponentEnabledSetting(new ComponentName(packageName, variant.alias));
			boolean isEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
					|| (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && variant == IconVariant.DEFAULT);
			if (isEnabled) {
				return variant;
			}
		}
		return IconVariant.DEFAULT;
	}
}
