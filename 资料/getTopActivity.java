//实现获取栈顶Activity的多种方法，没有验证全部的。  博文地址：http://blog.csdn.net/include_u/article/details/50558130


//方法一   在魅族flyme5.0上面不能正确运行，其他机型未尝试

String packname = ""; /* Android5.0之后获取程序锁的方式是不一样的*/ 
if (Build.VERSION.SDK_INT > 20) { 
// 5.0及其以后的版本 
	List<RunningAppProcessInfo> tasks = am.getRunningAppProcesses(); 
	if (null != tasks && tasks.size() > 0) { 
		packname = tasks.get(0).processName; 
	} 
	} else{ 
		// 5.0之前 
		// 获取正在运行的任务栈(一个应用程序占用一个任务栈) 最近使用的任务栈会在最前面 
		// 1表示给集合设置的最大容量 List<RunningTaskInfo> infos = am.getRunningTasks(1); 
		// 获取最近运行的任务栈中的栈顶Activity(即用户当前操作的activity)的包名 
		packname = infos.get(0).topActivity.getPackageName(); 
}


//方法二   和方法一原理一样
public class DetectCalendarLaunchRunnable implements Runnable {  
  
	@Override  
	public void run() {  
		String[] activePackages;  
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {  
			activePackages = getActivePackages();  
		} else {  
			activePackages = getActivePackagesCompat();  
		}  
		if (activePackages != null) {  
			for (String activePackage : activePackages) {  
				if (activePackage.equals("com.google.android.calendar")) {  
					//Calendar app is launched, do something  
				}  
			}  
		}  
	mHandler.postDelayed(this, 1000);  
}  
  
	String[] getActivePackagesCompat() {  
		final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);  
		final ComponentName componentName = taskInfo.get(0).topActivity;  
		final String[] activePackages = new String[1];  
		activePackages[0] = componentName.getPackageName();  
		return activePackages;  
	}  
  
	String[] getActivePackages() {  
		final Set<String> activePackages = new HashSet<String>();  
		final List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();  
		for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {  
			if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {  
				activePackages.addAll(Arrays.asList(processInfo.pkgList));  
			}  
		}  
		return activePackages.toArray(new String[activePackages.size()]);  
	}  
}

//方法三   据说是一种绕路的方式来实现，没测试

public void updateServiceNotification(String message) { 
    if (!PreferenceUtils.getPrefBoolean(this, 
        PreferenceConstants.FOREGROUND, true)) 
        return; 
    String title = PreferenceUtils.getPrefString(this, 
        PreferenceConstants.ACCOUNT, ""); 
    Notification n = new Notification(R.drawable.login_default_avatar, 
        title, System.currentTimeMillis()); 
    n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR; 
   
    Intent notificationIntent = new Intent(this, MainActivity.class); 
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
    n.contentIntent = PendingIntent.getActivity(this, 0, 
        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT); 
   
    n.setLatestEventInfo(this, title, message, n.contentIntent); 
    startForeground(SERVICE_NOTIFICATION, n); 
} 
 Runnable monitorStatus = new Runnable() { 
    public void run() { 
        try { 
            L.i("monitorStatus is running... " + mPackageName); 
            mMainHandler.removeCallbacks(monitorStatus); 
            // 如果在后台运行并且连接上了 
            if (!isAppOnForeground()) { 
                L.i("app run in background..."); 
                // if (isAuthenticated())//不判断是否连接上了。 
                updateServiceNotification(getString(R.string.run_bg_ticker)); 
                return;// 服务已在前台运行，可以停止重复执行此任务 
            } else { 
                stopForeground(true); 
            } 
            mMainHandler.postDelayed(monitorStatus, 1000L); 
        } catch (Exception e) { 
            e.printStackTrace(); 
            L.i("monitorStatus:"+e.getMessage()); 
        } 
    }
 }
 
 //官方找的关于Android 5.0的新特性
 PRIVATE VOID LISTTASKS() THROWS PACKAGEMANAGER.NAMENOTFOUNDEXCEPTION {
	ACTIVITYMANAGER MGR = (ACTIVITYMANAGER)GETSYSTEMSERVICE(CONTEXT.ACTIVITY_SERVICE);
	LIST<ACTIVITYMANAGER.APPTASK>  TASKS = MGR.GETAPPTASKS();
	STRING PACKAGENAME;
	STRING LABEL;
	FOR (ACTIVITYMANAGER.APPTASK TASK: TASKS){
		PACKAGENAME = TASK.GETTASKINFO().BASEINTENT.GETCOMPONENT().GETPACKAGENAME();
		LABEL = GETPACKAGEMANAGER().GETAPPLICATIONLABEL(GETPACKAGEMANAGER().GETAPPLICATIONINFO(PACKAGENAME, PACKAGEMANAGER.GET_META_DATA)).TOSTRING();
		LOG.V(TAG,PACKAGENAME + ":" + LABEL);
	}
}


//判断当前设备中有没有“有权查看使用权限的应用”这个选项
private boolean isNoOption() {  
    PackageManager packageManager = getApplicationContext()  
            .getPackageManager();  
    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);  
    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,  
            PackageManager.MATCH_DEFAULT_ONLY);  
    return list.size() > 0;  
}  
//判断调用该设备中“有权查看使用权限的应用”这个选项的APP有没有打开
private boolean isNoSwitch() {
	long ts = System.currentTimeMillis();
	UsageStatsManager usageStatsManager = (UsageStatsManager) getApplicationContext()
		.getSystemService("usagestats");
	List queryUsageStats = usageStatsManager.queryUsageStats(
	UsageStatsManager.INTERVAL_BEST, 0, ts);
	if (queryUsageStats == null || queryUsageStats.isEmpty()) {
		return false;
	}
	return true;
}
//然后就是跳转的代码了：
if (isNoOption()) {
	buttonGuide.setOnClickListener(new OnClickListener() {
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(
		Settings.ACTION_USAGE_ACCESS_SETTINGS);
		startActivity(intent);
	}
});

private String getRunningApp() {
    long ts = System.currentTimeMillis();
    List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,ts-2000, ts);
    if (queryUsageStats == null || queryUsageStats.isEmpty()) {
        return null;
    }
    UsageStats recentStats = null;
    for (UsageStats usageStats : queryUsageStats) {
        if (recentStats == null || recentStats.getLastTimeUsed() < usageStats.getLastTimeUsed()) {
            recentStats = usageStats;
        }
    }
    return recentStats.getPackageName();
}
