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
    // 动态获取路径：与 C++ 层和 AssetExtractor 一致
    private String ROOT;
    private String CHARTS;
    static final String[] DIFFS = {"easy", "normal", "hard", "expert", "master"};
    static final int CN_MIN = 10000;
    static final String CN_BASE = "https://storage.sekai.best/sekai-cn-assets";

    static class Song {
        int id;
        String title;
        String kana;        // pronunciation
        String jacket;
        int[] levels = new int[5];
        List<String[]> vocals = new ArrayList<String[]>();
        List<String> aliases = new ArrayList<String>();
        // 小写化后的可搜索文本（title + kana + aliases 合并），只算一次
        String searchKey = "";
    }

    List<Song> songs = new ArrayList<Song>();
    List<Song> filtered = new ArrayList<Song>();  // 当前显示的子集
    List<CheckBox> boxes = new ArrayList<CheckBox>();
    LinearLayout list;
    ProgressBar bar;
    TextView status;
    Button goBtn;
    EditText searchBox;
    // 勾选状态独立于列表存在——过滤后重建列表不能丢已选
    Set<Integer> selectedIds = new HashSet<Integer>();

    // 下载选项（对应 chartdl 的 Request 结构）
    CheckBox[] diffBoxes = new CheckBox[5];
    CheckBox jacketBox;
    CheckBox sidecarBox;
    Spinner vocalSpinner;   // 0=不下, 1=只下第一个, 2=下全部

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        File extRoot = getExternalFilesDir(null);
        if (extRoot == null) extRoot = new File("/sdcard/Android/data/com.xxtsoft.cppsekai/files");
        ROOT = extRoot.getAbsolutePath();
        CHARTS = ROOT + "/charts";
        extRoot.mkdirs();
        new File(CHARTS).mkdirs();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // 搜索框：支持歌名 / 读音 / 别名，大小写无关
        searchBox = new EditText(this);
        searchBox.setHint("搜索歌名 / 读音 / 别名（支持多关键词空格分隔）");
        searchBox.setSingleLine(true);
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { applyFilter(); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });
        root.addView(searchBox);

        // ---- 下载选项 ----
        // 难度行
        LinearLayout diffRow = new LinearLayout(this);
        diffRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView diffLabel = new TextView(this);
        diffLabel.setText("难度:");
        diffLabel.setPadding(8, 8, 8, 8);
        diffRow.addView(diffLabel);
        String[] diffNames = {"EASY", "NORMAL", "HARD", "EXPERT", "MASTER"};
        for (int d = 0; d < 5; d++) {
            diffBoxes[d] = new CheckBox(this);
            diffBoxes[d].setText(diffNames[d]);
            diffBoxes[d].setChecked(true);  // 默认全选
            diffRow.addView(diffBoxes[d]);
        }
        root.addView(diffRow);

        // 封面 / sidecar / vocal 行
        LinearLayout optRow = new LinearLayout(this);
        optRow.setOrientation(LinearLayout.HORIZONTAL);
        jacketBox = new CheckBox(this);
        jacketBox.setText("封面");
        jacketBox.setChecked(true);
        sidecarBox = new CheckBox(this);
        sidecarBox.setText("元数据");
        sidecarBox.setChecked(true);
        optRow.addView(jacketBox);
        optRow.addView(sidecarBox);
        TextView vocLabel = new TextView(this);
        vocLabel.setText("  人声:");
        vocLabel.setPadding(8, 8, 4, 8);
        optRow.addView(vocLabel);
        vocalSpinner = new Spinner(this);
        ArrayAdapter<String> vocAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"不下", "首个", "全部"});
        vocalSpinner.setAdapter(vocAdapter);
        vocalSpinner.setSelection(1);  // 默认只下第一个
        optRow.addView(vocalSpinner);
        root.addView(optRow);

        LinearLayout btns = new LinearLayout(this);
        Button all = new Button(this);   all.setText("全选");
        Button none = new Button(this);  none.setText("清空");
        Button refresh = new Button(this); refresh.setText("重新扫描");
        all.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 只全选当前过滤出来的
                for (int i = 0; i < filtered.size(); i++) selectedIds.add(filtered.get(i).id);
                buildList();
            }
        });
        none.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 只清空当前过滤出来的
                for (int i = 0; i < filtered.size(); i++) selectedIds.remove(filtered.get(i).id);
                buildList();
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
        filtered.addAll(songs);
        buildList();
        scanDownloaded();
    }

    void loadData() {
        try {
            JSONArray arr = new JSONArray(readFile(ROOT + "/musics.json"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject r = arr.getJSONObject(i);
                Song s = new Song();
                s.id = r.optInt("id");
                s.title = r.optString("title");
                s.kana = r.optString("pronunciation");
                s.jacket = r.optString("assetbundleName");
                if (s.id > 0 && !s.title.isEmpty()) songs.add(s);
            }
            // 别名：music-aliases.json 格式为 {"1":["alias1","alias2",...], ...}
            try {
                JSONObject al = new JSONObject(readFile(ROOT + "/music-aliases.json"));
                for (int i = 0; i < songs.size(); i++) {
                    Song s = songs.get(i);
                    JSONArray a = al.optJSONArray(String.valueOf(s.id));
                    if (a == null) continue;
                    for (int j = 0; j < a.length(); j++) {
                        String v = a.optString(j);
                        if (!v.isEmpty()) s.aliases.add(v);
                    }
                }
            } catch (Exception ignored) {}
            // 预计算 searchKey（全部小写，空格拼接）
            for (int i = 0; i < songs.size(); i++) {
                Song s = songs.get(i);
                StringBuilder sb = new StringBuilder();
                sb.append(s.title.toLowerCase());
                if (s.kana != null && !s.kana.isEmpty()) sb.append(' ').append(s.kana.toLowerCase());
                for (int j = 0; j < s.aliases.size(); j++) {
                    sb.append(' ').append(s.aliases.get(j).toLowerCase());
                }
                s.searchKey = sb.toString();
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

    /** 根据搜索框内容重建 filtered 列表。空查询 = 显示全部。 */
    void applyFilter() {
        // 先保存当前勾选状态（已在下拉列表里的）
        saveChecked();

        String q = (searchBox == null) ? "" : searchBox.getText().toString().trim().toLowerCase();
        filtered.clear();
        if (q.isEmpty()) {
            filtered.addAll(songs);
        } else {
            // 空格分隔多关键词，全部命中才算（AND）
            String[] terms = q.split("\\s+");
            for (int i = 0; i < songs.size(); i++) {
                Song s = songs.get(i);
                boolean hit = true;
                for (int t = 0; t < terms.length; t++) {
                    if (terms[t].isEmpty()) continue;
                    if (s.searchKey.indexOf(terms[t]) < 0) { hit = false; break; }
                }
                if (hit) filtered.add(s);
            }
        }
        buildList();
    }

    /** 把当前可见的勾选状态存进 selectedIds。 */
    void saveChecked() {
        for (int i = 0; i < boxes.size(); i++) {
            CheckBox cb = boxes.get(i);
            Song s = (Song) cb.getTag();
            if (cb.isChecked()) selectedIds.add(s.id);
            else selectedIds.remove(s.id);
        }
    }

    void buildList() {
        list.removeAllViews();
        boxes.clear();
        for (int i = 0; i < filtered.size(); i++) {
            final Song s = filtered.get(i);
            CheckBox cb = new CheckBox(this);
            int total = 0;
            for (int d = 0; d < 5; d++) if (s.levels[d] > 0) total++;
            cb.setText(s.id + " " + s.title + "  [" + total + "难度]");
            cb.setTag(s);
            // 恢复勾选状态（过滤前选中的还在选）
            cb.setChecked(selectedIds.contains(s.id));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean c) {
                    Song so = (Song) v.getTag();
                    if (c) selectedIds.add(so.id);
                    else selectedIds.remove(so.id);
                    updateCount();
                }
            });
            list.addView(cb);
            boxes.add(cb);
        }
        updateCount();
    }

    void updateCount() {
        int n = selectedIds.size();
        status.setText("显示 " + filtered.size() + " / 共 " + songs.size() + " 首，已选 " + n + " 首");
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
        saveChecked();  // 保险起见再存一次
        final List<Song> sel = new ArrayList<Song>();
        for (int i = 0; i < songs.size(); i++) {
            if (selectedIds.contains(songs.get(i).id)) sel.add(songs.get(i));
        }
        if (sel.isEmpty()) {
            Toast.makeText(this, "没有选中歌曲", Toast.LENGTH_SHORT).show();
            return;
        }
        new File(CHARTS).mkdirs();

        // 读取选项
        final boolean[] wantDiff = new boolean[5];
        for (int d = 0; d < 5; d++) wantDiff[d] = diffBoxes[d].isChecked();
        final boolean wantJacket = jacketBox.isChecked();
        final boolean wantSidecar = sidecarBox.isChecked();
        final int vocalMode = vocalSpinner.getSelectedItemPosition();  // 0=不下,1=首个,2=全部

        final List<Runnable> tasks = new ArrayList<Runnable>();
        for (int si = 0; si < sel.size(); si++) {
            final Song s = sel.get(si);
            for (int d = 0; d < 5; d++) {
                if (s.levels[d] <= 0) continue;
                if (!wantDiff[d]) continue;
                final String df = DIFFS[d];
                tasks.add(new Runnable() {
                    public void run() {
                        dl(chartUrl(s, df), new File(CHARTS, id4(s.id) + "_" + df + ".sus"));
                    }
                });
            }
            if (vocalMode > 0 && !s.vocals.isEmpty()) {
                int count = (vocalMode == 2) ? s.vocals.size() : 1;
                for (int vi = 0; vi < count; vi++) {
                    final String asset = s.vocals.get(vi)[0];
                    tasks.add(new Runnable() {
                        public void run() {
                            dl(audioUrl(s, asset), new File(CHARTS, asset + ".mp3"));
                        }
                    });
                }
            }
            if (wantJacket) {
                tasks.add(new Runnable() {
                    public void run() {
                        dl(jacketUrl(s), new File(CHARTS, id4(s.id) + ".png"));
                    }
                });
            }
            if (wantSidecar) {
                tasks.add(new Runnable() {
                    public void run() { writeSidecar(s); }
                });
            }
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
