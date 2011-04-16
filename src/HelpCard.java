package nz.gen.geek_central.ti5x;

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
        final android.graphics.RectF GridBounds =
            new android.graphics.RectF(0.0f, 0.0f, getWidth(), getHeight());
        Draw.drawRect(GridBounds, GraphicsUseful.FillWithColor(ColorScheme.Dark));
      /* more TBD */
      } /*onDraw*/

  } /*HelpCard*/

