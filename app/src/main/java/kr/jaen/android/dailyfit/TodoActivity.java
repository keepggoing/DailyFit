package kr.jaen.android.dailyfit;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class TodoActivity extends AppCompatActivity {

    private static class TodoItem {
        String text;
        boolean done;
        String category;
        int catColor;

        TodoItem(String t, String c, int col) {
            text = t; category = c; catColor = col; done = false;
        }
    }

    private static final String PREFS_NAME = "todo_prefs";
    private static final String PREF_KEY = "todos";

    private static final LinkedHashMap<String, Integer> CATEGORY_MAP = new LinkedHashMap<>();
    static {
        CATEGORY_MAP.put("일반",  Color.parseColor("#81D4FA")); // 하늘색
        CATEGORY_MAP.put("공부",  Color.parseColor("#FFF176")); // 노란색
        CATEGORY_MAP.put("운동",  Color.parseColor("#AED581")); // 초록색
        CATEGORY_MAP.put("중요",  Color.parseColor("#FFB74D")); // 주황색
    }

    private final List<TodoItem> allItems = new ArrayList<>();
    private BaseAdapter adapter;
    private int selectedPosition = -1;

    private TextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        loadItems();
        reorderItems();

        ListView listView = findViewById(R.id.listView);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        tvProgress = findViewById(R.id.tvProgress);

        int black = Color.parseColor("#000000");
        ColorStateList blackTint = ColorStateList.valueOf(black);
        for (Button b : new Button[]{btnAdd, btnEdit, btnDelete}) {
            b.setBackgroundTintList(blackTint);
            b.setTextColor(Color.WHITE);
        }

        adapter = new BaseAdapter() {
            @Override public int getCount() { return allItems.size(); }
            @Override public Object getItem(int pos) { return allItems.get(pos); }
            @Override public long getItemId(int pos) { return pos; }

            @Override public View getView(int pos, View cv, ViewGroup parent) {
                TodoItem item = allItems.get(pos);

                LinearLayout row = new LinearLayout(TodoActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(24, 24, 24, 24);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                CheckBox cb = new CheckBox(TodoActivity.this);
                cb.setChecked(item.done);
                cb.setOnCheckedChangeListener((v, isChecked) -> {
                    item.done = isChecked;
                    reorderItems();
                    adapter.notifyDataSetChanged();
                    updateProgress();
                });

                View colorDot = new View(TodoActivity.this);
                int dotSize = dp(14);
                LinearLayout.LayoutParams lpDot = new LinearLayout.LayoutParams(dotSize, dotSize);
                lpDot.setMargins(dp(12), 0, dp(12), 0);
                colorDot.setLayoutParams(lpDot);
                colorDot.setBackgroundTintList(ColorStateList.valueOf(item.catColor));
                colorDot.setBackgroundResource(android.R.drawable.presence_online);

                TextView tv = new TextView(TodoActivity.this);
                tv.setText(item.text);
                tv.setTextSize(18f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tv.setLayoutParams(lp);

                if (item.done) {
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    tv.setTextColor(Color.BLACK);
                }

                if (pos == selectedPosition)
                    row.setBackgroundColor(Color.parseColor("#FCE4EC"));
                else
                    row.setBackgroundColor(Color.TRANSPARENT);

                row.setOnClickListener(v -> {
                    selectedPosition = pos;
                    notifyDataSetChanged();
                });

                row.addView(cb);
                row.addView(colorDot);
                row.addView(tv);
                return row;
            }
        };
        listView.setAdapter(adapter);
        updateProgress();

        btnAdd.setOnClickListener(v -> showAddDialog());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> {
            if (selectedPosition < 0) return;
            allItems.remove(selectedPosition);
            selectedPosition = -1;
            adapter.notifyDataSetChanged();
            updateProgress();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveItems();
    }

    private void showAddDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        String[] cats = CATEGORY_MAP.keySet().toArray(new String[0]);
        final int[] sel = {0};

        new AlertDialog.Builder(this)
                .setTitle("할 일 추가")
                .setView(input)
                .setSingleChoiceItems(cats, 0, (d, i) -> sel[0] = i)
                .setPositiveButton("추가", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (!s.isEmpty()) {
                        String cat = cats[sel[0]];
                        int col = CATEGORY_MAP.get(cat);
                        allItems.add(new TodoItem(s, cat, col));
                        reorderItems();
                        adapter.notifyDataSetChanged();
                        updateProgress();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditDialog() {
        if (selectedPosition < 0) return;
        TodoItem item = allItems.get(selectedPosition);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(item.text);

        String[] cats = CATEGORY_MAP.keySet().toArray(new String[0]);
        int curIdx = new ArrayList<>(CATEGORY_MAP.keySet()).indexOf(item.category);
        final int[] sel = {curIdx};

        new AlertDialog.Builder(this)
                .setTitle("할 일 수정")
                .setView(input)
                .setSingleChoiceItems(cats, curIdx, (d, i) -> sel[0] = i)
                .setPositiveButton("저장", (d, w) -> {
                    item.text = input.getText().toString().trim();
                    item.category = cats[sel[0]];
                    item.catColor = CATEGORY_MAP.get(item.category);
                    reorderItems();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void reorderItems() {
        Collections.sort(allItems, (a, b) -> Boolean.compare(a.done, b.done));
    }

    private void updateProgress() {
        int total = allItems.size();
        long done = allItems.stream().filter(t -> t.done).count();
        String txt = total == 0 ? "0%" : String.format(Locale.KOREA, "%.0f%% ( %d / %d )",
                (done * 100.0 / total), done, total);
        tvProgress.setText("달성률 " + txt);
    }

    private void loadItems() {
        try {
            JSONArray arr = new JSONArray(getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(PREF_KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                TodoItem it = new TodoItem(
                        o.getString("text"),
                        o.optString("cat", "일반"),
                        o.optInt("col", CATEGORY_MAP.get("일반"))
                );
                it.done = o.getBoolean("done");
                allItems.add(it);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveItems() {
        try {
            JSONArray arr = new JSONArray();
            for (TodoItem it : allItems) {
                JSONObject o = new JSONObject();
                o.put("text", it.text);
                o.put("done", it.done);
                o.put("cat", it.category);
                o.put("col", it.catColor);
                arr.put(o);
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY, arr.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}
