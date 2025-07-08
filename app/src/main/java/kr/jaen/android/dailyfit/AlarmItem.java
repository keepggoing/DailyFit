package kr.jaen.android.dailyfit;

public class AlarmItem {
    public int reqCode;
    public long timeMillis;
    public boolean isEnabled;
    public String displayTime;

    public AlarmItem(int reqCode, long timeMillis, boolean isEnabled, String displayTime) {
        this.reqCode = reqCode;
        this.timeMillis = timeMillis;
        this.isEnabled = isEnabled;
        this.displayTime = displayTime;
    }
}
