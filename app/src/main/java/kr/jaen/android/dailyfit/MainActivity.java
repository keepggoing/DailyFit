package kr.jaen.android.dailyfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private Button btnTodo;
    private Button btnNews;
    private Button btnAlarm;
    private Button btnGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 기본 액션바 제목, 로고 숨기기 (필수!)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayUseLogoEnabled(false);
        }


        btnTodo  = findViewById(R.id.btnTodo);
        btnNews  = findViewById(R.id.btnNews);
        btnAlarm = findViewById(R.id.btnAlarm);
        btnGame  = findViewById(R.id.btnGame);

        btnTodo.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TodoActivity.class))
        );

        btnNews.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NewsActivity.class))
        );

        btnAlarm.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AlarmActivity.class));
        });

        btnGame.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GameActivity.class))
        );
    }
}
