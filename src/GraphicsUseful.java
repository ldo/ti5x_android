package nz.gen.geek_central.ti5x;
/*
    Useful graphics routines

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

    public static class HSVA
      /* HSV colour space with alpha */
      {
        public final float H, S, V, A;

        public HSVA
          (
            int ARGB
          )
          {
            final float R = (ARGB >> 16 & 255) / 255.0f;
            final float G = (ARGB >> 8 & 255) / 255.0f;
            final float B = (ARGB & 255) / 255.0f;
            float Max, Min;
            float first, second, plus;
            if (R > G && R > B)
              {
                Max = R;
                Min = Math.min(G, B);
                first = G;
                second = B;
                plus = 6.0f;
              }
            else if (B > R && B > G)
              {
                Max = B;
                Min = Math.min(R, G);
                first = R;
                second = G;
                plus = 4.0f;
              }
            else /*G is largest*/
              {
                Max = G;
                Min = Math.min(R, B);
                first = B;
                second = R;
                plus = 2.0f;
              } /*if*/
            final float chroma = Max - Min;
            if (chroma > 0.0f)
              {
                H = (float)(Math.IEEEremainder(((first - second) / chroma + plus), 6.0) / 6.0);
                S = chroma / Max;
              }
            else
              {
                H = 0.0f; /*actually undefined*/
                S = 0.0f;
              } /*if*/
            V = Max;
            A = (ARGB >> 24 & 255) / 255.0f;
          } /*HSVA*/

        public HSVA
          (
            float H,
            float S,
            float V,
            float A
          )
          {
            this.H = H;
            this.S = S;
            this.V = V;
            this.A = A;
          } /*HSVA*/

        public int ToRGB()
          {
            final int hue = (int)(H * 360.0f);
            final int chroma = (int)(V * S * 255.0f);
            final int second = chroma * (60 - Math.abs(hue % 120 - 60)) / 60;
            final int brighten = (int)(V * 255.0f) - chroma;
            final int[] RGB = new int[3];
            int primary, secondary, opposite;
            if (hue < 60)
              {
                primary = 0;
                secondary = 1;
                opposite = 2;
              }
            else if (hue >= 60 && hue < 120)
              {
                primary = 1;
                secondary = 0;
                opposite = 2;
              }
            else if (hue >= 120 && hue < 180)
              {
                primary = 1;
                secondary = 2;
                opposite = 0;
              }
            else if (hue >= 180 && hue < 240)
              {
                primary = 2;
                secondary = 1;
                opposite = 0;
              }
            else if (hue >= 240 && hue < 300)
              {
                primary = 2;
                secondary = 0;
                opposite = 1;
              }
            else /* hue >= 300 && hue < 360 */
              {
                primary = 0;
                secondary = 2;
                opposite = 1;
              } /*if*/
            RGB[primary] = Math.max(0, Math.min(chroma + brighten, 255));
            RGB[secondary] = Math.max(0, Math.min(second + brighten, 255));
            RGB[opposite] = Math.max(0, Math.min(brighten, 255));
            return
                    (int)(A * 255.0f) << 24
                |
                    RGB[0] << 16
                |
                    RGB[1] << 8
                |
                    RGB[2];
          } /*ToRGB*/

      } /*HSVA*/

  } /*GraphicsUseful*/
