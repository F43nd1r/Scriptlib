package com.faendir.lightning_launcher.scriptlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import net.pierrox.lightning_launcher.plugin.IScriptService;
import net.pierrox.lightning_launcher.plugin.IScriptService_Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author lukas
 * @since 08.02.19
 */
public class LightningServiceManager {
	private final Object lock = new Object();
	private final Context context;
	private IScriptService service;
	private Connector connector;

	public LightningServiceManager(@NonNull Context context) {
		this.context = context;
	}

	public FluentFuture<IScriptService> getScriptService() {
		SettableFuture<IScriptService> future = SettableFuture.create();
		synchronized (lock) {
			if (service != null) {
				future.set(service);
			} else if (connector != null) {
				connector.addFuture(future);
			} else {
				Intent intent = new Intent();
				intent.setClassName("net.pierrox.lightning_launcher_extreme", "net.pierrox.lightning_launcher.util.ScriptService");
				ResolveInfo info = context.getPackageManager().resolveService(intent, 0);
				if (info != null) {
					try {
						PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
						if(packageInfo.versionCode >= 328){
							connector = new Connector();
							ListenableFuture<Boolean> permission = PermissionActivity.checkForPermission(context, IScriptService.PERMISSION);
							permission.addListener(() -> {
								try {
									if (permission.get()) {
										connector.addFuture(future);
										context.bindService(intent, connector, Context.BIND_AUTO_CREATE);
									} else {
										future.setException(new PermissionDeniedException());
										synchronized (lock) {
											connector = null;
										}
									}
								} catch (ExecutionException e) {
									e.printStackTrace();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}, Runnable::run);
						} else {
							future.setException(new LauncherOutdatedException());
						}
					} catch (PackageManager.NameNotFoundException e) {
						future.setException(e);
					}
				} else {
					future.setException(new LauncherOutdatedException());
				}
			}
		}
		return FluentFuture.from(future);
	}

	public void closeConnection() {
		synchronized (lock) {
			if (connector != null) {
				context.unbindService(connector);
				service = null;
				connector = null;
			}
		}
	}

	private class Connector implements ServiceConnection {
		private final List<SettableFuture<IScriptService>> futures = new ArrayList<>();

		public void addFuture(SettableFuture<IScriptService> future) {
			futures.add(future);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			synchronized (lock) {
				service = new IScriptService_Proxy(binder);
				for (SettableFuture<IScriptService> future : futures) {
					future.set(service);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			synchronized (lock) {
				service = null;
				connector = null;
			}
		}
	}
}
