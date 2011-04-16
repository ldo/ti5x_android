package nz.gen.geek_central.ti5x;

import android.graphics.RectF;

public class HelpCard extends android.view.View
  /* help-card display area */
  {

    public HelpCard
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
      } /*HelpCard*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas Draw
      )
      {
        super.onDraw(Draw);
        final android.graphics.PointF CardSize =
            new android.graphics.PointF(getWidth(), getHeight());
        Draw.drawRect
          (
            new RectF(0.0f, 0.0f, CardSize.x, CardSize.y),
            GraphicsUseful.FillWithColor(ColorScheme.Dark)
          );
      /* more TBD */
        Draw.drawRect
          (
            new RectF(0.0f, 0.0f, CardSize.x, CardSize.y * 0.25f),
            GraphicsUseful.FillWithColor(ColorScheme.LEDOff)
          );
      } /*onDraw*/

  } /*HelpCard*/

