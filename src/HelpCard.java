package nz.gen.geek_central.ti5x;

import android.graphics.RectF;

public class HelpCard extends android.view.View
  /* help-card display area */
  {
    android.graphics.Bitmap CardImage;

    public HelpCard
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
        CardImage = null;
      } /*HelpCard*/

    public void SetCardImage
      (
        android.graphics.Bitmap NewCardImage
      )
      {
        if (CardImage != null)
          {
            CardImage.recycle();
          } /*if*/
        CardImage = NewCardImage;
       /* sliding animation TBD */
        invalidate();
      } /*SetCardImage*/

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
        if (CardImage != null)
          {
            final android.graphics.Matrix ImageMap = new android.graphics.Matrix();
            ImageMap.setRectToRect
              (
                /*src =*/ new android.graphics.RectF
                  (
                    0,
                    0,
                    CardImage.getWidth(),
                    CardImage.getHeight()
                  ),
                /*dst =*/ new android.graphics.RectF(0, 0, CardSize.x, CardSize.y),
                /*stf =*/ android.graphics.Matrix.ScaleToFit.CENTER
              );
            Draw.drawBitmap
              (
                /*bitmap =*/ CardImage,
                /*matrix =*/ ImageMap,
                /*paint =*/ null
              );
          } /*if*/
        Draw.drawRect /* on top of CardImage */
          (
            new RectF(0.0f, 0.0f, CardSize.x, CardSize.y * 0.25f),
            GraphicsUseful.FillWithColor(ColorScheme.LEDOff)
          );
      } /*onDraw*/

  } /*HelpCard*/
