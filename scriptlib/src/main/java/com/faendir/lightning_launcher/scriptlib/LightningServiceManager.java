package com.faendir.lightning_launcher.scriptlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import com.google.common.util.concurrent.ListenableFuture;
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

	public ListenableFuture<IScriptService> getPluginService() {
		ResolvableFuture<IScriptService> future = ResolvableFuture.create();
		synchronized (lock) {
			if (service != null) {
				future.set(service);
			} else if (connector != null) {
				connector.addFuture(future);
			} else {
				connector = new Connector();
				ListenableFuture<Boolean> permission = PermissionActivity.checkForPermission(context, IScriptService.PERMISSION);
				permission.addListener(() -> {
					try {
						if (permission.get()) {
							Intent intent = new Intent();
							intent.setClassName("net.pierrox.lightning_launcher_extreme", "net.pierrox.net.pierrox.net.pierrox.lightning_launcher.util.PluginService");
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
			}
		}
		return future;
	}

	public void closeConnection() {
		if (connector != null) {
			context.unbindService(connector);
		}
	}

	private class Connector implements ServiceConnection {
		private final List<ResolvableFuture<IScriptService>> futures = new ArrayList<>();

		public void addFuture(ResolvableFuture<IScriptService> future) {
			futures.add(future);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			synchronized (lock) {
				service = new IScriptService_Proxy(binder);
				for (ResolvableFuture<IScriptService> future : futures) {
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
