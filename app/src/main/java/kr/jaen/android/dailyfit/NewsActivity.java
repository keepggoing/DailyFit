// NewsActivity.java
package kr.jaen.android.dailyfit;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.res.ColorStateList;

public class NewsActivity extends AppCompatActivity {
    private static final String TAG = "NewsActivity";

    /*** ▼ 추가: 세 개 탭 모두 다룰 수 있도록 변수 하나 더 ***/
    private Button btnTab1, btnTab2, btnTab3;

    private Map<String, String> feeds = new LinkedHashMap<>();
    private List<String> titles = new ArrayList<>();
    private List<String> links  = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // ───────────── 버튼 & 리스트뷰 ─────────────
        btnTab1  = findViewById(R.id.btnTab1);
        btnTab2  = findViewById(R.id.btnTab2);
        btnTab3  = findViewById(R.id.btnTab3);   // ▲ 레이아웃에 btnTab3 추가해 주세요
        listView = findViewById(R.id.listView);

        // ───────────── RSS 피드 매핑 ─────────────
        feeds.put("한겨레" , "https://www.hani.co.kr/rss/");
        feeds.put("아이뉴스", "https://www.inews24.com/rss/news_it.xml");
        feeds.put("전자신문", "https://rss.etnews.com/Section901.xml");   // ▲ 새 피드

        // ───────────── 어댑터 ─────────────
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);

        // ───────────── 탭 클릭 리스너 ─────────────
        btnTab1.setOnClickListener(v -> { selectTab(btnTab1); loadFeed("한겨레");  });
        btnTab2.setOnClickListener(v -> { selectTab(btnTab2); loadFeed("아이뉴스"); });
        btnTab3.setOnClickListener(v -> { selectTab(btnTab3); loadFeed("전자신문"); });

        // ───────────── 리스트 클릭 (브라우저) ─────────────
        listView.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < links.size())
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(links.get(pos))));
        });

        // 첫 번째 탭 선택 & 로드
        selectTab(btnTab1);
        loadFeed("한겨레");
    }

    /* --------------------------------------------------------------
       탭 버튼 색상 토글 ―─ 눌린 버튼만 회색, 나머지는 흰색
       -------------------------------------------------------------- */
    private void selectTab(Button active) {
        int selColor  = 0xFFEEEEEE;
        int idleColor = 0xFFFFFFFF;
        Button[] btns = {btnTab1, btnTab2, btnTab3};
        for (Button b : btns) {
            if (b == null) continue;
            b.setBackgroundTintList(ColorStateList.valueOf(b == active ? selColor : idleColor));
        }
    }

    /* --------------------------------------------------------------
       피드 로딩
       -------------------------------------------------------------- */
    private void loadFeed(String name) {
        String url = feeds.get(name);
        titles.clear();
        links.clear();
        adapter.notifyDataSetChanged();
        new FetchRssTask().execute(url);
    }

    /* --------------------------------------------------------------
       AsyncTask 로 RSS 가져오기
       -------------------------------------------------------------- */
    private class FetchRssTask extends AsyncTask<String,Void,Boolean>{
        @Override protected Boolean doInBackground(String... urls){
            try{
                URL u=new URL(urls[0]);
                HttpURLConnection conn=(HttpURLConnection)u.openConnection();
                conn.setRequestProperty("User-Agent","Mozilla/5.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                InputStream is=conn.getInputStream();

                XmlPullParserFactory f=XmlPullParserFactory.newInstance();
                f.setNamespaceAware(false);
                XmlPullParser xpp=f.newPullParser();
                xpp.setInput(is,"UTF-8");

                boolean insideItem=false;
                for(int e=xpp.getEventType();e!=XmlPullParser.END_DOCUMENT;e=xpp.next()){
                    if(e==XmlPullParser.START_TAG){
                        String tag=xpp.getName();
                        if("item".equalsIgnoreCase(tag)) insideItem=true;
                        else if(insideItem&&"title".equalsIgnoreCase(tag)) titles.add(xpp.nextText());
                        else if(insideItem&&"link".equalsIgnoreCase(tag))  links .add(xpp.nextText());
                    }else if(e==XmlPullParser.END_TAG&&"item".equalsIgnoreCase(xpp.getName())){
                        insideItem=false;
                    }
                }
                is.close();
                return true;
            }catch(Exception ex){
                Log.e(TAG,"RSS fetch error",ex);
                return false;
            }
        }
        @Override protected void onPostExecute(Boolean ok){
            if(!ok||titles.isEmpty()){
                titles.clear(); links.clear();
                titles.add("뉴스를 불러오는데 실패했습니다.");
            }
            adapter.notifyDataSetChanged();
        }
    }
}
