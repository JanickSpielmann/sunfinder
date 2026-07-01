package it.spielmann.sunfinder;

import android.content.Context;
import androidx.annotation.NonNull;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SunCompassView extends View {

    private double sunAzimuth = 135;
    private double sunElevation = 45;
    private double vehicleHeading = 0;

    private final Paint bgPaint = new Paint();
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vehiclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vehicleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fwdLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SunCompassView(Context context) {
        super(context);
        init();
    }

    public SunCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SunCompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        bgPaint.setColor(Color.parseColor("#1B2631"));
        bgPaint.setStyle(Paint.Style.FILL);

        ringPaint.setColor(Color.parseColor("#5D6D7E"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);

        tickPaint.setColor(Color.parseColor("#7F8C8D"));
        tickPaint.setStyle(Paint.Style.STROKE);

        sunPaint.setColor(Color.parseColor("#F4D03F"));
        sunPaint.setStyle(Paint.Style.FILL);

        sunGlowPaint.setColor(Color.parseColor("#40F4D03F"));
        sunGlowPaint.setStyle(Paint.Style.FILL);

        sunLinePaint.setColor(Color.parseColor("#60F4D03F"));
        sunLinePaint.setStyle(Paint.Style.STROKE);
        sunLinePaint.setPathEffect(new DashPathEffect(new float[]{12, 8}, 0));

        vehiclePaint.setColor(Color.parseColor("#85C1E9"));
        vehiclePaint.setStyle(Paint.Style.FILL);

        vehicleBorderPaint.setColor(Color.parseColor("#2980B9"));
        vehicleBorderPaint.setStyle(Paint.Style.STROKE);

        windowPaint.setColor(Color.parseColor("#1A5276"));
        windowPaint.setStyle(Paint.Style.FILL);

        arrowPaint.setColor(Color.parseColor("#E74C3C"));
        arrowPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(Color.parseColor("#70000000"));
        shadowPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(Color.parseColor("#ECF0F1"));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        fwdLabelPaint.setColor(Color.parseColor("#E74C3C"));
        fwdLabelPaint.setFakeBoldText(true);
        fwdLabelPaint.setTextAlign(Paint.Align.CENTER);

        dimLabelPaint.setColor(Color.parseColor("#85C1E9"));
        dimLabelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSunPosition(double azimuth, double elevation) {
        this.sunAzimuth = azimuth;
        this.sunElevation = elevation;
        invalidate();
    }

    public void setVehicleHeading(double heading) {
        this.vehicleHeading = heading;
        invalidate();
    }

    // Sun azimuth relative to vehicle heading (0 = in front, 90 = right, 180 = behind, 270 = left)
    private double getRelativeAzimuth() {
        return (sunAzimuth - vehicleHeading + 360) % 360;
    }

    public ShadeSide getShadeSide() {
        double rel = getRelativeAzimuth();
        if (rel < 5 || rel > 355) return ShadeSide.NEITHER;
        if (rel > 175 && rel < 185) return ShadeSide.NEITHER;
        if (rel < 180) return ShadeSide.LEFT;
        return ShadeSide.RIGHT;
    }

    public enum ShadeSide {LEFT, RIGHT, NEITHER}

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.82f;

        drawBackground(canvas, cx, cy, radius);
        drawCompassRing(canvas, cx, cy, radius);
        drawSunLine(canvas, cx, cy, radius);
        drawGroundShadow(canvas, cx, cy, radius * 0.28f, radius * 0.16f, radius);
        drawVehicle(canvas, cx, cy, radius * 0.28f, radius * 0.16f);
        drawSun(canvas, cx, cy, radius);
    }

    private void drawBackground(Canvas canvas, float cx, float cy, float radius) {
        canvas.drawCircle(cx, cy, radius + 50, bgPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);
    }

    private void drawCompassRing(Canvas canvas, float cx, float cy, float radius) {
        float textSize = radius * 0.13f;
        labelPaint.setTextSize(textSize);
        fwdLabelPaint.setTextSize(textSize);
        dimLabelPaint.setTextSize(textSize * 0.8f);

        // Tick marks
        for (int deg = 0; deg < 360; deg += 5) {
            float rad = (float) Math.toRadians(deg - 90);
            float innerR;
            float strokeW;
            if (deg % 90 == 0) {
                innerR = radius - radius * 0.10f;
                strokeW = 2.5f;
            } else if (deg % 45 == 0) {
                innerR = radius - radius * 0.08f;
                strokeW = 2f;
            } else if (deg % 15 == 0) {
                innerR = radius - radius * 0.05f;
                strokeW = 1.5f;
            } else {
                innerR = radius - radius * 0.03f;
                strokeW = 1f;
            }
            tickPaint.setStrokeWidth(strokeW);
            canvas.drawLine(
                    cx + (float) Math.cos(rad) * radius,
                    cy + (float) Math.sin(rad) * radius,
                    cx + (float) Math.cos(rad) * innerR,
                    cy + (float) Math.sin(rad) * innerR,
                    tickPaint);
        }

        // Labels: vehicle-relative directions
        // 0° = top = Vorne, 90° = right = Rechts, 180° = bottom = Hinten, 270° = left = Links
        float labelRadius = radius + textSize * 1.1f;
        float textOffset = textSize * 0.35f;

        // Vorne (top)
        canvas.drawText("VORNE", cx, cy - labelRadius + textOffset, fwdLabelPaint);
        // Hinten (bottom)
        canvas.drawText("HINTEN", cx, cy + labelRadius + textOffset, dimLabelPaint);
        // Rechts (right)
        canvas.drawText("R", cx + labelRadius, cy + textOffset, labelPaint);
        // Links (left)
        canvas.drawText("L", cx - labelRadius, cy + textOffset, labelPaint);
    }

    private void drawSunLine(Canvas canvas, float cx, float cy, float radius) {
        float[] pos = getSunXY(cx, cy, radius);
        sunLinePaint.setStrokeWidth(2f);
        canvas.drawLine(cx, cy, pos[0], pos[1], sunLinePaint);
    }

    private void drawSun(Canvas canvas, float cx, float cy, float radius) {
        float[] pos = getSunXY(cx, cy, radius);
        float sx = pos[0], sy = pos[1];
        float sunR = radius * 0.09f;

        canvas.drawCircle(sx, sy, sunR * 2.2f, sunGlowPaint);
        canvas.drawCircle(sx, sy, sunR, sunPaint);

        Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rayPaint.setColor(Color.parseColor("#F4D03F"));
        rayPaint.setStyle(Paint.Style.STROKE);
        rayPaint.setStrokeWidth(1.5f);
        for (int i = 0; i < 8; i++) {
            float a = (float) Math.toRadians(i * 45);
            canvas.drawLine(
                    sx + (float) Math.cos(a) * (sunR + 3),
                    sy + (float) Math.sin(a) * (sunR + 3),
                    sx + (float) Math.cos(a) * (sunR + 8),
                    sy + (float) Math.sin(a) * (sunR + 8),
                    rayPaint);
        }
    }

    // Sun position uses relative azimuth so the bus stays fixed and sun rotates
    private float[] getSunXY(float cx, float cy, float radius) {
        double relAz = getRelativeAzimuth();
        float canvasAngle = (float) Math.toRadians(relAz - 90);
        float dist = (float) (radius * 0.82f * (1.0 - Math.max(0, sunElevation) / 90.0));
        dist = Math.min(dist, radius * 0.82f);
        return new float[]{
                cx + (float) Math.cos(canvasAngle) * dist,
                cy + (float) Math.sin(canvasAngle) * dist
        };
    }

    // Vehicle always points up — no rotation based on heading
    private void drawVehicle(Canvas canvas, float cx, float cy, float halfLen, float halfW) {
        double relAngle = getRelativeAzimuth();

        RectF body = new RectF(cx - halfW, cy - halfLen, cx + halfW, cy + halfLen);
        float corner = halfW * 0.2f;

        // 1. Vehicle body
        vehicleBorderPaint.setStrokeWidth(halfW * 0.08f);
        canvas.drawRoundRect(body, corner, corner, vehiclePaint);

        // 2. Windows
        float wh = halfLen * 0.22f;
        float gap = halfLen * 0.38f;
        for (int row = 0; row < 2; row++) {
            float wy = cy - halfLen * 0.55f + row * gap;
            canvas.drawRoundRect(new RectF(cx - halfW + halfW * 0.12f, wy, cx - halfW * 0.12f, wy + wh), 3, 3, windowPaint);
            canvas.drawRoundRect(new RectF(cx + halfW * 0.12f, wy, cx + halfW - halfW * 0.12f, wy + wh), 3, 3, windowPaint);
        }

        // 3. Shade overlay drawn ON TOP, clipped to vehicle shape
        //    Apply shade per face: left/right from X component, front/rear from Y component
        {
            float sunCanvasAngle = (float) Math.toRadians(relAngle - 90);
            float sunDx = (float) Math.cos(sunCanvasAngle); // >0 = sun to right
            float sunDy = (float) Math.sin(sunCanvasAngle); // >0 = sun behind (canvas Y-down)
            float t = 0.15f; // threshold to avoid shading at near-perpendicular angles

            canvas.save();
            Path clipPath = new Path();
            clipPath.addRoundRect(body, corner, corner, Path.Direction.CW);
            canvas.clipPath(clipPath);

            Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shadePaint.setColor(Color.parseColor("#80000000"));
            shadePaint.setStyle(Paint.Style.FILL);

            if (sunDx > t)  canvas.drawRect(cx - halfW, cy - halfLen, cx,       cy + halfLen, shadePaint); // left half
            if (sunDx < -t) canvas.drawRect(cx,         cy - halfLen, cx + halfW, cy + halfLen, shadePaint); // right half
            if (sunDy > t)  canvas.drawRect(cx - halfW, cy - halfLen, cx + halfW, cy,           shadePaint); // top/front half
            if (sunDy < -t) canvas.drawRect(cx - halfW, cy,           cx + halfW, cy + halfLen, shadePaint); // bottom/rear half

            canvas.restore();
        }

        // 4. Vehicle border (on top of shade so outline stays sharp)
        canvas.drawRoundRect(body, corner, corner, vehicleBorderPaint);

        // 5. Front arrow (always pointing up = direction of travel)
        Path arrow = new Path();
        float arrowY = cy - halfLen;
        arrow.moveTo(cx, arrowY - halfW * 0.5f);
        arrow.lineTo(cx - halfW * 0.5f, arrowY + halfW * 0.2f);
        arrow.lineTo(cx + halfW * 0.5f, arrowY + halfW * 0.2f);
        arrow.close();
        canvas.drawPath(arrow, arrowPaint);

        // 6. L / R labels — bright on shade side, dim on sun side
        float sunAngleForLabel = (float) Math.toRadians(relAngle - 90);
        float sunDxLabel = (float) Math.cos(sunAngleForLabel);
        Paint lrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lrPaint.setColor(Color.WHITE);

        lrPaint.setTextAlign(Paint.Align.CENTER);
        lrPaint.setTextSize(halfW * 0.55f);
        float lrY = cy + halfW * 0.55f * 0.35f;

        lrPaint.setAlpha(sunDxLabel > 0.15f ? 230 : 100); // bright when sun to right (left side in shade)
        canvas.drawText("L", cx - halfW * 0.5f, lrY, lrPaint);

        lrPaint.setAlpha(sunDxLabel < -0.15f ? 230 : 100); // bright when sun to left (right side in shade)
        canvas.drawText("R", cx + halfW * 0.5f, lrY, lrPaint);
    }

    // Shadow parallelogram using true silhouette corners (works for any sun angle)
    private void drawGroundShadow(Canvas canvas, float cx, float cy, float halfLen, float halfW, float radius) {
        double relAngle = getRelativeAzimuth();

        canvas.save();
        Path circlePath = new Path();
        circlePath.addCircle(cx, cy, radius, Path.Direction.CW);
        canvas.clipPath(circlePath);

        // Sun direction vector in canvas coords (Y-axis points down)
        float sunAngleRad = (float) Math.toRadians(relAngle - 90);
        float sunDx = (float) Math.cos(sunAngleRad);
        float sunDy = (float) Math.sin(sunAngleRad);

        // Shadow direction = opposite of sun
        float shadowLen = radius * 1.8f;

        // Perpendicular to sun direction → projects corners to find silhouette extremes
        float perpDx = -sunDy;

        float[][] corners = {
            {cx - halfW, cy - halfLen},  // TL
            {cx + halfW, cy - halfLen},  // TR
            {cx + halfW, cy + halfLen},  // BR
            {cx - halfW, cy + halfLen},  // BL
        };

        float maxP = Float.NEGATIVE_INFINITY, minP = Float.POSITIVE_INFINITY;
        int maxIdx = 0, minIdx = 0;
        for (int i = 0; i < 4; i++) {
            float p = (corners[i][0] - cx) * perpDx + (corners[i][1] - cy) * sunDx;
            if (p > maxP) { maxP = p; maxIdx = i; }
            if (p < minP) { minP = p; minIdx = i; }
        }

        float c1x = corners[minIdx][0], c1y = corners[minIdx][1];
        float c2x = corners[maxIdx][0], c2y = corners[maxIdx][1];

        Path shadowPath = new Path();
        shadowPath.moveTo(c1x, c1y);
        shadowPath.lineTo(c2x, c2y);
        shadowPath.lineTo(c2x - sunDx * shadowLen, c2y - sunDy * shadowLen);
        shadowPath.lineTo(c1x - sunDx * shadowLen, c1y - sunDy * shadowLen);
        shadowPath.close();

        Paint groundShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundShadow.setColor(Color.parseColor("#60000000"));
        groundShadow.setStyle(Paint.Style.FILL);
        canvas.drawPath(shadowPath, groundShadow);

        canvas.restore();
    }
}
