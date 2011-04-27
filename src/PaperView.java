package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- virtual printer display

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

public class PaperView extends android.view.View
  {
    final int TargetWidth = 400;
      /* have to specify this explicitly, can't seem to make layout width automatic */

    public PaperView
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
      } /*PaperView*/

    public int GetViewHeight()
      {
        return
            Global.Print != null ?
                Global.Print.Paper.getHeight() * TargetWidth / Global.Print.Paper.getWidth()
            :
                0;
      } /*GetViewHeight*/

    @Override
    public void onMeasure
      (
        int MeasureWidth,
        int MeasureHeight
      )
      {
        setMeasuredDimension(TargetWidth, GetViewHeight());
      } /*onMeasure*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas Draw
      )
      {
        System.err.println("PaperView onDraw"); /* debug */
        if (Global.Print != null)
          {
            final android.graphics.Matrix FitWidth = new android.graphics.Matrix();
            final float ScaleFactor = (float)getWidth() / (float)Global.Print.Paper.getWidth();
            System.err.println("PaperView scale factor = " + ScaleFactor); /* debug */
            FitWidth.postScale(ScaleFactor, ScaleFactor, 0.0f, 0.0f);
            final android.graphics.Paint DrawBits = new android.graphics.Paint();
            DrawBits.setFilterBitmap(true);
            Draw.drawBitmap(Global.Print.Paper, FitWidth, DrawBits);
          } /*if*/
      } /*onDraw*/

  } /*PaperView*/

