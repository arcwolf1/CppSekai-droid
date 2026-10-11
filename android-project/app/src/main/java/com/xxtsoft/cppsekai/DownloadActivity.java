package com.xxtsoft.cppsekai;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadActivity extends Activity {
    static final String ROOT   = "/sdcard/CppSekai";
    static final String CHARTS = ROOT + "/charts";
    static final String[] DIFFS = {"easy", "normal", "hard", "expert", "master"};
    static final int CN_MIN = 10000;
    static final String CN_BASE = "https://storage.sekai.best/sekai-cn-assets";

    static class Song {
        int id;
        String title;
        String jacket;
        int[] levels = new int[5];
        List<String[]> vocals = new ArrayList<String[]>();
    }

    List<Song> songs = new ArrayList<Song>();
    List<CheckBox> boxes = new ArrayList<CheckBox>();
    LinearLayout list;
    ProgressBar bar;
    TextView status;
    Button goBtn;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout btns = new LinearLayout(this);
        Button all = new Button(this);   all.setText("全选");
        Button none = new Button(this);  none.setText("清空");
        Button refresh = new Button(this); refresh.setText("重新扫描");
        all.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < boxes.size(); i++) boxes.get(i).setChecked(true);
            }
        });
        none.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < boxes.size(); i++) boxes.get(i).setChecked(false);
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanDownloaded(); }
        });
        btns.addView(all); btns.addView(none); btns.addView(refresh);
        root.addView(btns);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        root.addView(bar);

        status = new TextView(this);
        status.setText("准备中...");
        root.addView(status);

        goBtn = new Button(this);
        goBtn.setText("开始下载");
        goBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startDownload(); }
        });
        root.addView(goBtn);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv);

        setContentView(root);

        loadData();
        buildList();
        scanDownloaded();
        status.setText("共 " + songs.size() + " 首，已选 0 首");
    }

    void loadData() {
        try {
            JSONArray arr = new JSONArray(readFile(ROOT + "/musics.json"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject r = arr.getJSONObject(i);
                Song s = new Song();
                s.id = r.optInt("id");
                s.title = r.optString("title");
                s.jacket = r.optString("assetbundleName");
                if (s.id > 0 && !s.title.isEmpty()) songs.add(s);
            }
            try {
                JSONObject v = new JSONObject(readFile(ROOT + "/music-vocals.json"));
                for (int i = 0; i < songs.size(); i++) {
                    Song s = songs.get(i);
                    JSONArray a = v.optJSONArray(String.valueOf(s.id));
                    if (a == null) continue;
                    for (int j = 0; j < a.length(); j++) {
                        JSONObject r = a.getJSONObject(j);
                        String asset = r.optString("asset");
                        if (!asset.isEmpty()) {
                            s.vocals.add(new String[]{asset, r.optString("caption")});
                        }
                    }
                }
            } catch (Exception ignored) {}
            try {
                JSONObject lv = new JSONObject(readFile(ROOT + "/music-levels.json"));
                for (int i = 0; i < songs.size(); i++) {
                    Song s = songs.get(i);
                    JSONArray a = lv.optJSONArray(String.valueOf(s.id));
                    if (a == null) continue;
                    for (int d = 0; d < 5 && d < a.length(); d++) s.levels[d] = a.optInt(d, 0);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            status.setText("读数据失败: " + e.getMessage());
        }
    }

    void buildList() {
        list.removeAllViews();
        boxes.clear();
        for (int i = 0; i < songs.size(); i++) {
            final Song s = songs.get(i);
            CheckBox cb = new CheckBox(this);
            int total = 0;
            for (int d = 0; d < 5; d++) if (s.levels[d] > 0) total++;
            cb.setText(s.id + " " + s.title + "  [" + total + "难度]");
            cb.setTag(s);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean c) { updateCount(); }
            });
            list.addView(cb);
            boxes.add(cb);
        }
    }

    void updateCount() {
        int n = 0;
        for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isChecked()) n++;
        status.setText("共 " + songs.size() + " 首，已选 " + n + " 首");
    }

    void scanDownloaded() {
        Set<String> have = new HashSet<String>();
        File[] fs = new File(CHARTS).listFiles();
        if (fs != null) for (int i = 0; i < fs.length; i++) have.add(fs[i].getName());
        for (int i = 0; i < songs.size(); i++) {
            Song s = songs.get(i);
            CheckBox cb = boxes.get(i);
            String base = cb.getText().toString().replace(" [已下载]", "");
            String tag = have.contains(String.format("%04d.json", s.id)) ? " [已下载]" : "";
            cb.setText(base + tag);
        }
    }

    void startDownload() {
        final List<Song> sel = new ArrayList<Song>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isChecked()) sel.add((Song) boxes.get(i).getTag());
        }
        if (sel.isEmpty()) {
            Toast.makeText(this, "没有选中歌曲", Toast.LENGTH_SHORT).show();
            return;
        }
        new File(CHARTS).mkdirs();

        final List<Runnable> tasks = new ArrayList<Runnable>();
        for (int si = 0; si < sel.size(); si++) {
            final Song s = sel.get(si);
            for (int d = 0; d < 5; d++) {
                if (s.levels[d] <= 0) continue;
                final String df = DIFFS[d];
                tasks.add(new Runnable() {
                    public void run() {
                        dl(chartUrl(s, df), new File(CHARTS, id4(s.id) + "_" + df + ".sus"));
                    }
                });
            }
            if (!s.vocals.isEmpty()) {
                final String asset = s.vocals.get(0)[0];
                tasks.add(new Runnable() {
                    public void run() {
                        dl(audioUrl(s, asset), new File(CHARTS, asset + ".mp3"));
                    }
                });
            }
            tasks.add(new Runnable() {
                public void run() {
                    dl(jacketUrl(s), new File(CHARTS, id4(s.id) + ".png"));
                }
            });
            tasks.add(new Runnable() {
                public void run() { writeSidecar(s); }
            });
        }

        final AtomicInteger done = new AtomicInteger();
        final int total = tasks.size();
        goBtn.setEnabled(false);

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < tasks.size(); i++) {
            final Runnable t = tasks.get(i);
            pool.submit(new Runnable() {
                public void run() {
                    try { t.run(); } catch (Exception ignored) {}
                    final int n = done.incrementAndGet();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            status.setText(n + " / " + total);
                            bar.setProgress(n * 100 / Math.max(1, total));
                        }
                    });
                }
            });
        }
        pool.shutdown();
        new Thread(new Runnable() {
            public void run() {
                try { pool.awaitTermination(1, TimeUnit.DAYS); } catch (Exception ignored) {}
                runOnUiThread(new Runnable() {
                    public void run() {
                        status.setText("完成: " + done.get() + " / " + total);
                        goBtn.setEnabled(true);
                        scanDownloaded();
                    }
                });
            }
        }).start();
    }

    static String id4(int id) { return String.format("%04d", id); }

    String chartUrl(Song s, String diff) {
        if (s.id >= CN_MIN)
            return CN_BASE + "/music/music_score/" + id4(s.id) + "_01/" + diff + ".txt";
        return "https://assets.unipjsk.com/startapp/music/music_score/" + id4(s.id) + "_01/" + diff;
    }

    String audioUrl(Song s, String asset) {
        if (s.id >= CN_MIN)
            return CN_BASE + "/music/long/" + asset + "/" + asset + ".mp3";
        return "https://assets.unipjsk.com/ondemand/music/long/" + asset + "/" + asset + ".mp3";
    }

    String jacketUrl(Song s) {
        String stem = s.jacket.isEmpty() ? id4(s.id) + "_01" : s.jacket;
        if (s.id >= CN_MIN)
            return CN_BASE + "/music/jacket/" + stem + "/" + stem + ".png";
        return "https://assets.unipjsk.com/startapp/music/jacket/" + stem + "/" + stem + ".png";
    }

    void dl(String url, File out) {
        if (out.exists() && out.length() > 0) return;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            int code = c.getResponseCode();
            if (code != 200) { c.disconnect(); return; }
            File part = new File(out.getParentFile(), out.getName() + ".part");
            FileOutputStream f = new FileOutputStream(part);
            InputStream in = c.getInputStream();
            byte[] buf = new byte[128 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) f.write(buf, 0, n);
            in.close();
            f.close();
            c.disconnect();
            out.delete();
            part.renameTo(out);
        } catch (Exception ignored) {}
    }

    void writeSidecar(Song s) {
        try {
            File out = new File(CHARTS, id4(s.id) + ".json");
            if (out.exists()) return;
            JSONObject d = new JSONObject();
            d.put("title", s.title);
            Writer w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
            w.write(d.toString(2));
            w.close();
        } catch (Exception ignored) {}
    }

    String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        return sb.toString();
    }
}
