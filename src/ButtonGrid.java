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

    int SelectedButton = -1;

    public State CalcState;
    int DigitsNeeded;
    boolean AcceptSymbolic, AcceptInd, NextLiteral;
    int AccumDigits, FirstOperand;
    boolean GotFirstOperand, GotFirstInd, IsSymbolic, GotInd;
    int CollectingForFunction;
    int ButtonCode;

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
        ResetOperands();
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

    void StoreInv()
      /* prefixes the next instruction with an INV opcode if appropriate. */
      {
        if (CalcState.InvState)
          {
            CalcState.StoreInstr(22);
          } /*if*/
      } /*StoreInv*/

    void StoreOperand
      (
        int NrDigits, /* 1, 2 or 3 */
        boolean SeparateInd
      )
      /* stores operand for a program instruction. */
      {
        if (SeparateInd && GotInd)
          {
            CalcState.StoreInstr(40);
          } /*if*/
        if (IsSymbolic)
          {
            CalcState.StoreInstr(ButtonCode);
          }
        else
          {
            if (NrDigits < 3 || GotInd)
              {
                CalcState.StoreInstr(AccumDigits);
              }
            else
              {
                CalcState.StoreInstr(AccumDigits / 100);
                CalcState.StoreInstr(AccumDigits % 100);
              } /*if*/
          } /*if*/
      } /*StoreOperand*/

    public void Invoke()
      {
        if (CalcState != null && SelectedButton > 0)
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
                        CalcState.SetErrorState();
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
                        CalcState.SetErrorState();
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
                        CalcState.SetErrorState();
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
                        CalcState.SetErrorState();
                      }
                    else
                      {
                        Digit = ButtonCode - 81;
                      } /*if*/
                break;
                case 92: /*digit 0*/
                    if (NextLiteral)
                      {
                        CalcState.SetErrorState();
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
                if (AccumDigits >= 0 && DigitsNeeded == 0 || IsSymbolic)
                  {
                    boolean Finished = true; /* to begin with */
                    if (IsSymbolic || AccumDigits >= 0)
                      {
                        if (CalcState.ProgMode)
                          {
                            StoreInv(); /* even if it's ignored by some of them */
                          } /*if*/
                        switch (CollectingForFunction)
                          {
                        case 36: /*Pgm*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 62 : 36);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.SelectProgram(AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 42: /*STO*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 72 : 42);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.MemoryOp(CalcState.MEMOP_STO, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 43: /*RCL*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 73 : 43);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.MemoryOp(CalcState.MEMOP_RCL, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 44: /*SUM*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 74 : 44);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.MemoryOp
                                  (
                                    CalcState.InvState ?
                                        CalcState.MEMOP_SUB
                                    :
                                        CalcState.MEMOP_ADD,
                                    AccumDigits,
                                    GotInd
                                  );
                              } /*if*/
                        break;
                        case 48: /*Exc*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 63 : 48);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.MemoryOp(CalcState.MEMOP_EXC, AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 49: /*Prd*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 64 : 49);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.MemoryOp
                                  (
                                    CalcState.InvState ?
                                        CalcState.MEMOP_DIV
                                    :
                                        CalcState.MEMOP_MUL,
                                    AccumDigits,
                                    GotInd
                                  );
                              } /*if*/
                        break;
                        case 58: /*Fix*/
                          /* assert not CalcState.InvState */
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(58);
                                StoreOperand(1, true);
                              }
                            else
                              {
                                CalcState.SetDisplayMode(CalcState.FORMAT_FIXED, AccumDigits);
                              } /*if*/
                        break;
                        case 67: /*x=t*/
                        case 77: /*x≥t*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(CollectingForFunction);
                                StoreOperand(3, true);
                              }
                            else
                              {
                                CalcState.CompareBranch
                                  (
                                    CalcState.InvState,
                                    CollectingForFunction == 77,
                                    AccumDigits, GotInd
                                  );
                              } /*if*/
                        break;
                        case 69: /*Op*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 84 : 69);
                                StoreOperand(2, false);
                              }
                            else
                              {
                                CalcState.SpecialOp(AccumDigits, GotInd);
                              } /*if*/
                        break;
                        case 61: /*GTO*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(GotInd ? 83 : 61);
                                StoreOperand(3, false);
                              }
                            else
                              {
                                CalcState.FillInLabels();
                                CalcState.Transfer(false, IsSymbolic ? ButtonCode : AccumDigits, IsSymbolic, GotInd);
                              } /*if*/
                        break;
                        case 71: /*SBR*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(71);
                                StoreOperand(3, true);
                              }
                            else
                              {
                                CalcState.FillInLabels();
                                CalcState.Transfer(true, IsSymbolic ? ButtonCode : AccumDigits, IsSymbolic, GotInd);
                              } /*if*/
                        break;
                        case 76: /*Lbl*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(76);
                                CalcState.StoreInstr(ButtonCode); /* always symbolic */
                              }
                            else
                              {
                              /* ignore */
                              } /*if*/
                        break;
                        case 86: /*St flg*/
                            if (CalcState.ProgMode)
                              {
                                CalcState.StoreInstr(86);
                                StoreOperand(1, true);
                              }
                            else
                              {
                                CalcState.SetFlag(AccumDigits, GotInd, !CalcState.InvState);
                              } /*if*/
                        break;
                        case 87: /*If flg*/
                        case 97: /*Dsz*/
                            if (GotFirstOperand)
                              {
                                if (CalcState.ProgMode)
                                  {
                                    CalcState.StoreInstr(CollectingForFunction);
                                    if (GotFirstInd)
                                      {
                                        CalcState.StoreInstr(40);
                                      } /*if*/
                                    CalcState.StoreInstr(FirstOperand);
                                    StoreOperand(3, true);
                                  }
                                else
                                  {
                                    if (CollectingForFunction == 87)
                                      {
                                        CalcState.BranchIfFlag
                                          (
                                            CalcState.InvState,
                                            FirstOperand, GotFirstInd,
                                            AccumDigits, IsSymbolic, GotInd
                                          );
                                      }
                                    else
                                      {
                                        CalcState.DecrementSkip
                                          (
                                            CalcState.InvState,
                                            FirstOperand, GotFirstInd,
                                            AccumDigits, IsSymbolic, GotInd
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
                        CalcState.SetErrorState();
                        Handled = true;
                      } /*if*/
                    if (Finished)
                      {
                        CollectingForFunction = -1;
                        ResetOperands();
                      } /*if*/
                  } /*if*/
              } 
                /*if CollectingForFunction*/
            if (!Handled)
              {
              /* check for functions needing further entry */
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
                    if (!CalcState.InvState)
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
                    if (ButtonCode == 71 && CalcState.InvState)
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
                        CalcState.InvState = !CalcState.InvState;
                        WasModifier = true;
                    break;
                    case 31: /*LRN*/
                        CalcState.SetProgMode(false);
                        ResetOperands();
                    break;
                  /* 40 handled above */
                    case 41: /*SST*/
                        CalcState.StepPC(true);
                        ResetOperands();
                    break;
                    case 46: /*Ins*/
                        CalcState.InsertAtCurInstr();
                        ResetOperands();
                    break;
                    case 51: /*BST*/
                        CalcState.StepPC(false);
                        ResetOperands();
                    break;
                    case 56: /*Del*/
                        CalcState.DeleteCurInstr();
                        ResetOperands();
                    break;
                    case 62: /*digit 7*/
                    case 63: /*digit 8*/
                    case 64: /*digit 9*/
                        CalcState.StoreInstr(ButtonCode - 55);
                    break;
                    case 71: /*SBR*/
                        if (CalcState.InvState)
                          {
                            CalcState.StorePrevInstr(92);
                          } /*if*/
                      /* else handled above */
                    break;
                    case 72: /*digit 4*/
                    case 73: /*digit 5*/
                    case 74: /*digit 6*/
                        CalcState.StoreInstr(ButtonCode - 68);
                    break;
                  /* 76 handled above */
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
                        CalcState.FillInLabels();
                        CalcState.Transfer(true, ButtonCode, true, false);
                    break;
                    case 21:
                    case 26:
                        AltState = !AltState;
                        WasModifier = true;
                    break;
                    case 22:
                    case 27:
                        CalcState.InvState = !CalcState.InvState;
                        WasModifier = true;
                    break;
                    case 23:
                        CalcState.Ln(CalcState.InvState);
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
                        CalcState.Log(CalcState.InvState);
                    break;
                    case 29:
                        CalcState.ClearProgram();
                    break;
                  /* 20 same as 25 */
                    case 31: /*LRN*/
                        CalcState.SetProgMode(true);
                        ResetOperands();
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
                        CalcState.Polar(CalcState.InvState);
                    break;
                    case 38:
                        CalcState.Sin(CalcState.InvState);
                    break;
                    case 39:
                        CalcState.Cos(CalcState.InvState);
                    break;
                    case 30:
                        CalcState.Tan(CalcState.InvState);
                    break;
                    case 41:
                        CalcState.StepProgram();
                    break;
                  /* 42, 43, 44 handled above */
                    case 45:
                        CalcState.Operator(CalcState.InvState ? CalcState.STACKOP_ROOT : CalcState.STACKOP_EXP);
                    break;
                    case 46:
                      /* ignore? */
                    break;
                    case 47:
                        CalcState.ClearMemories();
                    break;
                  /* 48, 49, 40 handled above */
                    case 51:
                      /* ignore */
                    break;
                    case 52:
                        CalcState.EnterExponent(CalcState.InvState);
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
                      /* ignore */
                    break;
                    case 57:
                        CalcState.SetDisplayMode
                          (
                            CalcState.InvState ? CalcState.FORMAT_FIXED : CalcState.FORMAT_ENG,
                            -1
                          );
                    break;
                    case 58:
                      /* assert CalcState.InvState */
                        CalcState.SetDisplayMode(CalcState.FORMAT_FIXED, -1);
                    break;
                    case 59:
                        CalcState.Int(CalcState.InvState);
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
                    case 66: /*Pause*/
                      /* ignore */
                    break;
                  /* 67 handled above */
                    case 68: /*Nop*/
                      /* No effect */
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
                      /* ignore */
                    break;
                  /* 77 handled above */
                    case 78:
                        CalcState.StatsSum(CalcState.InvState);
                    break;
                    case 79:
                        CalcState.StatsResult(CalcState.InvState);
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
                        CalcState.D_MS(CalcState.InvState);
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
                ResetOperands(); /* abandon */ /* is this necessary? */
              } /*if*/
            if (!WasModifier && CollectingForFunction < 0)
              {
                CalcState.InvState = false;
              } /*if*/
          } /*if*/
      } /*Invoke*/

  } /*ButtonGrid*/
