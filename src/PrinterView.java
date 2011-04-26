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

public class PrinterView extends android.app.Activity
  {
    android.widget.ImageView PaperView;

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.printer);
        PaperView = (android.widget.ImageView)findViewById(R.id.paper);
      } /*onCreate*/

    @Override
    public void onPause()
      {
        super.onPause();
        if (Global.Print != null)
          {
            Global.Print.ShowingView = null;
          } /*if*/
      } /*onPause*/

    @Override
    public void onResume()
      {
        super.onResume();
        if (Global.Print != null)
          {
            final android.graphics.Matrix FitWidth = new android.graphics.Matrix();
            final float ScaleFactor = (float)PaperView.getWidth() / (float)Global.Print.Paper.getWidth();
            System.err.println("PrinterView scale factor = " + ScaleFactor); /* debug */
            FitWidth.postScale
              (
                ScaleFactor,
                ScaleFactor,
                (float)PaperView.getWidth() / 2.0f,
                /* (float)PaperView.getHeight() */ 0.0f
              );
            PaperView.setImageBitmap(Global.Print.Paper);
            PaperView.setScaleType(android.widget.ImageView.ScaleType.MATRIX);
            PaperView.setScaleType(android.widget.ImageView.ScaleType.FIT_END); /* debug */
            PaperView.setImageMatrix(FitWidth);
            Global.Print.ShowingView = PaperView;
          } /*if*/
      } /*onResume*/

  } /*PrinterView*/

