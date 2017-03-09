package cn.mrpan.redpackhelper.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.mrpan.redpackhelper.R;
import cn.mrpan.redpackhelper.utils.AppUtils;

import static android.R.attr.duration;

/**
 * Created by Mr.Pan on 2017/3/9.
 */

public class CiycleService extends Service {
    private static int duration = 30000;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private BroadcastReceiver mBatInfoReceiver;
    private TimeCount time;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();

       // startTimeCount();
    }

    /**
     * 注册广播
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
//        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //监听日期变化
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction("com.example.service_destory");
        filter.addAction("com.example.open_app");

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d("xf", "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d("xf", "screen off");
                    //改为60秒一存储
                    duration = 60000;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d("xf", "screen unlock");
//                    save();
                    //改为30秒一存储
                    duration = 30000;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    Log.i("xf", " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");

                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Log.i("xf", " receive ACTION_SHUTDOWN");
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//日期变化步数重置为0

                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //时间变化步数重置为0
                    isCall();
                    Log.e("时间变化",getCurrentDate());

                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//日期变化步数重置为0
                    isCall();
                    Log.e("时间变化2",getCurrentDate());
                }else if("com.example.service_destory".equals(action)){
                    Log.e("un", "上次服务被挂了");
                }else if("com.example.open_app".equals(action)){
                    AppUtils.doStartApplicationWithPackageName(getApplicationContext(),"com.alibaba.android.rimet");
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter);
    }

    String getCurrentDate(){
        SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    /**
     * 监听时间变化提醒用户锻炼
     */
    private void isCall() {
        String time="08:37:00";
        if(time.equals(getCurrentDate())){
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    AppUtils.doStartApplicationWithPackageName(getApplicationContext(),"com.alibaba.android.rimet");
                }
            }).run();*/
            Intent i = new Intent("com.example.open_app");
            sendBroadcast(i);
            initNotify();
        }
    }


    void startRecieve(){
        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        //创建Intent
        Intent intent = new Intent("android.intent.action.SETP_BROADCASET");
        //LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        //周期触发
        manager.setRepeating(AlarmManager.RTC, 0, 5 * 1000, pendingIntent);


    }


    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
    /**
     * Notification构造器
     */
    android.support.v4.app.NotificationCompat.Builder mBuilder;
    /**
     * Notification的ID
     */
    int notifyId = 100;

    /**
     * 初始化通知栏
     */
    private void initNotify() {

        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(this);
        mBuilder.setContentTitle("打卡记录")
                .setContentText("已经成功打卡")
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
//				.setNumber(number)//显示数量
                .setTicker("提醒您，为您打卡成功")//通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setPriority(Notification.PRIORITY_DEFAULT)//设置该通知优先级
                .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：
                //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.mipmap.ic_launcher);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyId, mBuilder.build());
    }

    private void startTimeCount() {
        Intent intent = new Intent();
        intent.setAction("cn.mrpan.daka");
        getApplicationContext().sendBroadcast(intent);
        time = new TimeCount(duration, 1000);
        time.start();

    }
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消前台进程
        stopForeground(true);
        unregisterReceiver(mBatInfoReceiver);
        Log.e("un", "Service onDestory");
        Intent i = new Intent("com.example.service_destory");
        sendBroadcast(i);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * @获取默认的pendingIntent,为了防止2.3及以下版本报错
     * @flags属性: 在顶部常驻:Notification.FLAG_ONGOING_EVENT
     * 点击去除： Notification.FLAG_AUTO_CANCEL
     */
    public PendingIntent getDefalutIntent(int flags) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }
}
