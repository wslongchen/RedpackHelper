package cn.mrpan.redpackhelper.activity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AnalogClock;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cn.mrpan.redpackhelper.R;
import cn.mrpan.redpackhelper.service.CiycleService;
import cn.mrpan.redpackhelper.utils.AppUtils;
import cn.mrpan.redpackhelper.utils.ConnectivityUtil;
import cn.mrpan.redpackhelper.utils.UpdateTask;

public class MainActivity extends Activity implements AccessibilityManager.AccessibilityStateChangeListener {

    //开关切换按钮
    private TextView pluginStatusText;
    private ImageView pluginStatusIcon;
    //AccessibilityService 管理
    private AccessibilityManager accessibilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pluginStatusText = (TextView) findViewById(R.id.layout_control_accessibility_text);
        pluginStatusIcon = (ImageView) findViewById(R.id.layout_control_accessibility_icon);

        handleMaterialStatusBar();

        explicitlyLoadPreferences();

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
        updateServiceStatus();
        startedService();

    }

    private void explicitlyLoadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
    }

    /**
     * 适配MIUI沉浸状态栏
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleMaterialStatusBar() {
        // Not supported in APK level lower than 21
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        Drawable drawable1 =new ColorDrawable(getResources().getColor(R.color.statusBar));
        window.setStatusBarColor(getResources().getColor(R.color.statusBar));

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateServiceStatus();
        // Check for update when WIFI is connected or on first time.
        if (ConnectivityUtil.isWifi(this) || UpdateTask.count == 0)
            new UpdateTask(this, false).update();
    }

    @Override
    protected void onDestroy() {
        //移除监听服务
        accessibilityManager.removeAccessibilityStateChangeListener(this);
        flag=false;
        super.onDestroy();
    }

    public void openMore(View view){
        Toast.makeText(this, getString(R.string.click_more), Toast.LENGTH_LONG).show();
    }

    public void openAccessibility(View view) {
        try {
            Toast.makeText(this, getString(R.string.turn_on_toast) + pluginStatusText.getText(), Toast.LENGTH_SHORT).show();
            Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibleIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.turn_on_error_toast), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    public void openWechat(View view) {
        new TipPopupWindows(this,pluginStatusIcon,1);
    }

    public void openSettings(View view) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra("title", getString(R.string.preference));
        settingsIntent.putExtra("frag_id", "GeneralSettingsFragment");
        startActivity(settingsIntent);
    }


    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateServiceStatus();

    }

    boolean flag=false;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!flag) {
            //showWindow();
            flag=true;
        }

    }
    /**
     * 更新当前 HongbaoService 显示状态
     */
    private void updateServiceStatus() {
        if (isServiceEnabled()) {
            pluginStatusText.setText(R.string.service_off);
            pluginStatusIcon.setBackgroundResource(R.mipmap.ic_stop);
        } else {
            pluginStatusText.setText(R.string.service_on);
            pluginStatusIcon.setBackgroundResource(R.mipmap.ic_start);
        }
    }

    private void showWindow(){
        if (!isServiceEnabled()) {
            new TipPopupWindows(this,pluginStatusIcon,0);
        }
    }

    /**
     * 获取 HongbaoService 是否启用状态
     *
     * @return
     */
    private boolean isServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.service.HongbaoService")) {
                return true;
            }
        }
        return false;
    }

    /***
     * 打开对话框
     */
    public class TipPopupWindows extends PopupWindow implements View.OnClickListener{

        private Context mContext;
        private int type;
        public TipPopupWindows(Context mContext, View parent,int type) {

            final View view = View
                    .inflate(mContext, R.layout.dialog_tip, null);
            view.startAnimation(AnimationUtils.loadAnimation(mContext,
                    R.anim.fade_ins));
            RelativeLayout ll_popup = (RelativeLayout) view
                    .findViewById(R.id.tip_popup);
            ll_popup.startAnimation(AnimationUtils.loadAnimation(mContext,
                    R.anim.push_bottom_in_2));
            //setOutsideTouchable(true);
            this.mContext=mContext;
            this.type=type;
            setContentView(view);
            init(view);
            setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            setBackgroundDrawable(new BitmapDrawable());
            setFocusable(true);
            showAtLocation(parent, Gravity.CENTER_HORIZONTAL, 0, 0);
            backgroundAlpha(0.7f);
            update();

            setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss() {
                    backgroundAlpha(1f);
                }
            });

        }

        private void init(View view){
            Button button = (Button)view.findViewById(R.id.btn_apply);
            ImageView close = (ImageView)view.findViewById(R.id.iv_close);
            ImageView snow=(ImageView)view.findViewById(R.id.main_bg_snow);
            ImageView wechat=(ImageView)view.findViewById(R.id.main_bg_wechat);
            button.setOnClickListener(this);
            close.setOnClickListener(this);
            if(type==1){
                snow.setVisibility(View.INVISIBLE);
                button.setVisibility(View.INVISIBLE);
                wechat.setVisibility(View.VISIBLE);
            }else{
                snow.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                wechat.setVisibility(View.INVISIBLE);
            }
        }

        public void backgroundAlpha(float bgAlpha)
        {
            WindowManager.LayoutParams lp = ((Activity)mContext).getWindow().getAttributes();
            lp.alpha = bgAlpha; //0.0-1.0
            ((Activity)mContext).getWindow().setAttributes(lp);
        }

        @Override
        public void onClick(View v) {

            switch (v.getId()){
                case R.id.iv_close:
                    dismiss();
                    break;
                case R.id.btn_apply:

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 开启周期服务
     */
    private void startedService() {
        //开启服务
        Intent intent = new Intent(MainActivity.this,CiycleService.class);
        startService(intent);
        if(!isServiceEnabled()){
            Toast.makeText(this,"请开启插件！",Toast.LENGTH_SHORT).show();
        }
    }
}

