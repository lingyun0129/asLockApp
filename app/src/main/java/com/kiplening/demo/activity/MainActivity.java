package com.kiplening.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kiplening.demo.R;
import com.kiplening.demo.module.App;
import com.kiplening.demo.service.LockService;
import com.kiplening.demo.tools.DataBaseHelper;
import com.kiplening.demo.tools.DataBaseUtil;
import com.kiplening.demo.tools.ListViewAdapter;
import com.kiplening.demo.tools.SlidingMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static MainActivity instance;
    private List<Map<String, Object>> listItems;
    private ListView myList;
    private ListViewAdapter listViewAdapter;
    private ArrayList<String> lockList = new ArrayList<String>();

    private ArrayList<App> lockedApps;

    private SQLiteDatabase db;

    private String dataBaseName = "kiplening";
    private String tableName = "app";
    public final DataBaseHelper helper = new DataBaseHelper(this,dataBaseName,null,1,null);
    private DataBaseUtil dataBaseUtil = new DataBaseUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);


        instance = this;

        db = helper.getWritableDatabase();
        lockedApps = dataBaseUtil.getAll(db);

        listItems = new ArrayList<Map<String,Object>>();
        //Intent intent = getIntent();
        ArrayList<String> appList = new ArrayList<String>();
        List<PackageInfo> packages = getPackageManager()
                .getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            // for (ResolveInfo resolveInfo : allMatches) {
            PackageInfo packageInfo = packages.get(i);


            if (isUserApp(packageInfo)) {
                appList.add(packageInfo.packageName);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("info", "installed app");
                map.put("name",
                        packageInfo.applicationInfo.loadLabel(
                                getPackageManager()).toString());
                map.put("packageName", packageInfo.applicationInfo.packageName);
                map.put("icon", packageInfo.applicationInfo
                        .loadIcon(getPackageManager()));
                if (isLocked(packageInfo,lockedApps)){
                    map.put("flag", "已锁定");
                    lockList.add(packageInfo.applicationInfo.packageName);
                }else {
                    map.put("flag", "锁定");
                }

                //lockList.add(packageInfo.applicationInfo.packageName);
                listItems.add(map);
                Log.i("test", packageInfo.applicationInfo.loadLabel(
                        getPackageManager()).toString());
            }
        }

        // 左侧视图
        View leftViewGroup = createLeftListView();

        //　右侧视图
        View listView = createRightListView();

        final SlidingMenu mSlidingMenu = new SlidingMenu(this);
        mSlidingMenu.addLeftView(leftViewGroup);
        mSlidingMenu.addRightView(listView);

        setContentView(mSlidingMenu);

        //myList = (ListView)findViewById(R.id.list);
        //listViewAdapter = new ListViewAdapter(this,listItems);
        //myList.setAdapter(listViewAdapter);

        Intent intent = new Intent(MainActivity.this, LockService.class);
        intent.putStringArrayListExtra("lockList", lockList);
        startService(intent);


    }

    private View createLeftListView() {
        LinearLayout linearLayout = new LinearLayout(getBaseContext());
        linearLayout.setLayoutParams(new LayoutParams(300, LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textViewOne = createTextView("开启应用锁", Color.WHITE);
        TextView textViewTwo = createTextView("绑定邮箱", Color.WHITE);
        TextView textViewThree = createTextView("修改密码", Color.WHITE);
        TextView textViewFour = createTextView("关于", Color.WHITE);
        //TextView textViewFive = createTextView("俺是设置", Color.WHITE);
        linearLayout.addView(textViewOne);
        linearLayout.addView(textViewTwo);
        linearLayout.addView(textViewThree);
        linearLayout.addView(textViewFour);
        //linearLayout.addView(textViewFive);

        return linearLayout;
    }


    private TextView createTextView(final String text, final int color) {
        TextView textView = new TextView(getBaseContext());
        textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setText(text);
        textView.setPadding(0, 50, 0, 50);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(color);
        textView.setTextColor(Color.BLACK);
        textView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Toast.makeText(getBaseContext(), "click  " + text, Toast.LENGTH_SHORT).show();
            }
        });
        return textView;
    }

    private View createRightListView() {
        //ListView listView = (ListView)findViewById(R.id.list);
        ListView listView = new ListView(this);

        ListViewAdapter customBaseAdapter = new ListViewAdapter(getBaseContext(), listItems);
        listView.setAdapter(customBaseAdapter);

        //listView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        //listView.setDivider(new ColorDrawable(Color.BLACK));
        //listView.setDividerHeight(1);
        //listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        //    @Override
        //    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
        //                            long id) {
        //        Toast.makeText(getBaseContext(), "World position = " + position, Toast.LENGTH_SHORT).show();
        //    }
        //});
        return listView;
    }

    @Override
    protected void onStop() {
        lockedApps = dataBaseUtil.getAll(db);
        super.onStop();
    }

    public boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public boolean isUserApp(PackageInfo pInfo) {
        return (!isSystemApp(pInfo) && !isSystemUpdateApp(pInfo));
    }
    public boolean isLocked(PackageInfo pInfo,ArrayList<App> lockedApps){
        for (App a:lockedApps) {
            if (pInfo.applicationInfo.packageName.equals(a.getPackageName())){

                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
