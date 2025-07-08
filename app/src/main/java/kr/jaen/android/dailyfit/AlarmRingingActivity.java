package kr.jaen.android.dailyfit;

import android.app.Activity;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmRingingActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ringing);

        TextView text = findViewById(R.id.txtAlarm);
        text.setText("NFC 태그를 터치해 알람을 끄세요!");

        // 알람 소리 재생
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // NFC 어댑터
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null)
            nfcAdapter.enableReaderMode(this, this::onTagDiscovered, NfcAdapter.FLAG_READER_NFC_A, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableReaderMode(this);
    }

    private void onTagDiscovered(Tag tag) {
        runOnUiThread(() -> {
            Toast.makeText(this, "✅ NFC 태그 인식됨! 알람 종료", Toast.LENGTH_SHORT).show();
            stopAlarm();
            finish();
        });
    }

    private void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
