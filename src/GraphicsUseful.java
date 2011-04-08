package nz.gen.geek_central.ti5x;

public class GraphicsUseful
  {

    public static android.graphics.Paint FillWithColor
      (
        int TheColor
      )
      /* returns a Paint that will fill with a solid colour. */
      {
        final android.graphics.Paint ThePaint = new android.graphics.Paint();
        ThePaint.setStyle(android.graphics.Paint.Style.FILL);
        ThePaint.setColor(TheColor);
        return
            ThePaint;
      } /*FillWithColor*/

    public static void DrawCenteredText
      (
        android.graphics.Canvas Draw,
        String TheText,
        float x,
        float y,
        android.graphics.Paint UsePaint
      )
      /* draws text at position x, vertically centred around y. */
      {
        final android.graphics.Rect TextBounds = new android.graphics.Rect();
        UsePaint.getTextBounds(TheText, 0, TheText.length(), TextBounds);
        Draw.drawText
          (
            TheText,
            x, /* depend on UsePaint to align horizontally */
            y + (TextBounds.bottom - TextBounds.top) / 2.0f,
            UsePaint
          );
      } /*DrawCenteredText*/

  } /*GraphicsUseful*/
