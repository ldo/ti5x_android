package nz.gen.geek_central.ti5x;
/*
    Help-card display area

    Copyright 2011 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    This program is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
    A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import android.graphics.RectF;

public class HelpCard extends android.view.View
  {
    final android.content.Context TheContext;
    android.graphics.Bitmap CardImage;
    byte[] Help;

    public HelpCard
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
        this.TheContext = TheContext;
        CardImage = null;
        Help = null;
        setOnTouchListener
          (
            new android.view.View.OnTouchListener()
              {
                public boolean onTouch
                  (
                    android.view.View TheView,
                    android.view.MotionEvent TheEvent
                  )
                  {
                    boolean Handled = false;
                    switch (TheEvent.getAction())
                      {
                    case android.view.MotionEvent.ACTION_DOWN:
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (Help != null)
                          {
                            final android.content.Intent ShowHelp =
                                new android.content.Intent(android.content.Intent.ACTION_VIEW);
                            ShowHelp.putExtra("content", Help);
                            ShowHelp.setClass(HelpCard.this.TheContext, Help.class);
                            HelpCard.this.TheContext.startActivity(ShowHelp);
                          }
                        else
                          {
                            android.widget.Toast.makeText
                              (
                                /*context =*/ HelpCard.this.TheContext,
                                /*text =*/ HelpCard.this.TheContext.getString(R.string.no_help),
                                /*duration =*/ android.widget.Toast.LENGTH_SHORT
                              ).show();
                          } /*if*/
                        Handled = true;
                    break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                      /* ignore */
                        Handled = true;
                    break;
                      } /*switch*/
                    return
                        Handled;
                  } /*onClick*/
              }
          );
      } /*HelpCard*/

    public void SetHelp
      (
        android.graphics.Bitmap NewCardImage,
        byte[] NewHelp
      )
      {
        CardImage = NewCardImage;
       /* sliding animation TBD */
        Help = NewHelp;
        invalidate();
      } /*SetHelp*/

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
                    CardImage.getWidth() * 70.0f / 72.0f,
                      /* right-hand edge of card disappears in entry slot */
                    CardImage.getHeight()
                  ),
                /*dst =*/ new android.graphics.RectF(0, 0, CardSize.x, CardSize.y),
                /*stf =*/ android.graphics.Matrix.ScaleToFit.CENTER
              );
            final android.graphics.Paint DrawBits = new android.graphics.Paint();
            DrawBits.setFilterBitmap(true);
            Draw.drawBitmap
              (
                /*bitmap =*/ CardImage,
                /*matrix =*/ ImageMap,
                /*paint =*/ DrawBits
              );
          } /*if*/
        Draw.drawRect /* on top of CardImage */
          (
            new RectF(0.0f, 0.0f, CardSize.x, CardSize.y * 0.25f),
            GraphicsUseful.FillWithColor(ColorScheme.LEDOff)
          );
      } /*onDraw*/

  } /*HelpCard*/
