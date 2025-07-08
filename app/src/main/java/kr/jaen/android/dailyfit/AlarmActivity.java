package kr.jaen.android.dailyfit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmActivity extends AppCompatActivity
        implements AlarmAdapter.OnAlarmActionListener {

    private static final String TAG      = "AlarmActivity";
    private static final String PREFS    = "alarms_prefs";
    private static final String KEY_LIST = "alarms_json";

    private AlarmManager       manager;
    private List<AlarmItem>    alarms;
    private AlarmAdapter       adapter;

    private Button             btnTabSetting, btnTabList;
    private View               viewSetting, viewList;
    private DatePicker         datePicker;
    private TimePicker         timePicker;
    private Button             btnDateReg, btnDateCancel;
    private RecyclerView       rvAlarms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        manager      = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        btnTabSetting= findViewById(R.id.btnTabSetting);
        btnTabList   = findViewById(R.id.btnTabList);
        viewSetting  = findViewById(R.id.view_alarm_setting);
        viewList     = findViewById(R.id.view_alarm_list);

        datePicker   = findViewById(R.id.datePicker);
        timePicker   = findViewById(R.id.timePicker);
        btnDateReg   = findViewById(R.id.btnDateReg);
        btnDateCancel= findViewById(R.id.btnDateCancel);
        rvAlarms     = findViewById(R.id.rvAlarms);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setIs24HourView(true);
        }

        btnTabSetting.setOnClickListener(v -> {
            viewSetting.setVisibility(View.VISIBLE);
            viewList   .setVisibility(View.GONE);
        });
        btnTabList.setOnClickListener(v -> {
            viewSetting.setVisibility(View.GONE);
            viewList   .setVisibility(View.VISIBLE);
        });

        // 1) 불러오기
        alarms = loadAlarms();
        adapter = new AlarmAdapter(alarms, manager, this);
        rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        rvAlarms.setAdapter(adapter);

        // 2) 등록 · 취소
        btnDateReg   .setOnClickListener(v -> addAlarm());
        btnDateCancel.setOnClickListener(v -> deleteLastAlarm());
    }

    private void addAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !manager.canScheduleExactAlarms()) {
            startActivity(new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        int y = datePicker.getYear(),
                m = datePicker.getMonth(),
                d = datePicker.getDayOfMonth(),
                h = timePicker.getHour(),
                mi= timePicker.getMinute();
        Calendar c = Calendar.getInstance();
        c.set(y, m, d, h, mi, 0);
        long at = c.getTimeInMillis();
        int code = 200 + alarms.size();

        scheduleAlarmClock(code, at);
        String disp = String.format("%04d-%02d-%02d %02d:%02d", y, m+1, d, h, mi);

        alarms.add(new AlarmItem(code, at, true, disp));
        adapter.notifyDataSetChanged();
        saveAlarms();
        Toast.makeText(this, "날짜 알람 등록", Toast.LENGTH_SHORT).show();
    }

    private void deleteLastAlarm() {
        if (alarms.isEmpty()) return;
        AlarmItem item = alarms.remove(alarms.size() - 1);
        cancelAlarm(item.reqCode);
        adapter.notifyDataSetChanged();
        saveAlarms();
        Toast.makeText(this, "마지막 알람 취소", Toast.LENGTH_SHORT).show();
    }

    @Override public void onDelete(AlarmItem item) {
        cancelAlarm(item.reqCode);
        alarms.remove(item);
        adapter.notifyDataSetChanged();
        saveAlarms();
        Toast.makeText(this, "삭제됨", Toast.LENGTH_SHORT).show();
    }

    @Override public void onToggle(AlarmItem item, boolean on) {
        if (on)  scheduleAlarmClock(item.reqCode, item.timeMillis);
        else    cancelAlarm(item.reqCode);
        item.isEnabled = on;
        saveAlarms();
    }

    @Override public void onEdit(AlarmItem item) {
        // 날짜 → 시간 순서로 다이얼로그
        new DatePickerDialog(this,
                (vw,y,m,d)-> new TimePickerDialog(this,
                        (vw2,h,mi)-> {
                            Calendar c = Calendar.getInstance();
                            c.set(y, m, d, h, mi, 0);
                            long at = c.getTimeInMillis();
                            cancelAlarm(item.reqCode);
                            scheduleAlarmClock(item.reqCode, at);
                            item.timeMillis  = at;
                            item.displayTime = String.format("%04d-%02d-%02d %02d:%02d",
                                    y, m+1, d, h, mi);
                            adapter.notifyDataSetChanged();
                            saveAlarms();
                            Toast.makeText(this, "수정됨", Toast.LENGTH_SHORT).show();
                        },
                        timePicker.getHour(),
                        timePicker.getMinute(),
                        true
                ).show(),
                datePicker.getYear(),
                datePicker.getMonth(),
                datePicker.getDayOfMonth()
        ).show();
    }

    private void scheduleAlarmClock(int code, long at) {
        PendingIntent pi = PendingIntent.getBroadcast(
                this, code, new Intent(this, AlarmReceiver.class),
                PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmClockInfo(at, pi), pi);
    }

    private void cancelAlarm(int code) {
        PendingIntent pi = PendingIntent.getBroadcast(
                this, code, new Intent(this, AlarmReceiver.class),
                PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);
    }

    private void saveAlarms() {
        try {
            JSONArray arr = new JSONArray();
            for (AlarmItem a : alarms) {
                JSONObject o = new JSONObject();
                o.put("code", a.reqCode);
                o.put("time", a.timeMillis);
                o.put("on", a.isEnabled);
                o.put("disp", a.displayTime);
                arr.put(o);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LIST, arr.toString())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "saveAlarms", e);
        }
    }

    private List<AlarmItem> loadAlarms() {
        List<AlarmItem> list = new ArrayList<>();
        try {
            String s = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(KEY_LIST, "[]");
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new AlarmItem(
                        o.getInt("code"),
                        o.getLong("time"),
                        o.getBoolean("on"),
                        o.getString("disp")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAlarms", e);
        }
        return list;
    }
}
