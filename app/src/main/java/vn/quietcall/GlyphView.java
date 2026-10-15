package vn.quietcall;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small vector line icons that don't depend on a device's emoji font. */
public final class GlyphView extends View {
    private final String glyph;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public GlyphView(Context context, String glyph, int color) {
        super(context); this.glyph = glyph; paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.85f);
        paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void path(Canvas c, float... points) {
        Path p = new Path(); p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]); c.drawPath(p, paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save();
        float size = Math.min(getWidth(), getHeight()) * .67f;
        canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2); canvas.scale(size / 24, size / 24);
        switch (glyph) {
            case "shield", "blocked" -> {
                Path p = new Path(); p.moveTo(12, 2); p.lineTo(21, 6); p.lineTo(20, 14); p.quadTo(18, 20, 12, 23);
                p.quadTo(6, 20, 4, 14); p.lineTo(3, 6); p.close(); canvas.drawPath(p, paint);
                if (glyph.equals("blocked")) path(canvas, 8, 12, 11, 15, 16, 9);
            }
            case "phone" -> {
                Path p = new Path(); p.moveTo(5, 3); p.lineTo(9, 2); p.lineTo(11, 8); p.lineTo(8, 10);
                p.quadTo(10, 15, 15, 17); p.lineTo(17, 14); p.lineTo(23, 16); p.lineTo(22, 20);
                p.quadTo(21, 23, 17, 22); p.quadTo(3, 19, 2, 7); p.quadTo(1, 4, 5, 3); canvas.drawPath(p, paint);
            }
            case "brands" -> {
                path(canvas, 4, 10, 4, 22, 20, 22, 20, 10);
                path(canvas, 2, 10, 5, 3, 19, 3, 22, 10, 2, 10);
                path(canvas, 9, 3, 8, 10); path(canvas, 15, 3, 16, 10);
                path(canvas, 9, 22, 9, 15, 15, 15, 15, 22);
            }
            case "report" -> { path(canvas, 5, 23, 5, 3, 20, 3, 17, 9, 20, 15, 5, 15); }
            case "settings" -> {
                canvas.drawCircle(12, 12, 7, paint); canvas.drawCircle(12, 12, 3, paint);
                for (int i = 0; i < 8; i++) { canvas.save(); canvas.rotate(i * 45, 12, 12); path(canvas, 12, 2, 12, 5); canvas.restore(); }
            }
            case "search" -> { canvas.drawCircle(10, 10, 6.5f, paint); path(canvas, 15, 15, 22, 22); }
            case "clock" -> { canvas.drawCircle(12, 12, 9, paint); path(canvas, 12, 6, 12, 12, 17, 15); }
            case "plus" -> { path(canvas, 12, 4, 12, 20); path(canvas, 4, 12, 20, 12); }
            case "chevron" -> path(canvas, 9, 5, 16, 12, 9, 19);
            case "back" -> { path(canvas, 11, 4, 3, 12, 11, 20); path(canvas, 3, 12, 22, 12); }
            case "close" -> { path(canvas, 5, 5, 19, 19); path(canvas, 19, 5, 5, 19); }
            case "more" -> { paint.setStyle(Paint.Style.FILL); for (int i = 0; i < 3; i++) canvas.drawCircle(12, 5 + i * 7, 1.6f, paint); paint.setStyle(Paint.Style.STROKE); }
            case "mute" -> { path(canvas, 3, 9, 8, 9, 14, 4, 14, 20, 8, 15, 3, 15, 3, 9); path(canvas, 18, 9, 23, 15); path(canvas, 23, 9, 18, 15); }
            case "check" -> path(canvas, 4, 12, 10, 18, 21, 5);
        }
        canvas.restore();
    }
}
