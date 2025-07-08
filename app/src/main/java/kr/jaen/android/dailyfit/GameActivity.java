package kr.jaen.android.dailyfit;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity_DailyFit";
    private static final int ROUND_TIME = 10;

    private FrameLayout frameLayout;
    private ImageView[] imageViews;
    private TextView tvTime, tvLives, tvProgress;

    private int screenWidth, screenHeight;
    private final int MIN_SIZE = 100;
    private final int MAX_SIZE = 250;
    private final Random random = new Random();

    private int level, howManyMoles, count;
    private int gameSpeed;
    private int lives = 3;

    private boolean threadEndFlag;
    private MoleTask moleTask;
    private CountDownTimer roundTimer;

    private SoundPool soundPool;
    private int hitSound;
    private MediaPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 🔹 툴바를 ActionBar로 지정하고 Bold 스타일 적용
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            SpannableString title = new SpannableString("두더지 게임");
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setSpan(new RelativeSizeSpan(0.85f), 0, title.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(title);
        }

        frameLayout = findViewById(R.id.frame);
        frameLayout.setBackgroundResource(R.drawable.ic_grass);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attrs)
                .build();
        hitSound = soundPool.load(this, R.raw.mouse_scream, 1);

        bgmPlayer = MediaPlayer.create(this, R.raw.bgm);
        bgmPlayer.setLooping(true);

        initOverlayTexts();

        level = 1;
        howManyMoles = 5;
        startRound();
    }

    private void initOverlayTexts() {
        tvTime = new TextView(this);
        tvLives = new TextView(this);
        tvProgress = new TextView(this);

        for (TextView tv : new TextView[]{tvTime, tvLives, tvProgress}) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }

        FrameLayout.LayoutParams lpTime = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpTime.gravity = Gravity.START | Gravity.TOP;
        lpTime.leftMargin = dp(16);
        lpTime.topMargin = dp(12);

        FrameLayout.LayoutParams lpLives = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpLives.gravity = Gravity.END | Gravity.TOP;
        lpLives.rightMargin = dp(16);
        lpLives.topMargin = dp(12);

        FrameLayout.LayoutParams lpProg = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpProg.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        lpProg.topMargin = dp(12);

        frameLayout.addView(tvTime, lpTime);
        frameLayout.addView(tvLives, lpLives);
        frameLayout.addView(tvProgress, lpProg);

        updateLivesUI();
        updateTimeUI(ROUND_TIME);
        updateProgressUI();
    }

    private void startRound() {
        count = 0;
        threadEndFlag = true;
        gameSpeed = (int) (1000 * (10 - Math.min(level, 9)) / 10.0);
        updateProgressUI();
        updateTimeUI(ROUND_TIME);

        frameLayout.post(() -> {
            frameLayout.removeViews(3, frameLayout.getChildCount() - 3);
            imageViews = new ImageView[howManyMoles];
            for (int i = 0; i < howManyMoles; i++) {
                ImageView iv = new ImageView(this);
                iv.setImageResource(R.drawable.mole);
                iv.setOnClickListener(moleClickListener);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MIN_SIZE, MIN_SIZE);
                frameLayout.addView(iv, lp);
                imageViews[i] = iv;
            }
            moleTask = new MoleTask();
            moleTask.execute();
        });

        roundTimer = new CountDownTimer(ROUND_TIME * 1000L, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                updateTimeUI((int) (millisUntilFinished / 1000));
            }

            @Override public void onFinish() {
                updateTimeUI(0);
                onRoundTimeOver();
            }
        }.start();
    }

    private final View.OnClickListener moleClickListener = v -> {
        count++;
        soundPool.play(hitSound, 1, 1, 0, 0, 1);
        v.setVisibility(View.INVISIBLE);
        updateProgressUI();

        if (count >= howManyMoles) {
            cancelTasks();
            new AlertDialog.Builder(GameActivity.this)
                    .setMessage("라운드 성공! 다음 레벨로 갈까요?")
                    .setPositiveButton("네", (d, w) -> {
                        level++;
                        howManyMoles++;
                        startRound();
                    })
                    .setNegativeButton("아니오", (d, w) -> finish())
                    .show();
        }
    };

    private void onRoundTimeOver() {
        cancelTasks();
        lives--;
        updateLivesUI();

        if (lives > 0) {
            Toast.makeText(this, "시간 초과! 남은 목숨: " + lives, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::startRound, 300);
        } else {
            new AlertDialog.Builder(GameActivity.this)
                    .setTitle("Game Over")
                    .setMessage("모든 목숨을 잃었습니다. 메인으로 돌아갈까요?")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private void updatePositions() {
        if (!threadEndFlag) return;

        for (ImageView img : imageViews) {
            int size = random.nextInt(MAX_SIZE - MIN_SIZE + 1) + MIN_SIZE;
            int x = random.nextInt(screenWidth - size);
            int y = random.nextInt(screenHeight - size);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) img.getLayoutParams();
            lp.width = size;
            lp.height = size;
            lp.leftMargin = x;
            lp.topMargin = y;
            img.setLayoutParams(lp);
            img.setVisibility(View.VISIBLE);
        }
    }

    private class MoleTask extends AsyncTask<Void, Void, Void> {
        @Override protected Void doInBackground(Void... p) {
            while (threadEndFlag) {
                publishProgress();
                try { Thread.sleep(gameSpeed); } catch (InterruptedException ignored) {}
            }
            return null;
        }
        @Override protected void onProgressUpdate(Void... p) { updatePositions(); }
    }

    private void updateTimeUI(int sec) {
        tvTime.setText(String.format(Locale.KOREA, "⏱ %02d:%02d", sec / 60, sec % 60));
    }
    private void updateLivesUI() { tvLives.setText("❤️ " + lives); }
    private void updateProgressUI() {
        tvProgress.setText(String.format(Locale.KOREA, "두더지 %d / %d", count, howManyMoles));
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void cancelTasks() {
        if (moleTask != null) {
            threadEndFlag = false;
            moleTask.cancel(true);
            moleTask = null;
        }
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (bgmPlayer != null) bgmPlayer.start();
    }
    @Override protected void onPause() {
        super.onPause();
        if (bgmPlayer != null && bgmPlayer.isPlaying()) bgmPlayer.pause();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
        }
        cancelTasks();
    }
}
