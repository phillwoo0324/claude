package com.foldanim.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.View;

/**
 * 힌지 각도에 맞춰 스크린샷을 블러 + 입체(책처럼 접히는) 효과로 그리는 뷰.
 * 커버 화면: 펼칠수록 흐려지며 살짝 확대.
 * 내부 화면: 펼칠수록 선명해지고, 양쪽 절반이 평평하게 펴짐.
 */
public class FoldView extends View {

    // ---- 취향에 맞게 조절할 수 있는 값 ----
    /** 커버 화면에서 블러가 최대가 되는 힌지 각도 */
    private static final float COVER_END_ANGLE = 90f;
    /** 내부 화면에서 애니메이션이 시작되는 힌지 각도 */
    private static final float INNER_START_ANGLE = 25f;
    /** 내부 화면 양쪽 절반이 기울어지는 최대 각도 */
    private static final float MAX_TILT_DEG = 32f;
    /** 각도 변화를 따라가는 속도 (0~1, 클수록 즉각 반응) */
    private static final float FOLLOW = 0.4f;

    private Bitmap cover;
    private Bitmap inner;
    private float target = 180f;
    private float shown = 180f;
    private float maxBlur = 60f;
    private float appliedBlur = 0f;
    private boolean ticking;

    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint shadePaint = new Paint();
    private final Matrix matrix = new Matrix();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final float[] from = new float[8];
    private final float[] to = new float[8];

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            step();
        }
    };

    public FoldView(Context context) {
        super(context);
    }

    public void setCover(Bitmap b) {
        cover = b;
        invalidate();
    }

    public void setInner(Bitmap b) {
        inner = b;
        invalidate();
    }

    public void setMaxBlur(float px) {
        maxBlur = px;
        apply();
    }

    /** 힌지 각도(0 = 완전히 접힘, 180 = 완전히 펼침) */
    public void setAngle(float degrees) {
        target = Math.max(0f, Math.min(180f, degrees));
        if (!ticking) {
            ticking = true;
            postOnAnimation(tick);
        }
    }

    public boolean isCoverDisplay() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return false;
        }
        float ratio = (float) Math.min(w, h) / (float) Math.max(w, h);
        return ratio < 0.6f; // 커버 화면은 길쭉하고, 내부 화면은 정사각형에 가까움
    }

    private void step() {
        float d = target - shown;
        if (Math.abs(d) < 0.2f) {
            shown = target;
            ticking = false;
        } else {
            shown += d * FOLLOW;
            postOnAnimation(tick);
        }
        apply();
    }

    /** 0 = 효과 없음(선명), 1 = 효과 최대 */
    private float strength() {
        if (isCoverDisplay()) {
            return ease(shown / COVER_END_ANGLE);
        }
        return 1f - ease((shown - INNER_START_ANGLE) / (180f - INNER_START_ANGLE));
    }

    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    private void apply() {
        float blur = strength() * maxBlur;
        if (blur < 1f) {
            if (appliedBlur != 0f) {
                setRenderEffect(null);
                appliedBlur = 0f;
            }
        } else if (Math.abs(blur - appliedBlur) > 0.5f) {
            setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
            appliedBlur = blur;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f;
        float half = w * 0.18f;
        shadePaint.setShader(new LinearGradient(cx - half, 0f, cx + half, 0f,
                new int[] {0x00000000, 0xFF000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        apply(); // 화면(커버/내부)이 바뀌면 효과 다시 계산
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        canvas.drawColor(Color.BLACK);
        float s = strength();

        if (isCoverDisplay()) {
            float sc = 1f + 0.08f * s;
            canvas.save();
            canvas.scale(sc, sc, w / 2f, h / 2f);
            drawCropped(canvas, cover, w, h);
            canvas.restore();
            canvas.drawColor(Color.argb((int) (s * 90f), 0, 0, 0));
            return;
        }

        // 내부 화면: 가운데 접힘선을 축으로 양쪽 절반을 사다리꼴로 변형해 입체감을 줌
        double th = Math.toRadians(s * MAX_TILT_DEG);
        float cx = w / 2f;
        float inset = cx * (1f - (float) Math.cos(th));
        float grow = h * 0.05f * (float) Math.sin(th);

        set(from, 0f, 0f, cx, 0f, cx, h, 0f, h);
        set(to, inset, -grow, cx, 0f, cx, h, inset, h + grow);
        drawHalf(canvas, 0f, cx, w, h);

        set(from, cx, 0f, w, 0f, w, h, cx, h);
        set(to, cx, 0f, w - inset, -grow, w - inset, h + grow, cx, h);
        drawHalf(canvas, cx, w, w, h);

        if (s > 0.01f) {
            shadePaint.setAlpha((int) (s * 150f));
            canvas.drawRect(cx - w * 0.18f, 0f, cx + w * 0.18f, h, shadePaint);
            canvas.drawColor(Color.argb((int) (s * 100f), 0, 0, 0));
        }
    }

    private void drawHalf(Canvas canvas, float left, float right, int w, int h) {
        matrix.setPolyToPoly(from, 0, to, 0, 4);
        canvas.save();
        canvas.concat(matrix);
        canvas.clipRect(left, 0f, right, (float) h);
        drawCropped(canvas, inner, w, h);
        canvas.restore();
    }

    /** 비율을 유지한 채 화면을 꽉 채우도록(center-crop) 그림 */
    private void drawCropped(Canvas canvas, Bitmap b, int w, int h) {
        if (b == null) {
            return;
        }
        int bw = b.getWidth();
        int bh = b.getHeight();
        float viewAspect = (float) w / (float) h;
        float bmpAspect = (float) bw / (float) bh;
        if (bmpAspect > viewAspect) {
            int cw = Math.round(bh * viewAspect);
            int x = (bw - cw) / 2;
            src.set(x, 0, x + cw, bh);
        } else {
            int ch = Math.round(bw / viewAspect);
            int y = (bh - ch) / 2;
            src.set(0, y, bw, y + ch);
        }
        dst.set(0f, 0f, w, h);
        canvas.drawBitmap(b, src, dst, bmpPaint);
    }

    private static void set(float[] a, float x0, float y0, float x1, float y1,
                            float x2, float y2, float x3, float y3) {
        a[0] = x0; a[1] = y0; a[2] = x1; a[3] = y1;
        a[4] = x2; a[5] = y2; a[6] = x3; a[7] = y3;
    }
}
