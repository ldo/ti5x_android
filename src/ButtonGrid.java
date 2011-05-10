package nz.gen.geek_central.ti5x;
/*
    Display and interaction with calculator buttons

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

public class ButtonGrid extends android.view.View
  {
    static final int NrButtonRows = 9;
    static final int NrButtonCols = 5;

    android.media.SoundPool MakeNoise;
  /* it appears SoundPool allocates loaded sound IDs starting from 1 */
    int ButtonDown = 0;

    final int Dark, White, ButtonBrown, ButtonYellow, OverlayBlue;

    class ButtonDef
      /* defines appearance of a button */
      {
        int BaseCode;
        final String Text, AltText, MergedText;
        final int TextColor, ButtonColor, AltTextColor, OverlayColor, BGColor;

        public ButtonDef
          (
            String Text,
            String AltText,
            String MergedText, /* may be null */
            int TextColor,
            int ButtonColor
          )
          {
            this.Text = Text;
            this.AltText = AltText;
            this.MergedText = MergedText;
            this.TextColor = TextColor;
            this.ButtonColor = ButtonColor;
            this.AltTextColor = White;
            this.OverlayColor = OverlayBlue;
            this.BGColor = Dark;
          } /*ButtonDef*/

        public ButtonDef
          (
            String Text,
            String AltText,
            int TextColor,
            int ButtonColor
          )
          {
            this(Text, AltText, null, TextColor, ButtonColor);
          } /*ButtonDef*/

      } /*ButtonDef*/

    ButtonDef[][] ButtonDefs;

    void MakeButtonDefs()
      {
        ButtonDefs = new ButtonDef[][]
            {
                new ButtonDef[]
                    {
                        new ButtonDef("A", "A´", White, ButtonBrown),
                        new ButtonDef("B", "B´", White, ButtonBrown),
                        new ButtonDef("C", "C´", White, ButtonBrown),
                        new ButtonDef("D", "D´", White, ButtonBrown),
                        new ButtonDef("E", "E´", White, ButtonBrown),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("2nd", "", Dark, ButtonYellow),
                        new ButtonDef("INV", "", White, ButtonBrown),
                        new ButtonDef("lnx", "log", White, ButtonBrown),
                        new ButtonDef("CE", "CP", White, ButtonBrown),
                        new ButtonDef("CLR", "", Dark, ButtonYellow),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("LRN", "Pgm", White, ButtonBrown),
                        new ButtonDef("x⇌t", "P→R", White, ButtonBrown),
                        new ButtonDef("x²", "sin", White, ButtonBrown),
                        new ButtonDef("√x", "cos", White, ButtonBrown),
                        new ButtonDef("1/x", "tan", White, ButtonBrown),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("SST", "Ins", White, ButtonBrown),
                        new ButtonDef("STO", "CMs", White, ButtonBrown),
                        new ButtonDef("RCL", "Exc", White, ButtonBrown),
                        new ButtonDef("SUM", "Prd", White, ButtonBrown),
                        new ButtonDef("y**x", "Ind", White, ButtonBrown),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("BST", "Del", White, ButtonBrown),
                        new ButtonDef("EE", "Eng", White, ButtonBrown),
                        new ButtonDef("(", "Fix", White, ButtonBrown),
                        new ButtonDef(")", "Int", White, ButtonBrown),
                        new ButtonDef("÷", "|x|", Dark, ButtonYellow),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("GTO", "Pause", White, ButtonBrown),
                        new ButtonDef("7", "x=t", "Pgm Ind", Dark, White),
                        new ButtonDef("8", "Nop", "Exc Ind", Dark, White),
                        new ButtonDef("9", "Op", "Prd Ind", Dark, White),
                        new ButtonDef("×", "Deg", Dark, ButtonYellow),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("SBR", "Lbl", White, ButtonBrown),
                        new ButtonDef("4", "x≥t", "STO Ind", Dark, White),
                        new ButtonDef("5", "∑x", "RCL Ind", Dark, White),
                        new ButtonDef("6", "mean(x)", "SUM Ind", Dark, White),
                        new ButtonDef("-", "Rad", Dark, ButtonYellow),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("RST", "St flg", White, ButtonBrown),
                        new ButtonDef("1", "If flg", Dark, White),
                        new ButtonDef("2", "D.MS", "GTO Ind", Dark, White),
                        new ButtonDef("3", "π", "Op Ind", Dark, White),
                        new ButtonDef("+", "Grad", Dark, ButtonYellow),
                    },
                new ButtonDef[]
                    {
                        new ButtonDef("R/S", "", White, ButtonBrown),
                        new ButtonDef("0", "Dsz", "INV SBR", Dark, White),
                        new ButtonDef(".", "Adv", Dark, White),
                        new ButtonDef("+/-", "Prt", Dark, White),
                        new ButtonDef("=", "List", Dark, ButtonYellow),
                    },
            };
        for (int Row = 0; Row < NrButtonRows; ++Row)
          {
            for (int Col = 0; Col < NrButtonCols; ++Col)
              {
                ButtonDefs[Row][Col].BaseCode = (Row + 1) * 10 + Col + 1;
              } /*for*/
          } /*for*/
      } /*MakeButtonDefs*/

    public boolean OverlayVisible;

    final RectF ButtonRelMargins = new RectF(0.175f, 0.5f, 0.175f, 0.05f);
      /* relative bounds of button within grid cell */
    final float CornerRoundness = 1.5f;

  /* global modifier state */
    public boolean AltState;

    public int SelectedButton = -1;

    public int DigitsNeeded;
    public boolean AcceptSymbolic, AcceptInd, NextLiteral;
    public int AccumDigits, FirstOperand;
    public boolean GotFirstOperand, GotFirstInd, IsSymbolic, GotInd;
    public int CollectingForFunction;
    int ButtonCode;

    long LastClick = 0;

    public void Reset()
      /* resets to power-up state. */
      {
        AltState = false;
        ResetOperands();
        OverlayVisible = false;
        invalidate();
      } /*Reset*/

    public ButtonGrid
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
        final android.content.res.Resources Res = TheContext.getResources();
        Dark = Res.getColor(R.color.dark);
        White = Res.getColor(R.color.white);
        ButtonBrown = Res.getColor(R.color.button_brown);
        ButtonYellow = Res.getColor(R.color.button_yellow);
        OverlayBlue = Res.getColor(R.color.overlay_blue);
        MakeButtonDefs();
        MakeNoise = new android.media.SoundPool(1, android.media.AudioManager.STREAM_MUSIC, 0);
        if (MakeNoise != null)
          {
            ButtonDown = MakeNoise.load(TheContext, R.raw.button_down, 1);
          } /*if*/
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
                    final ButtonGrid TheButtons = (ButtonGrid)TheView;
                    switch (TheEvent.getAction())
                      {
                    case android.view.MotionEvent.ACTION_DOWN:
                    case android.view.MotionEvent.ACTION_MOVE:
                        final long ThisClick = java.lang.System.currentTimeMillis();
                        if (ThisClick - LastClick > 100) /* debounce */
                          {
                            final RectF GridBounds =
                                new RectF(0.0f, 0.0f, TheView.getWidth(), TheView.getHeight());
                            android.graphics.PointF ClickWhere =
                                new android.graphics.PointF(TheEvent.getX(), TheEvent.getY());
                            final float CellWidth = (float)TheView.getWidth() / NrButtonCols;
                            final float CellHeight = (float)TheView.getHeight() / NrButtonRows;
                            final android.graphics.Point ClickedCell =
                                new android.graphics.Point
                                  (
                                    Math.max(0, Math.min((int)Math.floor(ClickWhere.x / CellWidth), NrButtonCols - 1)),
                                    Math.max(0, Math.min((int)Math.floor(ClickWhere.y / CellHeight), NrButtonRows - 1))
                                  );
                            final RectF CellBounds = new RectF
                              (
                                GridBounds.left + CellWidth * ClickedCell.x,
                                GridBounds.top + CellHeight * ClickedCell.y,
                                GridBounds.left + CellWidth * (ClickedCell.x + 1),
                                GridBounds.top + CellHeight * (ClickedCell.y + 1)
                              );
                            final RectF ButtonBounds = new RectF
                              (
                                CellBounds.left + (CellBounds.right - CellBounds.left) * ButtonRelMargins.left,
                                CellBounds.top + (CellBounds.bottom - CellBounds.top) * ButtonRelMargins.top,
                                CellBounds.right + (CellBounds.left - CellBounds.right) * ButtonRelMargins.right,
                                CellBounds.bottom + (CellBounds.top - CellBounds.bottom) * ButtonRelMargins.bottom
                              );
                            int NewSelectedButton;
                            if (ButtonBounds.contains(ClickWhere.x, ClickWhere.y))
                              {
                                NewSelectedButton = ButtonDefs[ClickedCell.y][ClickedCell.x].BaseCode;
                              }
                            else
                              {
                                NewSelectedButton = -1;
                              } /*if*/
                            if (SelectedButton != NewSelectedButton)
                              {
                                SelectedButton = NewSelectedButton;
                                if (SelectedButton != -1)
                                  {
                                    if (MakeNoise != null)
                                      {
                                        MakeNoise.play(ButtonDown, 1.0f, 1.0f, 0, 0, 1.0f);
                                      } /*if*/
                                    Invoke();
                                  } /*if*/
                                TheView.invalidate();
                              } /*if*/
                            Handled = true;
                            LastClick = ThisClick;
                          } /*if*/
                    break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (SelectedButton != -1)
                          {
                            if (SelectedButton == 61 && Global.Calc != null && Global.Calc.ProgRunning)
                              {
                                Global.Calc.SetSlowExecution(false);
                              } /*if*/
                            SelectedButton = -1;
                            TheView.invalidate();
                          } /*if*/
                        Handled = true;
                    break;
                      } /*switch*/
                    return
                        Handled;
                  } /*onClick*/
              }
          );
        Reset();
      } /*ButtonGrid*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas Draw
      )
      {
        super.onDraw(Draw);
        final RectF GridBounds = new RectF(0.0f, 0.0f, getWidth(), getHeight());
        final float CellWidth = GridBounds.right / NrButtonCols;
        final float CellHeight = GridBounds.bottom / NrButtonRows;
        for (int Row = 0; Row < NrButtonRows; ++Row)
          {
            for (int Col = 0; Col < NrButtonCols; ++Col)
              {
                final ButtonDef ThisButton = ButtonDefs[Row][Col];
                final RectF CellBounds = new RectF
                  (
                    GridBounds.left + CellWidth * Col,
                    GridBounds.top + CellHeight * Row,
                    GridBounds.left + CellWidth * (Col + 1),
                    GridBounds.top + CellHeight * (Row + 1)
                  );
                final RectF ButtonBounds = new RectF
                  (
                    CellBounds.left + (CellBounds.right - CellBounds.left) * ButtonRelMargins.left,
                    CellBounds.top + (CellBounds.bottom - CellBounds.top) * ButtonRelMargins.top,
                    CellBounds.right + (CellBounds.left - CellBounds.right) * ButtonRelMargins.right,
                    CellBounds.bottom + (CellBounds.top - CellBounds.bottom) * ButtonRelMargins.bottom
                  );
                if (ThisButton.BaseCode == SelectedButton)
                  {
                    ButtonBounds.offset(2.0f, 2.0f);
                  } /*if*/
                Draw.drawRect(CellBounds, GraphicsUseful.FillWithColor(ThisButton.BGColor));
                final android.graphics.Paint TextPaint = new android.graphics.Paint();
                TextPaint.setStyle(android.graphics.Paint.Style.FILL);
                TextPaint.setColor(ThisButton.AltTextColor);
                TextPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                TextPaint.setAntiAlias(true);
                final float BaseTextSize = getResources().getDimension(R.dimen.font_size);
                TextPaint.setTextSize(BaseTextSize * 0.9f);
                GraphicsUseful.DrawCenteredText
                  (
                    Draw,
                    ThisButton.AltText,
                    (CellBounds.left + CellBounds.right) / 2.0f,
                    CellBounds.top + (CellBounds.bottom - CellBounds.top) * ButtonRelMargins.top / 2.0f,
                    TextPaint
                  );
                  {
                    RectF DrawBounds;
                    final GraphicsUseful.HSVA ButtonColor =
                        new GraphicsUseful.HSVA(ThisButton.ButtonColor);
                    TextPaint.setColor
                      (
                        new GraphicsUseful.HSVA
                          (
                            ButtonColor.H,
                            ButtonColor.S,
                            1.0f - (1.0f - ButtonColor.V) * 0.75f, /* lighten */
                            ButtonColor.A
                          ).ToRGB()
                      );
                    DrawBounds = new RectF(ButtonBounds);
                    DrawBounds.offset(-1.0f, -1.0f);
                    Draw.drawRoundRect
                      (
                        DrawBounds,
                        CornerRoundness,
                        CornerRoundness,
                        TextPaint
                      );
                    if (ThisButton.BaseCode != SelectedButton)
                      {
                        final GraphicsUseful.HSVA Darken = new GraphicsUseful.HSVA(Dark);
                        TextPaint.setColor
                          (
                            new GraphicsUseful.HSVA
                              (
                                Darken.H,
                                Darken.S,
                                Darken.V / 2.0f, /* darken */
                                Darken.A
                              ).ToRGB()
                          );
                        DrawBounds = new RectF(ButtonBounds);
                        DrawBounds.offset(2.0f, 2.0f);
                        Draw.drawRoundRect
                          (
                            DrawBounds,
                            CornerRoundness,
                            CornerRoundness,
                            TextPaint
                          );
                      } /*if*/
                    TextPaint.setColor(ButtonColor.ToRGB());
                    Draw.drawRoundRect
                      (
                        ButtonBounds,
                        CornerRoundness,
                        CornerRoundness,
                        TextPaint
                      );
                  }
                if (OverlayVisible)
                  {
                    TextPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
                    final boolean HasBaseOverlay =
                            ThisButton.BaseCode != 21
                        &&
                            ThisButton.BaseCode != 31
                        &&
                            ThisButton.BaseCode != 41
                        &&
                            ThisButton.BaseCode != 51;
                    final boolean HasAltOverlay =
                            ThisButton.BaseCode != 21
                        &&
                            ThisButton.BaseCode != 41
                        &&
                            ThisButton.BaseCode != 51;
                    final boolean HasMergedOverlay = ThisButton.MergedText != null;
                    if (HasBaseOverlay || HasAltOverlay || HasMergedOverlay)
                      {
                        final float Left = CellBounds.left + (CellBounds.right - CellBounds.left) * 0.0f;
                          /* not quite authentic position, but what the hey */
                        TextPaint.setTextSize(BaseTextSize * 0.6f);
                        TextPaint.setColor(ThisButton.OverlayColor);
                        if (HasBaseOverlay)
                          {
                            int BaseCode = ThisButton.BaseCode;
                            switch (BaseCode)
                              {
                            case 62: /*digit 7*/
                            case 63: /*digit 8*/
                            case 64: /*digit 9*/
                                BaseCode -= 55;
                            break;
                            case 72: /*digit 4*/
                            case 73: /*digit 5*/
                            case 74: /*digit 6*/
                                BaseCode -= 68;
                            break;
                            case 82: /*digit 1*/
                            case 83: /*digit 2*/
                            case 84: /*digit 3*/
                                BaseCode -= 81;
                            break;
                            case 92: /*digit 0*/
                                BaseCode = 0;
                            break;
                              } /*switch*/
                            Draw.drawText
                              (
                                String.format(Global.StdLocale, "%02d", BaseCode),
                                Left,
                                CellBounds.bottom + (ButtonBounds.top - ButtonBounds.bottom) * 0.2f,
                                TextPaint
                              );
                          } /*if*/
                        if (HasMergedOverlay)
                          {
                            Draw.drawText
                              (
                                String.format
                                  (
                                    Global.StdLocale,
                                    "%02d  %s",
                                    ThisButton.BaseCode,
                                    ThisButton.MergedText
                                  ),
                                Left,
                                CellBounds.bottom + (ButtonBounds.top - ButtonBounds.bottom) * 0.8f,
                                TextPaint
                              );
                          } /*if*/
                        if (HasAltOverlay)
                          {
                            Draw.drawText
                              (
                                String.format
                                  (
                                    Global.StdLocale,
                                    "%02d",
                                        ThisButton.BaseCode / 10 * 10
                                    +
                                        (ThisButton.BaseCode % 10 + 5) % 10
                                  ),
                                Left,
                                CellBounds.bottom + (ButtonBounds.top - ButtonBounds.bottom) * 1.4f,
                                TextPaint
                              );
                          } /*if*/
                      } /*if*/
                  } /*if*/
                TextPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                TextPaint.setColor(ThisButton.TextColor);
                TextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                TextPaint.setTextSize(BaseTextSize * 1.2f);
                GraphicsUseful.DrawCenteredText
                  (
                    Draw,
                    ThisButton.Text,
                    (ButtonBounds.left + ButtonBounds.right) / 2.0f,
                    (ButtonBounds.bottom + ButtonBounds.top) / 2.0f,
                    TextPaint
                  );
              } /*for*/
          } /*for*/
      } /*onDraw*/

    void ResetOperands()
      {
        DigitsNeeded = 0;
        AcceptSymbolic = false;
        AcceptInd = false;
        NextLiteral = false;
        AccumDigits = -1;
        GotFirstOperand = false;
        IsSymbolic = false;
        GotInd = false;
        CollectingForFunction = -1;
      } /*ResetOperands*/

    void StoreOperand
      (
        int NrDigits, /* 1, 2 or 3 */
        boolean SeparateInd
      )
      /* stores operand for a program instruction. */
      {
        if (SeparateInd && GotInd)
          {
            Global.Calc.StoreInstr(40);
          } /*if*/
        if (IsSymbolic)
          {
            Global.Calc.StoreInstr(ButtonCode);
          }
        else
          {
            if (NrDigits < 3 || GotInd)
              {
                Global.Calc.StoreInstr(AccumDigits);
              }
            else
              {
                Global.Calc.StoreInstr(AccumDigits / 100);
                Global.Calc.StoreInstr(AccumDigits % 100);
              } /*if*/
          } /*if*/
      } /*StoreOperand*/

    public void Invoke()
      {
        if (Global.Calc != null && SelectedButton > 0)
          {
            boolean WasModifier = false;
            boolean Handled = false;
            if (AltState)
              {
                ButtonCode = SelectedButton / 10 * 10 + (SelectedButton % 10 + 5) % 10;
              }
            else
              {
                ButtonCode = SelectedButton;
              } /*if*/
            if (Global.Calc.ProgRunning)
              {
                switch (ButtonCode)
                  {
                case 61:
                case 66: /*Pause, actually will always be 61 (GTO)*/
                    Global.Calc.SetSlowExecution(true);
                break;
                case 91: /*R/S*/
                case 96:
                    Global.Calc.StopProgram();
                break;
                  } /*switch*/
                Handled = true; /* ignore everything else */
              }
            else if (CollectingForFunction != -1)
              {
                int Digit = -1;
                Handled = true; /* next assumption */
                switch (ButtonCode) /* collect digits */
                  {
                case 40: /*Ind*/
                    if (AcceptInd && AccumDigits < 0)
                      {
                        GotInd = !GotInd;
                      }
                    else if (NextLiteral)
                      {
                      /* note I can't use Ind as label because I can't goto/gosub it */
                        Global.Calc.SetErrorState();
                      }
                    else
                      {
                        Handled = false;
                      } /*if*/
                break;
                case 21:
                case 26:
                  /* needed for Ind and Lbl */
                    AltState = !AltState;
                    WasModifier = true;
                break;
                case 62: /*digit 7*/
                case 63: /*digit 8*/
                case 64: /*digit 9*/
                    if (NextLiteral)
                      {
                        Global.Calc.SetErrorState();
                      }
                    else
                      {
                        Digit = ButtonCode - 55;
                      } /*if*/
                break;
                case 72: /*digit 4*/
                case 73: /*digit 5*/
                case 74: /*digit 6*/
                    if (NextLiteral)
                      {
                        Global.Calc.SetErrorState();
                      }
                    else
                      {
                        Digit = ButtonCode - 68;
                      } /*if*/
                break;
                case 82: /*digit 1*/
                case 83: /*digit 2*/
                case 84: /*digit 3*/
                    if (NextLiteral)
                      {
                        Global.Calc.SetErrorState();
                      }
                    else
                      {
                        Digit = ButtonCode - 81;
                      } /*if*/
                break;
                case 92: /*digit 0*/
                    if (NextLiteral)
                      {
                        Global.Calc.SetErrorState();
                      }
                    else
                      {
                        Digit = 0;
                      } /*if*/
                break;
                default:
                    if (AcceptSymbolic || NextLiteral)
                      {
                        IsSymbolic = true;
                      }
                    else
                      {
                        Handled = false;
                      } /*if*/
                break;
                  } /*switch*/
                if (Digit >= 0)
                  {
                    if (AccumDigits < 0)
                      {
                        AccumDigits = 0;
                        AcceptSymbolic = false;
                        if (GotInd)
                          {
                            DigitsNeeded = 2; /* for register number */
                          } /*if*/
                      } /*if*/
                    AccumDigits = AccumDigits * 10 + Digit;
                  } /*if*/
                if (Handled)
                  {
                    if (Digit >= 0)
                      {
                        --DigitsNeeded;
                      } /*if*/
                  }
                else
                  {
                    DigitsNeeded = 0; /* non-digit cuts short digit entry */
                  } /*if*/
                if (!WasModifier && (DigitsNeeded == 0 || IsSymbolic))
                  {
                    boolean Finished = true; /* to begin with */
                    if (IsSymbolic || AccumDigits >= 0)
                      {
                        switch (CollectingForFunction)
                          {
                        case 36: /*Pgm*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 62 : 36);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.SelectProgram(AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 42: /*STO*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 72 : 42);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.MemoryOp(Global.Calc.MEMOP_STO, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 43: /*RCL*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 73 : 43);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.MemoryOp(Global.Calc.MEMOP_RCL, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 44: /*SUM*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 74 : 44);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.MemoryOp(Global.Calc.MEMOP_ADD, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 48: /*Exc*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 63 : 48);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.MemoryOp(Global.Calc.MEMOP_EXC, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 49: /*Prd*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 64 : 49);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.MemoryOp(Global.Calc.MEMOP_MUL, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 58: /*Fix*/
                          /* assert not Global.Calc.InvState */
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(58);
                                StoreOperand(1, true);
                              }
                            else
                              {
                                Global.Calc.SetDisplayMode(Global.Calc.FORMAT_FIXED, AccumDigits);
                              } /*if*/
                        break;
                        case 67: /*x=t*/
                        case 77: /*x≥t*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(CollectingForFunction);
                                StoreOperand(3, true);
                              }
                            else
                              {
                                Global.Calc.CompareBranch
                                  (
                                    CollectingForFunction == 77,
                                    Global.Calc.CurBank, AccumDigits, GotInd
                                  );
                              } /*if*/
                        break;
                        case 69: /*Op*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 84 : 69);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                Global.Calc.SpecialOp(AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 61: /*GTO*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(GotInd ? 83 : 61);
                                StoreOperand(3, false);
                              }
                            else
                              {
                                Global.Calc.FillInLabels();
                                Global.Calc.Transfer
                                  (
                                    /*Type =*/
                                        Global.Calc.InvState ?
                                            Global.Calc.TRANSFER_TYPE_LEA /*extension!*/
                                        :
                                            Global.Calc.TRANSFER_TYPE_GTO,
                                    /*BankNr =*/ Global.Calc.CurBank,
                                    /*Loc =*/ IsSymbolic ? ButtonCode : AccumDigits,
                                    /*LocType =*/
                                        IsSymbolic ?
                                            Global.Calc.TRANSFER_LOC_SYMBOLIC
                                        : GotInd ?
                                            Global.Calc.TRANSFER_LOC_INDIRECT
                                        :
                                            Global.Calc.TRANSFER_LOC_DIRECT
                                  );
                              } /*if*/
                        break;
                        case 71: /*SBR*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(71);
                                StoreOperand(3, true);
                              }
                            else
                              {
                                Global.Calc.FillInLabels();
                                Global.Calc.Transfer
                                  (
                                    /*Type =*/ Global.Calc.TRANSFER_TYPE_INTERACTIVE_CALL,
                                    /*BankNr =*/ Global.Calc.CurBank,
                                    /*Loc =*/ IsSymbolic ? ButtonCode : AccumDigits,
                                    /*LocType =*/
                                        IsSymbolic ?
                                            Global.Calc.TRANSFER_LOC_SYMBOLIC
                                        : GotInd ?
                                            Global.Calc.TRANSFER_LOC_INDIRECT
                                        :
                                            Global.Calc.TRANSFER_LOC_DIRECT
                                  );
                              } /*if*/
                        break;
                        case 76: /*Lbl*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(76);
                                Global.Calc.StoreInstr(ButtonCode); /* always symbolic */
                              }
                            else
                              {
                              /* ignore */
                              } /*if*/
                        break;
                        case 86: /*St flg*/
                            if (Global.Calc.ProgMode)
                              {
                                Global.Calc.StoreInstr(86);
                                StoreOperand(1, true);
                              }
                            else
                              {
                                Global.Calc.SetFlag(AccumDigits, GotInd, !Global.Calc.InvState);
                              } /*if*/
                        break;
                        case 87: /*If flg*/
                        case 97: /*Dsz*/
                            if (GotFirstOperand)
                              {
                                if (Global.Calc.ProgMode)
                                  {
                                    Global.Calc.StoreInstr(CollectingForFunction);
                                    if (GotFirstInd)
                                      {
                                        Global.Calc.StoreInstr(40);
                                      } /*if*/
                                    Global.Calc.StoreInstr(FirstOperand);
                                    StoreOperand(3, true);
                                  }
                                else
                                  {
                                    if (CollectingForFunction == 87) /*If flg*/
                                      {
                                        Global.Calc.BranchIfFlag
                                          (
                                            FirstOperand, GotFirstInd,
                                            Global.Calc.CurBank, AccumDigits,
                                            IsSymbolic ?
                                                Global.Calc.TRANSFER_LOC_SYMBOLIC
                                            : GotInd ?
                                                Global.Calc.TRANSFER_LOC_INDIRECT
                                            :
                                                Global.Calc.TRANSFER_LOC_DIRECT
                                          );
                                      }
                                    else /*Dsz*/
                                      {
                                        Global.Calc.DecrementSkip
                                          (
                                            FirstOperand, GotFirstInd,
                                            Global.Calc.CurBank, AccumDigits,
                                            IsSymbolic ?
                                                Global.Calc.TRANSFER_LOC_SYMBOLIC
                                            : GotInd ?
                                                Global.Calc.TRANSFER_LOC_INDIRECT
                                            :
                                                Global.Calc.TRANSFER_LOC_DIRECT
                                          );
                                      } /*if*/
                                  } /*if*/
                              }
                            else
                              {
                                GotFirstInd = GotInd;
                                GotInd = false;
                                AcceptInd = true;
                                FirstOperand = AccumDigits;
                                AccumDigits = -1;
                                GotFirstOperand = true;
                                DigitsNeeded = 3;
                                AcceptSymbolic = true;
                                Finished = false;
                              } /*if*/
                        break;
                        default:
                          /* shouldn't occur */
                            throw new RuntimeException("unhandled collected function " + CollectingForFunction);
                      /* break; */
                          } /*switch*/
                      }
                    else
                      {
                        Global.Calc.SetErrorState();
                        Handled = true;
                      } /*if*/
                    if (Finished)
                      {
                        CollectingForFunction = -1;
                        ResetOperands();
                      } /*if*/
                  } /*if*/
              } /*if CollectingForFunction*/
            if (!Handled)
              {
              /* check for functions needing further entry */
                if (!Global.Calc.ProgMode || Global.Calc.ProgramWritable())
                  {
                    Handled = true; /* next assumption */
                    switch (ButtonCode)
                      {
                    case 36: /* Pgm */
                    case 42: /* STO */
                    case 43: /* RCL */
                    case 44: /* SUM */
                    case 48: /* Exc */
                    case 49: /* Prd */
                    case 69: /* Op */
                        DigitsNeeded = 2;
                        AcceptInd = true;
                    break;
                    case 58: /* Fix */
                        if (!Global.Calc.InvState)
                          {
                            DigitsNeeded = 1;
                            AcceptInd = true;
                          }
                        else
                          {
                            Handled = false; /* no special handling required */
                          } /*if*/
                    break;
                    case 67: /* x = t */
                    case 77: /* x ≥ t */
                        DigitsNeeded = 3;
                        AcceptInd = true;
                        AcceptSymbolic = true;
                    break;
                    case 61: /* GTO */
                    case 71: /* SBR */
                        if (ButtonCode == 71 && Global.Calc.InvState)
                          {
                            Handled = false; /* special handling for INV SBR happens below */
                          }
                        else
                          {
                            DigitsNeeded = 3;
                            AcceptInd = true;
                            AcceptSymbolic = true;
                          } /*if*/
                    break;
                    case 76: /* Lbl */
                        NextLiteral = true;
                    break;
                    case 86: /* St flg */
                        DigitsNeeded = 1;
                        AcceptInd = true;
                    break;
                    case 87: /* If flg */
                    case 97: /* Dsz */
                        DigitsNeeded = 1;
                        AcceptInd = true;
                        AcceptSymbolic = false;
                        GotFirstOperand = false;
                    break;
                    default:
                      /* wasn't one of these after all */
                        Handled = false;
                    break;
                      } /*ButtonCode*/
                    if (Handled)
                      {
                        CollectingForFunction = ButtonCode;
                      } /*if*/
                  } /*if*/
              } /*if*/
            if (!Handled && ButtonCode == 40 /*Ind*/)
              {
              /* ignore? */
                Handled = true;
              } /*if*/
            if (!Handled)
              {
              /* deal with everything not already handled */
                if (Global.Calc.ProgMode)
                  {
                    if
                      (
                            Global.Calc.ProgramWritable()
                        ||
                            ButtonCode == 31 /*LRN*/
                        ||
                            ButtonCode == 41 /*SST*/
                        ||
                            ButtonCode == 51 /*BST*/
                      )
                      {
                        switch (ButtonCode) /* undo effect of 2nd key on buttons with no alt function */
                          {
                        case 27: /* INV */
                            ButtonCode = 22;
                        break;
                        case 20: /* CLR */
                            ButtonCode = 25;
                        break;
                        case 96: /* R/S */
                            ButtonCode = 91;
                        break;
                          } /*switch*/
                        switch (ButtonCode)
                          /* special handling of program-editing/viewing functions
                            and number entry */
                          {
                        case 21:
                        case 26:
                            AltState = !AltState;
                            WasModifier = true;
                        break;
                        case 22:
                            Global.Calc.StoreInstr(22);
                            Global.Calc.InvState = !Global.Calc.InvState;
                            WasModifier = true;
                        break;
                        case 31: /*LRN*/
                            Global.Calc.SetProgMode(false);
                            ResetOperands();
                        break;
                      /* 40 handled above */
                        case 41: /*SST*/
                            Global.Calc.StepPC(true);
                            ResetOperands();
                        break;
                        case 46: /*Ins*/
                            Global.Calc.InsertAtCurInstr();
                            ResetOperands();
                        break;
                        case 51: /*BST*/
                            Global.Calc.StepPC(false);
                            ResetOperands();
                        break;
                        case 56: /*Del*/
                            Global.Calc.DeleteCurInstr();
                            ResetOperands();
                        break;
                        case 62: /*digit 7*/
                        case 63: /*digit 8*/
                        case 64: /*digit 9*/
                            Global.Calc.StoreInstr(ButtonCode - 55);
                        break;
                        case 71: /*SBR*/
                            if (Global.Calc.InvState)
                              {
                                Global.Calc.StorePrevInstr(92);
                              } /*if*/
                          /* else handled above */
                        break;
                        case 72: /*digit 4*/
                        case 73: /*digit 5*/
                        case 74: /*digit 6*/
                            Global.Calc.StoreInstr(ButtonCode - 68);
                        break;
                      /* 76 handled above */
                        case 82: /*digit 1*/
                        case 83: /*digit 2*/
                        case 84: /*digit 3*/
                            Global.Calc.StoreInstr(ButtonCode - 81);
                        break;
                        case 92: /*digit 0*/
                            Global.Calc.StoreInstr(0);
                        break;
                        default:
                            Global.Calc.StoreInstr(ButtonCode);
                        break;
                          } /*switch*/
                      } /*if*/
                      /* else ignore program entry attempts */
                  }
                else /* calculation mode */
                  {
                    switch (ButtonCode)
                      {
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 10:
                        Global.Calc.FillInLabels();
                        Global.Calc.Transfer
                          (
                            /*Type =*/ Global.Calc.TRANSFER_TYPE_INTERACTIVE_CALL,
                            /*BankNr =*/ Global.Calc.CurBank,
                            /*Loc =*/ ButtonCode,
                            /*LocType =*/ Global.Calc.TRANSFER_LOC_SYMBOLIC
                          );
                    break;
                    case 21:
                    case 26:
                        AltState = !AltState;
                        WasModifier = true;
                    break;
                    case 22:
                    case 27:
                        Global.Calc.InvState = !Global.Calc.InvState;
                        WasModifier = true;
                    break;
                    case 23:
                        Global.Calc.Ln();
                    break;
                    case 24:
                        Global.Calc.ClearEntry();
                    break;
                    case 25:
                    case 20:
                        Global.Calc.ClearAll();
                    break;
                  /* 26 same as 21 */
                  /* 27 same as 22 */
                    case 28:
                        Global.Calc.Log();
                    break;
                    case 29:
                        Global.Calc.ClearProgram();
                    break;
                  /* 20 same as 25 */
                    case 31: /*LRN*/
                        Global.Calc.SetProgMode(true);
                        ResetOperands();
                    break;
                    case 32:
                        Global.Calc.SwapT();
                    break;
                    case 33:
                        Global.Calc.Square();
                    break;
                    case 34:
                        Global.Calc.Sqrt();
                    break;
                    case 35:
                        Global.Calc.Reciprocal();
                    break;
                  /* 36 handled above */
                    case 37:
                        Global.Calc.Polar();
                    break;
                    case 38:
                        Global.Calc.Sin();
                    break;
                    case 39:
                        Global.Calc.Cos();
                    break;
                    case 30:
                        Global.Calc.Tan();
                    break;
                    case 41:
                        Global.Calc.StepProgram();
                    break;
                  /* 42, 43, 44 handled above */
                    case 45:
                        Global.Calc.Operator(Global.Calc.STACKOP_EXP);
                    break;
                    case 46:
                      /* ignore? */
                    break;
                    case 47:
                        Global.Calc.ClearMemories();
                    break;
                  /* 48, 49, 40 handled above */
                    case 51:
                      /* ignore */
                    break;
                    case 52:
                        Global.Calc.EnterExponent();
                    break;
                    case 53:
                        Global.Calc.LParen();
                    break;
                    case 54:
                        Global.Calc.RParen();
                    break;
                    case 55:
                        Global.Calc.Operator(Global.Calc.STACKOP_DIV);
                    break;
                    case 56:
                      /* ignore */
                    break;
                    case 57:
                        Global.Calc.SetDisplayMode
                          (
                            Global.Calc.InvState ? Global.Calc.FORMAT_FIXED : Global.Calc.FORMAT_ENG,
                            -1
                          );
                    break;
                    case 58:
                      /* assert Global.Calc.InvState */
                        Global.Calc.SetDisplayMode(Global.Calc.FORMAT_FIXED, -1);
                    break;
                    case 59:
                        Global.Calc.Int();
                    break;
                    case 50:
                        Global.Calc.Abs();
                    break;
                  /* 61 handled above */
                    case 62:
                        Global.Calc.Digit('7');
                    break;
                    case 63:
                        Global.Calc.Digit('8');
                    break;
                    case 64:
                        Global.Calc.Digit('9');
                    break;
                    case 65:
                        Global.Calc.Operator(Global.Calc.STACKOP_MUL);
                    break;
                    case 66: /*Pause*/
                      /* ignore */
                    break;
                  /* 67 handled above */
                    case 68: /*Nop*/
                      /* No semantic effect, but why not do some saving of
                        volatile state, just in case */
                        Persistent.SaveState(getContext());
                        if (Global.Export != null)
                          {
                            Global.Export.Flush();
                          } /*if*/
                    break;
                  /* 69 handled above */
                    case 60:
                        Global.Calc.SetAngMode(Global.Calc.ANG_DEG);
                    break;
                    case 71:
                        if (Global.Calc.InvState)
                          {
                            Global.Calc.Return();
                          } /*if*/
                        /* else handled above */
                    break;
                    case 72:
                        Global.Calc.Digit('4');
                    break;
                    case 73:
                        Global.Calc.Digit('5');
                    break;
                    case 74:
                        Global.Calc.Digit('6');
                    break;
                    case 75:
                        Global.Calc.Operator(Global.Calc.STACKOP_SUB);
                    break;
                    case 76:
                      /* ignore */
                    break;
                  /* 77 handled above */
                    case 78:
                        Global.Calc.StatsSum();
                    break;
                    case 79:
                        Global.Calc.StatsResult();
                    break;
                    case 70:
                        Global.Calc.SetAngMode(Global.Calc.ANG_RAD);
                    break;
                    case 81:
                        Global.Calc.ResetProg();
                    break;
                    case 82:
                        Global.Calc.Digit('1');
                    break;
                    case 83:
                        Global.Calc.Digit('2');
                    break;
                    case 84:
                        Global.Calc.Digit('3');
                    break;
                    case 85:
                        Global.Calc.Operator(Global.Calc.STACKOP_ADD);
                    break;
                  /* 86, 87 handled above */
                    case 88:
                        Global.Calc.D_MS();
                    break;
                    case 89:
                        Global.Calc.Pi();
                    break;
                    case 80:
                        Global.Calc.SetAngMode(Global.Calc.ANG_GRAD);
                    break;
                    case 91:
                    case 96:
                        Global.Calc.StartProgram();
                    break;
                    case 92:
                        Global.Calc.Digit('0');
                    break;
                    case 93:
                        Global.Calc.DecimalPoint();
                    break;
                    case 94:
                        Global.Calc.ChangeSign();
                    break;
                    case 95:
                        Global.Calc.Equals();
                    break;
                  /* 96 same as 91 */
                  /* 97 handled above */
                    case 98: /*Adv*/
                        if (Global.Calc.InvState) /* extension! */
                          {
                            if (Global.Export != null)
                              {
                                Global.Export.Close();
                              } /*if*/
                          }
                        else
                          {
                            if (Global.Print != null)
                              {
                                Global.Print.Advance();
                              } /*if*/
                          } /*if*/
                    break;
                    case 99: /*Prt*/
                        if (Global.Calc.InvState) /* extension! */
                          {
                            Global.Calc.GetNextImport();
                          }
                        else
                          {
                            Global.Calc.PrintDisplay(false);
                          } /*if*/
                    break;
                    case 90:
                        if (Global.Print != null)
                          {
                          /* TBD */
                          } /*if*/
                    break;
                      } /*switch*/
                  } /*if ProgMode*/
              } /*if not Handled*/
            if (!WasModifier)
              {
                AltState = false;
              } /*if*/
            if (Global.Calc.InErrorState())
              {
                ResetOperands(); /* abandon */ /* is this necessary? */
              } /*if*/
            if (!WasModifier && CollectingForFunction < 0)
              {
                Global.Calc.InvState = false;
              } /*if*/
          } /*if*/
      } /*Invoke*/

  } /*ButtonGrid*/
