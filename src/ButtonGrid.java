package nz.gen.geek_central.ti5x;

import android.graphics.RectF;

public class ButtonGrid extends android.view.View
  /* display and interaction with calculator buttons */
  {
  /* colour scheme: */
    public static final int Dark = 0xff222424;
    public static final int Brown = 0xff4e3836;
    public static final int Yellow = 0xffcc9858;
    public static final int White = 0xffbdaa7d;

    static final int NrButtonRows = 9;
    static final int NrButtonCols = 5;

    android.media.SoundPool MakeNoise;
  /* it appears SoundPool allocates loaded sound IDs starting from 1 */
    int ButtonDown = 0;

    class ButtonDef
      /* defines appearance of a button */
      {
        int BaseCode;
        final String Text, AltText;
        final int TextColor, ButtonColor, AltTextColor, BGColor;

        public ButtonDef
          (
            String Text,
            String AltText,
            int TextColor,
            int ButtonColor
          )
          {
            this.Text = Text;
            this.AltText = AltText;
            this.TextColor = TextColor;
            this.ButtonColor = ButtonColor;
            this.AltTextColor = White;
            this.BGColor = Dark;
          } /*ButtonDef*/

      } /*ButtonDef*/

    ButtonDef[][] ButtonDefs =
        {
            new ButtonDef[]
                {
                    new ButtonDef("A", "A´", White, Brown),
                    new ButtonDef("B", "B´", White, Brown),
                    new ButtonDef("C", "C´", White, Brown),
                    new ButtonDef("D", "D´", White, Brown),
                    new ButtonDef("E", "E´", White, Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("2nd", "", Dark, Yellow),
                    new ButtonDef("INV", "", White, Brown),
                    new ButtonDef("lnx", "log", White, Brown),
                    new ButtonDef("CE", "CP", White, Brown),
                    new ButtonDef("CLR", "", Dark, Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("LRN", "Pgm", White, Brown),
                    new ButtonDef("x⇌t", "P→R", White, Brown),
                    new ButtonDef("x²", "sin", White, Brown),
                    new ButtonDef("√x", "cos", White, Brown),
                    new ButtonDef("1/x", "tan", White, Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SST", "Ins", White, Brown),
                    new ButtonDef("STO", "CMs", White, Brown),
                    new ButtonDef("RCL", "Exc", White, Brown),
                    new ButtonDef("SUM", "Prd", White, Brown),
                    new ButtonDef("y**x", "Ind", White, Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("BST", "Del", White, Brown),
                    new ButtonDef("EE", "Eng", White, Brown),
                    new ButtonDef("(", "Fix", White, Brown),
                    new ButtonDef(")", "Int", White, Brown),
                    new ButtonDef("÷", "|x|", Dark, Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("GTO", "Pause", White, Brown),
                    new ButtonDef("7", "x=t", Dark, White),
                    new ButtonDef("8", "Nop", Dark, White),
                    new ButtonDef("9", "Op", Dark, White),
                    new ButtonDef("×", "Deg", Dark, Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SBR", "Lbl", White, Brown),
                    new ButtonDef("4", "x≥t", Dark, White),
                    new ButtonDef("5", "∑x", Dark, White),
                    new ButtonDef("6", "mean(x)", Dark, White),
                    new ButtonDef("-", "Rad", Dark, Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("RST", "St flg", White, Brown),
                    new ButtonDef("1", "If flg", Dark, White),
                    new ButtonDef("2", "D.MS", Dark, White),
                    new ButtonDef("3", "π", Dark, White),
                    new ButtonDef("+", "Grad", Dark, Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("R/S", "", White, Brown),
                    new ButtonDef("0", "Dsz", Dark, White),
                    new ButtonDef(".", "Adv", Dark, White),
                    new ButtonDef("+/-", "Prt", Dark, White),
                    new ButtonDef("=", "List", Dark, Yellow),
                },
        };
      {
        for (int Row = 0; Row < NrButtonRows; ++Row)
          {
            for (int Col = 0; Col < NrButtonCols; ++Col)
              {
                ButtonDefs[Row][Col].BaseCode = (Row + 1) * 10 + Col + 1;
              } /*for*/
          } /*for*/
      }

    final RectF ButtonRelMargins = new RectF(0.07f, 0.5f, 0.07f, 0.05f);
      /* relative bounds of button within grid cell */
    final float CornerRoundness = 1.5f;

  /* global modifier state */
    public boolean AltState = false;
    public boolean InvState = false;

    int SelectedButton = -1;

    public State CalcState;
    int DigitsNeeded = 0;
    int AccumDigits = -1;
    boolean GotInd = false;
    boolean AcceptSymbolic = false;
    boolean AcceptInd = false;
    int CollectingForFunction = -1;

    public ButtonGrid
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
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
                          }
                    break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (SelectedButton != -1)
                          {
                            if (SelectedButton == 61 && CalcState != null && CalcState.ProgRunning)
                              {
                                CalcState.SetSlowExecution(false);
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
                final float OrigTextSize = TextPaint.getTextSize();
                TextPaint.setTextSize(OrigTextSize * 0.9f);
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
                TextPaint.setColor(ThisButton.TextColor);
                TextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                TextPaint.setTextSize(OrigTextSize * 1.1f);
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

    public void Invoke()
      {
        if (CalcState != null && SelectedButton > 0)
          {
            boolean WasModifier = false;
            boolean Handled = false;
            int ButtonCode;
            if (AltState)
              {
                ButtonCode = SelectedButton / 10 * 10 + (SelectedButton % 10 + 5) % 10;
              }
            else
              {
                ButtonCode = SelectedButton;
              } /*if*/
            if (CalcState.ProgRunning)
              {
                switch (ButtonCode)
                  {
                case 61:
                case 66: /*Pause, actually will always be 61 (GTO)*/
                    CalcState.SetSlowExecution(true);
                break;
                case 91: /*R/S*/
                case 96:
                    CalcState.StopProgram();
                break;
                  } /*switch*/
                Handled = true; /* ignore everything else */
              }
            else if (CollectingForFunction != -1)
              {
                boolean IsSymbolic = false;
                int Digit = -1;
                Handled = true; /* next assumption */
                switch (ButtonCode) /* collect digits */
                  {
                case 40: /*Ind*/
                    if (AcceptInd && AccumDigits < 0)
                      {
                        GotInd = !GotInd;
                      }
                    else
                      {
                        Handled = false;
                      } /*if*/
                break;
                case 21:
                case 26:
                  /* needed for Ind */
                    AltState = !AltState;
                    WasModifier = true;
                break;
                case 62:
                    Digit = 7;
                break;
                case 63:
                    Digit = 8;
                break;
                case 64:
                    Digit = 9;
                break;
                case 72:
                    Digit = 4;
                break;
                case 73:
                    Digit = 5;
                break;
                case 74:
                    Digit = 6;
                break;
                case 82:
                    Digit = 1;
                break;
                case 83:
                    Digit = 2;
                break;
                case 84:
                    Digit = 3;
                break;
                case 92:
                    Digit = 0;
                break;
                default:
                    if (AcceptSymbolic)
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
                    DigitsNeeded = 0;
                  } /*if*/
                if (DigitsNeeded == 0)
                  {
                    if (IsSymbolic || AccumDigits >= 0)
                      {
                      /* TBD if ProgMode, I should probably just enter the codes myself */
                        switch (CollectingForFunction)
                          {
                        case 36: /*Pgm*/
                            CalcState.SelectProgram(AccumDigits);
                        break;
                        case 42: /*STO*/
                            CalcState.MemoryOp(CalcState.MEMOP_STO, AccumDigits, GotInd);
                        break;
                        case 43: /*RCL*/
                            CalcState.MemoryOp(CalcState.MEMOP_RCL, AccumDigits, GotInd);
                        break;
                        case 44: /*SUM*/
                            CalcState.MemoryOp
                              (
                                InvState ?
                                    CalcState.MEMOP_SUB
                                :
                                    CalcState.MEMOP_ADD,
                                AccumDigits,
                                GotInd
                              );
                        break;
                        case 48: /*Exc*/
                            CalcState.MemoryOp(CalcState.MEMOP_EXC, AccumDigits, GotInd);
                        break;
                        case 49: /*Prd*/
                            CalcState.MemoryOp
                              (
                                InvState ?
                                    CalcState.MEMOP_DIV
                                :
                                    CalcState.MEMOP_MUL,
                                AccumDigits,
                                GotInd
                              );
                        break;
                        case 58: /*Fix*/
                          /* assert not InvState */
                            CalcState.SetDisplayMode(CalcState.FORMAT_FIXED, AccumDigits);
                        break;
                        case 67: /*x=t*/
                          /* TBD */
                        break;
                        case 69: /*Op*/
                            CalcState.SpecialOp(AccumDigits, GotInd);
                        break;
                        case 61: /*GTO*/
                          /* TBD */
                        break;
                        case 71: /*SBR*/
                          /* TBD */
                        break;
                        case 77: /*x≥t*/
                          /* TBD */
                        break;
                        case 86: /*St flg*/
                          /* TBD */
                        break;
                        case 97: /*Dsz*/
                          /* TBD */
                        break;
                        default:
                          /* shouldn't occur */
                            throw new RuntimeException("unhandled collected function " + CollectingForFunction);
                      /* break; */
                          } /*switch*/
                      }
                    else
                      {
                        CalcState.SetErrorState();
                        Handled = true;
                      } /*if*/
                    CollectingForFunction = -1;
                  } /*if*/
              } /*if CollectingForFunction*/
            if (!Handled)
              {
              /* check for functions needing further entry */
                Handled = true; /* next assumption */
                AcceptSymbolic = false; /* to begin with */
                AcceptInd = false;
                switch (ButtonCode)
                  {
                case 36: /* Pgm */
                case 42: /* STO */
                case 43: /* RCL */
                case 44: /* SUM */
                case 48: /* Exc */
                case 49: /* Prd */
                case 69:
                    DigitsNeeded = 2;
                    AcceptInd = true;
                break;
                case 58: /* Fix */
                    if (!InvState)
                      {
                        DigitsNeeded = 1;
                      }
                    else
                      {
                        Handled = false;
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
                    DigitsNeeded = 3;
                    AcceptInd = true;
                    AcceptSymbolic = true;
                break;
                case 86: /*Set flg*/
                case 87: /*If flg*/
                case 97: /* Dsz */
                    DigitsNeeded = 4; /* TBD check this */
                    AcceptInd = true;
                    AcceptSymbolic = true;
                break;
                default:
                  /* wasn't one of these after all */
                    Handled = false;
                break;
                  } /*ButtonCode*/
                if (Handled)
                  {
                    CollectingForFunction = ButtonCode;
                    GotInd = false;
                    AccumDigits = -1;
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
                if (CalcState.ProgMode)
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
                    switch (ButtonCode) /* special handling of program-editing/viewing functions and number entry */
                      {
                    case 21:
                    case 26:
                        AltState = !AltState;
                        WasModifier = true;
                    break;
                    case 22:
                        CalcState.StoreInstr(22);
                        InvState = !InvState;
                        WasModifier = true;
                    break;
                    case 31:
                        CalcState.SetProgMode(false);
                    break;
                  /* 40 handled above */
                    case 41:
                        CalcState.StepPC(true);
                    break;
                    case 46:
                        CalcState.InsertAtCurInstr();
                    break;
                    case 51:
                        CalcState.StepPC(false);
                    break;
                    case 56:
                        CalcState.DeleteCurInstr();
                    break;
                    case 62: /*digit 7*/
                    case 63: /*digit 8*/
                    case 64: /*digit 9*/
                        CalcState.StoreInstr(ButtonCode - 55);
                    break;
                    case 72: /*digit 4*/
                    case 73: /*digit 5*/
                    case 74: /*digit 6*/
                        CalcState.StoreInstr(ButtonCode - 68);
                    break;
                    case 82: /*digit 1*/
                    case 83: /*digit 2*/
                    case 84: /*digit 3*/
                        CalcState.StoreInstr(ButtonCode - 81);
                    break;
                    case 92: /*digit 0*/
                        CalcState.StoreInstr(0);
                    break;
                    default:
                        CalcState.StoreInstr(ButtonCode);
                    break;
                      } /*switch*/
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
                      /* TBD */
                    break;
                    case 21:
                    case 26:
                        AltState = !AltState;
                        WasModifier = true;
                    break;
                    case 22:
                    case 27:
                        InvState = !InvState;
                        WasModifier = true;
                    break;
                    case 23:
                        CalcState.Ln(InvState);
                    break;
                    case 24:
                        CalcState.ClearEntry();
                    break;
                    case 25:
                    case 20:
                        CalcState.ClearAll();
                    break;
                  /* 26 same as 21 */
                  /* 27 same as 22 */
                    case 28:
                        CalcState.Log(InvState);
                    break;
                    case 29:
                        CalcState.ClearProgram();
                    break;
                  /* 20 same as 25 */
                    case 31:
                        CalcState.SetProgMode(true);
                    break;
                    case 32:
                        CalcState.SwapT();
                    break;
                    case 33:
                        CalcState.Square();
                    break;
                    case 34:
                        CalcState.Sqrt();
                    break;
                    case 35:
                        CalcState.Reciprocal();
                    break;
                  /* 36 handled above */
                    case 37:
                        CalcState.Polar(InvState);
                    break;
                    case 38:
                        CalcState.Sin(InvState);
                    break;
                    case 39:
                        CalcState.Cos(InvState);
                    break;
                    case 30:
                        CalcState.Tan(InvState);
                    break;
                    case 41:
                        CalcState.StepProgram();
                    break;
                  /* 42, 43, 44 handled above */
                    case 45:
                        CalcState.Operator(InvState ? CalcState.STACKOP_ROOT : CalcState.STACKOP_EXP);
                    break;
                    case 46:
                      /* ignore? */
                    break;
                    case 47:
                        CalcState.ClearMemories();
                    break;
                  /* 48, 49, 40 handled above */
                    case 51:
                      /* ignore? */
                    break;
                    case 52:
                        CalcState.EnterExponent(InvState);
                    break;
                    case 53:
                        CalcState.LParen();
                    break;
                    case 54:
                        CalcState.RParen();
                    break;
                    case 55:
                        CalcState.Operator(CalcState.STACKOP_DIV);
                    break;
                    case 56:
                      /* ignore? */
                    break;
                    case 57:
                        CalcState.SetDisplayMode
                          (
                            InvState ? CalcState.FORMAT_FIXED : CalcState.FORMAT_ENG,
                            -1
                          );
                    break;
                    case 58:
                      /* assert InvState */
                        CalcState.SetDisplayMode(CalcState.FORMAT_FIXED, -1);
                    break;
                    case 59:
                        CalcState.Int(InvState);
                    break;
                    case 50:
                        CalcState.Abs();
                    break;
                  /* 61 handled above */
                    case 62:
                        CalcState.Digit('7');
                    break;
                    case 63:
                        CalcState.Digit('8');
                    break;
                    case 64:
                        CalcState.Digit('9');
                    break;
                    case 65:
                        CalcState.Operator(CalcState.STACKOP_MUL);
                    break;
                    case 66:
                      /* ignore */
                    break;
                  /* 67 handled above */
                    case 68:
                        CalcState.Nop();
                    break;
                  /* 69 handled above */
                    case 60:
                        CalcState.SetAngMode(CalcState.ANG_DEG);
                    break;
                  /* 71 handled above */
                    case 72:
                        CalcState.Digit('4');
                    break;
                    case 73:
                        CalcState.Digit('5');
                    break;
                    case 74:
                        CalcState.Digit('6');
                    break;
                    case 75:
                        CalcState.Operator(CalcState.STACKOP_SUB);
                    break;
                    case 76:
                      /* ignore? */
                    break;
                  /* 77 handled above */
                    case 78:
                        CalcState.StatsSum(InvState);
                    break;
                    case 79:
                        CalcState.StatsResult(InvState);
                    break;
                    case 70:
                        CalcState.SetAngMode(CalcState.ANG_RAD);
                    break;
                    case 81:
                        CalcState.Reset();
                    break;
                    case 82:
                        CalcState.Digit('1');
                    break;
                    case 83:
                        CalcState.Digit('2');
                    break;
                    case 84:
                        CalcState.Digit('3');
                    break;
                    case 85:
                        CalcState.Operator(CalcState.STACKOP_ADD);
                    break;
                  /* 86, 87 handled above */
                    case 88:
                        CalcState.D_MS(InvState);
                    break;
                    case 89:
                        CalcState.Pi();
                    break;
                    case 80:
                        CalcState.SetAngMode(CalcState.ANG_GRAD);
                    break;
                    case 91:
                    case 96:
                        CalcState.StartProgram();
                    break;
                    case 92:
                        CalcState.Digit('0');
                    break;
                    case 93:
                        CalcState.DecimalPoint();
                    break;
                    case 94:
                        CalcState.ChangeSign();
                    break;
                    case 95:
                        CalcState.Equals();
                    break;
                  /* 96 same as 91 */
                  /* 97 handled above */
                    case 98:
                      /* TBD */
                    break;
                    case 99:
                      /* TBD */
                    break;
                    case 90:
                      /* TBD */
                    break;
                      } /*switch*/
                  } /*if ProgMode*/
              } /*if not Handled*/
            if (!WasModifier)
              {
                AltState = false;
              } /*if*/
            if (CalcState.InErrorState())
              {
                CollectingForFunction = -1; /* abandon */ /* is this necessar? */
              } /*if*/
            if (!WasModifier && CollectingForFunction < 0)
              {
                InvState = false;
              } /*if*/
          } /*if*/
      } /*Invoke*/

  } /*ButtonGrid*/
