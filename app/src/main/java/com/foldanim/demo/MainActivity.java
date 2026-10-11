package com.foldanim.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int REQ_COVER = 1;
    private static final int REQ_INNER = 2;
    private static final String FILE_COVER = "cover.img";
    private static final String FILE_INNER = "inner.img";

    private FoldView foldView;
    private LinearLayout hud;
    private TextView status;
    private SensorManager sensorManager;
    private Sensor hinge;
    private float lastAngle = 180f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        foldView = new FoldView(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(foldView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        buildHud(root);
        setContentView(root);

        // 전체 화면 (상태바/내비게이션 바 숨김, 펀치홀 영역까지 사용)
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        foldView.setCover(loadOrPlaceholder(FILE_COVER, 540, 1260, "커버 화면", 0xFF1B2A6B, 0xFF7B2FF7));
        foldView.setInner(loadOrPlaceholder(FILE_INNER, 984, 1092, "내부 화면", 0xFF0F4C5C, 0xFFE36414));

        foldView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hud.setVisibility(hud.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        hinge = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE);
        updateStatus();
    }

    private void buildHud(FrameLayout root) {
        int pad = dp(16);
        hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setBackgroundColor(0xCC000000);
        hud.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15f);
        hud.addView(status);

        Button pickCover = new Button(this);
        pickCover.setText("커버 화면 스크린샷 선택");
        pickCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(REQ_COVER);
            }
        });
        hud.addView(pickCover);

        Button pickInner = new Button(this);
        pickInner.setText("내부 화면 스크린샷 선택");
        pickInner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(REQ_INNER);
            }
        });
        hud.addView(pickInner);

        hud.addView(label("블러 세기"));
        SeekBar blur = new SeekBar(this);
        blur.setMax(150);
        blur.setProgress(60);
        blur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                foldView.setMaxBlur(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        hud.addView(blur);

        hud.addView(label("수동 각도 (센서 없이 미리보기)"));
        SeekBar manual = new SeekBar(this);
        manual.setMax(180);
        manual.setProgress(180);
        manual.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    lastAngle = progress;
                    foldView.setAngle(progress);
                    updateStatus();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        hud.addView(manual);

        hud.addView(label("화면을 탭하면 이 메뉴가 숨겨집니다."));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        root.addView(hud, lp);
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFCCCCCC);
        t.setTextSize(13f);
        t.setPadding(0, dp(10), 0, 0);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void updateStatus() {
        String sensor = hinge == null ? "힌지 센서를 찾지 못했습니다 (수동 각도로 미리보기)"
                : "힌지 각도: " + Math.round(lastAngle) + "°";
        status.setText(sensor + "  ·  " + (foldView.isCoverDisplay() ? "커버 화면" : "내부 화면"));
    }

    // ---- 힌지 센서 ----

    @Override
    protected void onResume() {
        super.onResume();
        if (hinge != null) {
            sensorManager.registerListener(this, hinge, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        lastAngle = event.values[0];
        foldView.setAngle(lastAngle);
        updateStatus();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // ---- 스크린샷 선택 / 저장 / 불러오기 ----

    private void pick(int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        String name = requestCode == REQ_COVER ? FILE_COVER : FILE_INNER;
        if (!copyToFile(data.getData(), name)) {
            return;
        }
        Bitmap b = decode(new File(getFilesDir(), name));
        if (b == null) {
            return;
        }
        if (requestCode == REQ_COVER) {
            foldView.setCover(b);
        } else {
            foldView.setInner(b);
        }
    }

    private boolean copyToFile(Uri uri, String name) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(new File(getFilesDir(), name))) {
            if (in == null) {
                return false;
            }
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Bitmap decode(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > 3000) {
            sample *= 2;
        }
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    private Bitmap loadOrPlaceholder(String name, int w, int h, String text, int c1, int c2) {
        File f = new File(getFilesDir(), name);
        if (f.exists()) {
            Bitmap b = decode(f);
            if (b != null) {
                return b;
            }
        }
        // 스크린샷을 고르기 전에 보여줄 가짜 홈 화면
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0f, 0f, w, h, c1, c2, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, w, h, p);
        p.setShader(null);
        int cols = w > h * 0.6f ? 6 : 4;
        float cell = (float) w / cols;
        float icon = cell * 0.56f;
        float top = h * 0.30f;
        for (int row = 0; top + row * cell * 1.15f + icon < h * 0.95f; row++) {
            for (int col = 0; col < cols; col++) {
                float x = col * cell + (cell - icon) / 2f;
                float y = top + row * cell * 1.15f;
                p.setColor(Color.HSVToColor(230, new float[] {(row * 47 + col * 71) % 360, 0.55f, 0.95f}));
                c.drawRoundRect(x, y, x + icon, y + icon, icon * 0.26f, icon * 0.26f, p);
            }
        }
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(w * 0.09f);
        c.drawText(text, w / 2f, h * 0.18f, p);
        return b;
    }
}
