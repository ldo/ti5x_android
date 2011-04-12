package nz.gen.geek_central.ti5x;

public class State
  /* the calculation state and number entry */
  {
    public final static int EntryState = 0;
    public final static int DecimalEntryState = 1;
    public final static int ExponentEntryState = 2;
    public final static int ResultState = 10;
    public final static int ErrorState = 11;
    public int CurState = EntryState;
    public boolean ExponentEntered = false;

    public boolean InvState = false;

    Display TheDisplay;
    String CurDisplay;
    android.os.Handler BGTask;
    Runnable DelayTask = null;
    Runnable ExecuteTask = null;

    public static final int FORMAT_FIXED = 0;
    public static final int FORMAT_FLOAT = 1;
    public static final int FORMAT_ENG = 2;
    public int CurFormat = FORMAT_FIXED;
    public int CurNrDecimals = -1;

    public static final int ANG_RAD = 1;
    public static final int ANG_DEG = 2;
    public static final int ANG_GRAD = 3;
    public int CurAng = ANG_DEG;

    public final static int STACKOP_ADD = 1;
    public final static int STACKOP_SUB = 2;
    public final static int STACKOP_MUL = 3;
    public final static int STACKOP_DIV = 4;
    public final static int STACKOP_EXP = 5;
    public final static int STACKOP_ROOT = 6;

    public static class OpStackEntry
      {
        double Operand;
        int Operator;
        int ParenFollows;

        public OpStackEntry
          (
            double Operand,
            int Operator,
            int ParenFollows
          )
          {
            this.Operand = Operand;
            this.Operator = Operator;
            this.ParenFollows = ParenFollows;
          } /*OpStackEntry*/

      } /*OpStackEntry*/

    public final int MaxOpStack = 8;
    public double X, T;
    public OpStackEntry[] OpStack;
    public int OpStackNext;

    public boolean ProgMode;
    public final int MaxMemories = 30; /* TBD make this configurable */ /* can't be zero */
    public final int MaxProgram = 480; /* TBD make this configurable */
    public final int MaxFlags = 10;
    public final double[] Memory;
    public final byte[] Program;
    public final boolean[] Flag;
    public int PC;
    final java.util.Map<Integer, Integer> Labels; /* mapping from symbolic codes to program locations */
    boolean GotLabels;
    public boolean ProgRunning = false;
    public boolean ProgRunningSlowly = false;

    public static class ReturnStackEntry
      {
        public int Addr;
        public boolean FromInteractive;

        public ReturnStackEntry
          (
            int Addr,
            boolean FromInteractive
          )
          {
            this.Addr = Addr;
            this.FromInteractive = FromInteractive;
          } /*ReturnStackEntry*/

      } /*ReturnStackEntry*/

    public final int MaxReturnStack = 6;
    public ReturnStackEntry[] ReturnStack;
    public int ReturnLast;

    String LastShowing = null;

    public void Reset()
      /* resets to power-up/blank state. */
      {
        OpStackNext = 0;
        X = 0.0;
        T = 0.0;
        PC = 0;
        ReturnLast = -1;
        GotLabels = false;
        for (int i = 0; i < MaxFlags; ++i)
          {
            Flag[i] = false;
          } /*for*/
        for (int i = 0; i < MaxMemories; ++i)
          {
            Memory[i] = 0.0;
          } /*for*/
        for (int i = 0; i < MaxProgram; ++i)
          {
            Program[i] = (byte)0;
          } /*if*/
        ProgMode = false;
        ResetEntry();
      } /*Reset*/

    public State
      (
        Display TheDisplay
      )
      {
        this.TheDisplay = TheDisplay;
        OpStack = new OpStackEntry[MaxOpStack];
        Memory = new double[MaxMemories];
        Program = new byte[MaxProgram];
        Flag = new boolean[MaxFlags];
        BGTask = new android.os.Handler();
        ReturnStack = new ReturnStackEntry[MaxReturnStack];
        Labels = new java.util.HashMap<Integer, Integer>();
        Reset();
      } /*State*/

    class DelayedStep implements Runnable
      {
        public void run()
          {
            ShowCurProg();
          } /*run*/
      } /*DelayedStep*/

    void ClearDelayedStep()
      {
        if (DelayTask != null)
          {
            BGTask.removeCallbacks(DelayTask);
          } /*if*/
      } /*ClearDelayedStep*/

    void SetShowing
      (
        String ToDisplay
      )
      {
        ClearDelayedStep();
        LastShowing = ToDisplay;
        if (!ProgRunning || ProgRunningSlowly)
          {
            TheDisplay.SetShowing(ToDisplay);
          }
      } /*SetShowing*/

    public void ResetEntry()
      {
        CurState = EntryState;
        if (CurFormat == FORMAT_FIXED)
          {
            CurDisplay = "0.";
            ExponentEntered = false;
          }
        else
          {
            CurDisplay = "0. 00";
            ExponentEntered = true;
          } /*if*/
        SetShowing(CurDisplay);
      } /*ResetEntry*/

    public void Enter()
      /* finishes the entry of the current number. */
      {
        if (CurState != ResultState && CurState != ErrorState)
          {
            int Exp;
            if (ExponentEntered)
              {
                Exp = Integer.parseInt(CurDisplay.substring(CurDisplay.length() - 2));
                if (CurDisplay.charAt(CurDisplay.length() - 3) == '-')
                  {
                    Exp = - Exp;
                  } /*if*/
              }
            else
              {
                Exp = 0;
              } /*if*/
            X = Double.parseDouble
              (
                CurDisplay.substring
                  (
                    0,
                    ExponentEntered ? CurDisplay.length() - 3 : CurDisplay.length()
                  )
              );
            if (ExponentEntered)
              {
                X = X * Math.pow(10.0, Exp);
              } /*if*/
            SetX(X);
          } /*if*/
      } /*Enter*/

    public void SetErrorState()
      {
        ClearDelayedStep();
        if (!ProgRunning || ProgRunningSlowly)
          {
            TheDisplay.SetShowingError();
          } /*if*/
        CurState = ErrorState;
      } /*SetErrorState*/

    public boolean InErrorState()
      {
        return
            CurState == ErrorState;
      } /*InErrorState*/

    public void ClearAll()
      {
        OpStackNext = 0;
        ResetEntry();
      } /*ClearAll*/

    public void ClearEntry()
      {
        if (CurState != ResultState)
          {
            ResetEntry();
          } /*if*/
      } /*ClearEntry*/

    public void Digit
      (
        char TheDigit
      )
      {
        if (CurState == ResultState)
          {
            ResetEntry();
          } /*if*/
        String SaveExponent = "";
        switch (CurState)
          {
        case EntryState:
        case DecimalEntryState:
            if (ExponentEntered)
              {
                SaveExponent = CurDisplay.substring(CurDisplay.length() - 3);
                CurDisplay = CurDisplay.substring(0, CurDisplay.length() - 3);
              } /*if*/
        break;
          } /*switch*/
        switch (CurState)
          {
        case EntryState:
            if (CurDisplay.charAt(0) == '0')
              {
                CurDisplay = new String(new char[] {TheDigit}) + CurDisplay.substring(1);
              }
            else if (CurDisplay.charAt(0) == '-' && CurDisplay.charAt(1) == '0')
              {
                CurDisplay = "-" + new String(new char[] {TheDigit}) + CurDisplay.substring(2);
              }
            else
              {
                CurDisplay =
                        CurDisplay.substring(0,CurDisplay.length() - 1)
                    +
                        new String(new char[] {TheDigit})
                    +
                        CurDisplay.substring(CurDisplay.length() - 1);
              } /*if*/
        break;
        case DecimalEntryState:
            CurDisplay = CurDisplay + new String(new char[] {TheDigit});
        break;
        case ExponentEntryState:
          /* old exponent units digit becomes tens digit, new digit
            becomes units digit */
            CurDisplay =
                    CurDisplay.substring(0, CurDisplay.length() - 2)
                +
                    CurDisplay.substring(CurDisplay.length() - 1)
                +
                    new String(new char[] {TheDigit});
        break;
          } /*switch*/
        CurDisplay += SaveExponent;
        if (CurState != ErrorState)
          {
            SetShowing(CurDisplay);
          } /*if*/
      } /*Digit*/

    public void DecimalPoint()
      {
        if (CurState == ResultState)
          {
            ResetEntry();
          } /*if*/
        switch (CurState)
          {
        case EntryState:
        case ExponentEntryState:
            CurState = DecimalEntryState;
        break;
      /* otherwise ignore */
          } /*CurState*/
      } /*DecimalPoint*/

    public void EnterExponent()
      {
      /* InvState TBD */
        switch (CurState)
          {
        case EntryState:
        case DecimalEntryState:
            if (!ExponentEntered)
              {
                CurDisplay = CurDisplay + " 00";
              } /*if*/
            CurState = ExponentEntryState;
            SetShowing(CurDisplay);
            ExponentEntered = true;
        break;
        case ResultState:
            if (CurFormat != FORMAT_FLOAT)
              {
                CurFormat = FORMAT_FLOAT;
                SetX(X); /* will cause redisplay */
              } /*if*/
        break;
          } /*switch*/
      } /*EnterExponent*/

    public void SetX
      (
        double NewX
      )
      /* sets the display to show the specified value. */
      {
        int UseFormat = CurFormat;
        int Exp, BeforeDecimal;
        CurState = ResultState;
        X = NewX;
        if (!Double.isNaN(X) && !Double.isInfinite(X))
          {
            if
              (
                    CurFormat == FORMAT_FIXED
                &&
                    X != 0
                &&
                    (Math.abs(X) < 5.0 * Math.pow(10.0, -11.0) || Math.abs(X) > Math.pow(10.0, 10.0))
              )
              {
                UseFormat = FORMAT_FLOAT;
              } /*if*/
            Exp = 0;
            if (X != 0.0)
              {
                switch (UseFormat)
                  {
                case FORMAT_FLOAT:
                    Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(10.0));
                break;
                case FORMAT_ENG:
                    Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(1000.0)) * 3;
                break;
                  } /*switch*/
              } /*if*/
            if (X != 0.0)
              {
                BeforeDecimal = Math.max((int)Math.floor(Math.log10(X / Math.pow(10.0, Exp))), 1);
              }
            else
              {
                BeforeDecimal = 1;
              } /*if*/
            switch (UseFormat)
              {
            case FORMAT_FLOAT:
            case FORMAT_ENG:
                CurDisplay = String.format
                  (
                    java.util.Locale.US,
                    String.format(java.util.Locale.US, "%%.%df", Math.max(8 - BeforeDecimal, 0)),
                    X / Math.pow(10.0, Exp)
                  );
            break;
            case FORMAT_FIXED:
                if (CurNrDecimals >= 0)
                  {
                    CurDisplay = String.format
                      (
                        java.util.Locale.US,
                        String.format(java.util.Locale.US, "%%.%df", Math.max(CurNrDecimals + 1 - BeforeDecimal, 0)),
                        X / Math.pow(10.0, Exp)
                      );
                  }
                else
                  {
                    CurDisplay = String.format
                      (
                        java.util.Locale.US,
                        String.format(java.util.Locale.US, "%%.%df", Math.max(11 - BeforeDecimal, 0)),
                        X / Math.pow(10.0, Exp)
                      );
                    while
                      (
                            CurDisplay.length() != 0
                        &&
                            CurDisplay.charAt(CurDisplay.length() - 1) == '0'
                      )
                      {
                        CurDisplay = CurDisplay.substring(0, CurDisplay.length() - 1);
                      } /*while*/
                  } /*if*/
            break;
              } /*switch*/
            if (CurDisplay.length() == 0)
              {
                CurDisplay = "0.";
              } /*if*/
          /* assume there will always be a decimal point? */
            if (UseFormat != FORMAT_FIXED)
              {
                CurDisplay += (Exp < 0 ? "-" : " ") + String.format(java.util.Locale.US, "%02d", Math.abs(Exp));
              } /*if*/
            SetShowing(CurDisplay);
          }
        else
          {
            SetErrorState();
          } /*if*/
      } /*SetX*/

    public void ChangeSign()
      {
        switch (CurState)
          {
        case EntryState:
        case DecimalEntryState:
            if (CurDisplay.charAt(0) == '-')
              {
                CurDisplay = CurDisplay.substring(1);
              }
            else
              {
                CurDisplay = "-" + CurDisplay;
              } /*if*/
            SetShowing(CurDisplay);
        break;
        case ExponentEntryState:
            CurDisplay =
                    CurDisplay.substring(0, CurDisplay.length() - 3)
                +
                    (CurDisplay.charAt(CurDisplay.length() - 3) == '-' ? ' ' : '-')
                +
                    CurDisplay.substring(CurDisplay.length() - 2);
            SetShowing(CurDisplay);
        break;
        case ResultState:
            SetX(- X);
        break;
          } /*switch*/
      } /*ChangeSign*/

    public void SetDisplayMode
      (
        int NewMode,
        int NewNrDecimals
      )
      {
        Enter();
        CurFormat = NewMode;
        CurNrDecimals = NewNrDecimals;
        SetX(X);
      } /*SetDisplayMode*/

    void DoStackTop()
      {
        final OpStackEntry ThisOp = OpStack[--OpStackNext];
        switch (ThisOp.Operator)
          {
        case STACKOP_ADD:
            X = ThisOp.Operand + X;
        break;
        case STACKOP_SUB:
            X = ThisOp.Operand - X;
        break;
        case STACKOP_MUL:
            X = ThisOp.Operand * X;
        break;
        case STACKOP_DIV:
            X = ThisOp.Operand / X;
        break;
        case STACKOP_EXP:
            X = Math.pow(ThisOp.Operand, X);
        break;
        case STACKOP_ROOT:
            X = Math.pow(ThisOp.Operand, 1.0 / X);
        break;
          } /*switch*/
      /* leave it to caller to update display */
      } /*DoStackTop*/

    static int Precedence
      (
        int OpCode
      )
      {
        int Result = -1;
        switch (OpCode)
          {
        case STACKOP_ADD:
        case STACKOP_SUB:
            Result = 1;
        break;
        case STACKOP_MUL:
        case STACKOP_DIV:
            Result = 2;
        break;
        case STACKOP_EXP:
        case STACKOP_ROOT:
            Result = 3;
        break;
          } /*switch*/
        return
            Result;
      } /*Precedence*/

    void StackPush
      (
        int OpCode
      )
      {
        if (OpStackNext == MaxOpStack)
          {
          /* overflow! */
            SetErrorState();
          }
        else
          {
            OpStack[OpStackNext++] = new OpStackEntry(X, OpCode, 0);
          } /*if*/
      } /*StackPush*/

    public void Operator
      (
        int OpCode
      )
      {
        Enter();
        if (InvState)
          {
            switch (OpCode)
              {
            case STACKOP_EXP:
                OpCode = STACKOP_ROOT;
            break;
              } /*switch*/
          } /*if*/
        boolean PoppedSomething = false;
        for (;;)
          {
            if
              (
                    OpStackNext == 0
                ||
                    OpStack[OpStackNext - 1].ParenFollows != 0
                ||
                    Precedence(OpStack[OpStackNext - 1].Operator) < Precedence(OpCode)
              )
                break;
            DoStackTop();
            PoppedSomething = true;
          } /*for*/
        if (PoppedSomething)
          {
            SetX(X);
          } /*if*/
        StackPush(OpCode);
      } /*Operator*/

    public void LParen()
      {
        Enter();
        if (OpStackNext != 0)
          {
            ++OpStack[OpStackNext - 1].ParenFollows;
          } /*if*/
      /* else ignored */
      } /*LParen*/

    public void RParen()
      {
        Enter();
        boolean PoppedSomething = false;
        for (;;)
          {
            if (OpStackNext == 0)
                break;
            if (OpStack[OpStackNext - 1].ParenFollows != 0)
              {
                --OpStack[OpStackNext - 1].ParenFollows;
                break;
              } /*if*/
            DoStackTop();
            PoppedSomething = true;
          } /*for*/
        if (PoppedSomething)
          {
            SetX(X);
          } /*if*/
      } /*RParen*/

    public void Equals()
      {
        Enter();
        while (OpStackNext != 0)
          {
            DoStackTop();
          } /*while*/
        SetX(X);
      } /*Equals*/

    public void SetAngMode
      (
        int NewMode
      )
      {
        CurAng = NewMode;
      } /*SetAngMode*/

    public void Square()
      {
        Enter();
        SetX(X * X);
      } /*Square*/

    public void Sqrt()
      {
        Enter();
        SetX(Math.sqrt(X));
      } /*Sqrt*/

    public void Reciprocal()
      {
        Enter();
        SetX(1.0 / X);
      } /*Reciprocal*/

    double TrigScale()
      {
        Double Scale = 0.0;
        switch (CurAng)
          {
        case ANG_RAD:
            Scale = 1.0;
        break;
        case ANG_DEG:
            Scale = 180.0 / Math.PI;
        break;
        case ANG_GRAD:
            Scale = 200.0 / Math.PI;
        break;
          } /*CurAng*/
        return
            Scale;
      } /*TrigScale*/

    public void Sin()
      {
        Enter();
        if (InvState)
          {
            SetX(Math.asin(X) * TrigScale());
          }
        else
          {
            SetX(Math.sin(X / TrigScale()));
          } /*if*/
      } /*Sin*/

    public void Cos()
      {
        Enter();
        if (InvState)
          {
            SetX(Math.acos(X) * TrigScale());
          }
        else
          {
            SetX(Math.cos(X / TrigScale()));
          } /*if*/
      } /*Cos*/

    public void Tan()
      {
        Enter();
        if (InvState)
          {
            SetX(Math.atan(X) * TrigScale());
          }
        else
          {
            SetX(Math.tan(X / TrigScale()));
          } /*if*/
      } /*Tan*/

    public void Ln()
      {
        Enter();
        if (InvState)
          {
            SetX(Math.exp(X));
          }
        else
          {
            SetX(Math.log(X));
          } /*if*/
      } /*Ln*/

    public void Log()
      {
        Enter();
        if (InvState)
          {
            SetX(Math.pow(10.0, X));
          }
        else
          {
            SetX(Math.log10(X));
          } /*if*/
      } /*Log*/

    public void Pi()
      {
        SetX(Math.PI);
      } /*Pi*/

    public void Int()
      {
        Enter();
        final Double IntPart = Math.floor(Math.abs(X));
        if (InvState)
          {
            SetX((Math.abs(X) - IntPart) * Math.signum(X));
          }
        else
          {
            SetX(IntPart * Math.signum(X));
          } /*if*/
      } /*Int*/

    public void Abs()
      {
        SetX(Math.abs(X));
      } /*Abs*/

    public void SwapT()
      {
        Enter();
        final double SwapTemp = X;
        SetX(T);
        T = SwapTemp;
      } /*SwapT*/

    public void Polar()
      {
        Enter();
        final Double Scale = TrigScale();
        Double NewX, NewY;
        if (InvState)
          {
            NewX = Math.sqrt(X * X + T * T);
            NewY = Math.atan2(X, T) * Scale;
          }
        else
          {
            NewX = T * Math.cos(X / Scale);
            NewY = T * Math.sin(X / Scale);
          } /*if*/
        T = NewX;
        SetX(NewY);
      } /*Polar*/

    public void D_MS()
      {
        Enter();
        final Double Sign = Math.signum(X);
        final Double Degrees = Math.floor(Math.abs(X));
        final Double Fraction = Math.abs(X) - Degrees;
        if (InvState)
          {
            final double Minutes = Math.floor(Fraction * 60.0 + 0.1 /*fudge for rounding errors */);
            SetX((Degrees + Minutes / 100.0 + (Fraction * 60.0 - Minutes) * 6 / 1000.0) * Sign);
          }
        else
          {
            final Double Minutes = Math.floor(Fraction * 100.0 + 0.1 /*fudge for rounding errors */);
            SetX((Degrees + Minutes / 60.0 + (Fraction * 100.0 - Minutes) / 36.0) * Sign);
          } /*if*/
      } /*D_MS*/

    void ShowCurProg()
      {
        SetShowing(String.format(java.util.Locale.US, "%03d %02d", PC, (int)Program[PC]));
      } /*ShowCurProg*/

    public void SetProgMode
      (
        boolean NewProgMode
      )
      {
        ProgMode = NewProgMode;
        if (ProgMode)
          {
            ShowCurProg();
          }
        else
          {
            SetShowing(CurDisplay);
          } /*if*/
      } /*SetProgMode*/

    public void ClearMemories()
      {
        Enter(); /*?*/
        for (int i = 0; i < MaxMemories; ++i)
          {
            Memory[i] = 0.0;
          } /*for*/
      } /*ClearMemories*/

    public void ClearProgram()
      {
        Enter(); /*?*/
        for (int i = 0; i < MaxProgram; ++i)
          {
            Program[i] = (byte)0;
          } /*if*/
        PC = 0;
        ReturnLast = -1;
        T = 0.0;
        for (int i = 0; i < MaxFlags; ++i)
          {
            Flag[i] = false;
          } /*for*/
      } /*ClearProgram*/

    public void SelectProgram
      (
        int ProgNr,
        boolean Indirect
      )
      {
      /* TBD */
      } /*SelectProgram*/

    public static final int MEMOP_STO = 1;
    public static final int MEMOP_RCL = 2;
    public static final int MEMOP_ADD = 3;
    public static final int MEMOP_SUB = 4;
    public static final int MEMOP_MUL = 5;
    public static final int MEMOP_DIV = 6;
    public static final int MEMOP_EXC = 7;

    public void MemoryOp
      (
        int Op,
        int RegNr,
        boolean Indirect
      )
      {
        if (RegNr >= 0)
          {
            Enter();
            if (InvState)
              {
                switch (Op)
                  {
                case MEMOP_ADD:
                    Op = MEMOP_SUB;
                break;
                case MEMOP_MUL:
                    Op = MEMOP_DIV;
                break;
                  } /*switch*/
              } /*if*/
            boolean OK = false; /* to begin with */
            do /*once*/
              {
                if (RegNr >= MaxMemories)
                    break;
                if (Indirect)
                  {
                    RegNr = (int)Math.round(Memory[RegNr]);
                    if (RegNr < 0 || RegNr >= MaxMemories)
                        break;
                  } /*if*/
                switch (Op)
                  {
                case MEMOP_STO:
                    Memory[RegNr] = X;
                break;
                case MEMOP_RCL:
                    SetX(Memory[RegNr]);
                break;
                case MEMOP_ADD:
                    Memory[RegNr] += X;
                break;
                case MEMOP_SUB:
                    Memory[RegNr] -= X;
                break;
                case MEMOP_MUL:
                    Memory[RegNr] *= X;
                break;
                case MEMOP_DIV:
                    Memory[RegNr] /= X;
                break;
                case MEMOP_EXC:
                    final double Temp = Memory[RegNr];
                    Memory[RegNr] = X;
                    SetX(Temp);
                break;
                  } /*switch*/
              /* all done */
                OK = true;
              }
            while (false);
            if (!OK)
              {
                SetErrorState();
              } /*if*/
          } /*if*/
      } /*MemoryOp*/

    public void SpecialOp
      (
        int OpNr,
        boolean Indirect
      )
      {
        if (OpNr >= 0)
          {
            Enter();
            boolean OK = false;
            do /*once*/
              {
                if (Indirect)
                  {
                    if (OpNr >= MaxMemories)
                        break;
                    OpNr = (int)Math.round(Memory[OpNr]);
                  } /*if*/
                if (OpNr >= 20 && OpNr < 30)
                  {
                    Memory[OpNr - 20] += 1.0;
                    OK = true;
                    break;
                  } /*if*/
                if (OpNr >= 30 && OpNr < 40)
                  {
                    Memory[OpNr - 30] -= 1.0;
                    OK = true;
                    break;
                  } /*if*/
                switch (OpNr)
                  {
              /* more TBD */
                case 10:
                    SetX(Math.signum(X));
                    OK = true;
                break;
                case 11:
                  /* sample variance */
                    T = Memory[5] / Memory[3] - Memory[4] * Memory[4] / (Memory[3] * Memory[3]);
                    SetX(Memory[2] / Memory[3] - Memory[1] * Memory[1] / (Memory[3] * Memory[3]));
                    OK = true;
                break;
                case 12:
                  /* slope and intercept */
                    T =
                            (Memory[6] - Memory[1] * Memory[4] / Memory[3])
                        /
                            (Memory[5] - Memory[4] * Memory[4] / Memory[3]);
                    SetX
                      (
                        (Memory[1] - T * Memory[4]) / Memory[3]
                      );
                    OK = true;
                break;
                case 13:
                  /* correlation coefficient */
                    SetX
                      (
                            (Memory[6] - Memory[1] * Memory[4] / Memory[3])
                        /
                            (Memory[5] - Memory[4] * Memory[4] / Memory[3])
                        *
                            Math.sqrt
                              (
                                (Memory[5] - Memory[4] * Memory[4] / Memory[3]) / (Memory[3] - 1.0)
                              )
                        /
                            Math.sqrt
                              (
                                (Memory[2] - Memory[1] * Memory[1] / Memory[3]) / (Memory[3] - 1.0)
                              )
                      );
                    OK = true;
                break;
                case 14:
                  /* estimated y from x */
                      {
                        final double m =
                                (Memory[6] - Memory[1] * Memory[4] / Memory[3])
                            /
                                (Memory[5] - Memory[4] * Memory[4] / Memory[3]);
                        SetX
                          (
                                m * X
                            +
                                (Memory[1] - m * Memory[4]) / Memory[3]
                          );
                      }
                    OK = true;
                break;
                case 15:
                  /* estimated x from y */
                      {
                        final double m =
                                (Memory[6] - Memory[1] * Memory[4] / Memory[3])
                            /
                                (Memory[5] - Memory[4] * Memory[4] / Memory[3]);
                        SetX
                          (
                                ((Memory[1] - m * Memory[4]) / Memory[3] - X)
                            /
                                m
                          );
                      }
                    OK = true;
                break;
                case 16:
                    SetX(MaxProgram - 1.0 + (MaxMemories - 1.0) / 100.0);
                    OK = true;
                break;
              /* more TBD */
              /* 20-39 handled above */
                  } /*switch*/
              }
            while (false);
            if (!OK)
              {
                SetErrorState();
              } /*if*/
          } /*if*/
      } /*SpecialOp*/

    public void StatsSum()
      {
        Enter();
        if (InvState)
          {
          /* remove sample */
            Memory[1] -= X;
            Memory[2] -= X * X;
            Memory[3] -= 1.0;
            Memory[4] -= T;
            Memory[5] -= T * T;
            Memory[6] -= X * T;
            T -= 1.0;
          }
        else
          {
          /* accumulate sample */
            Memory[1] += X;
            Memory[2] += X * X;
            Memory[3] += 1.0;
            Memory[4] += T;
            Memory[5] += T * T;
            Memory[6] += X * T;
            T += 1.0;
          } /*if*/
        SetX(Memory[3]);
      } /*StatsSum*/

    public void StatsResult()
      {
        if (InvState)
          {
          /* population standard deviation */
            T = Math.sqrt((Memory[5] - Memory[4] * Memory[4] / Memory[3]) / (Memory[3] - 1.0));
            SetX
              (
                Math.sqrt((Memory[2] - Memory[1] * Memory[1] / Memory[3]) / (Memory[3] - 1.0))
              );
          }
        else
          {
          /* sample mean */
            T = Memory[4] / Memory[3];
            SetX(Memory[1] / Memory[3]);
          } /*if*/
      } /*StatsResult*/

    public void StepPC
      (
        boolean Forward
      )
      {
        if (Forward)
          {
            if (PC < MaxProgram - 1)
              {
                ++PC;
                ShowCurProg();
              } /*if*/
          }
        else
          {
            if (PC > 0)
              {
                --PC;
                ShowCurProg();
              } /*if*/
          } /*if*/
      } /*StepPC*/

    void ResetLabels()
      {
        Labels.clear();
        GotLabels = false;
      } /*ResetLabels*/

    public void StoreInstr
      (
        int Instr
      )
      {
        ResetLabels();
        Program[PC] = (byte)Instr;
        if (PC < MaxProgram - 1)
          {
            ClearDelayedStep();
            if (DelayTask == null)
              {
                DelayTask = new DelayedStep();
              } /*if*/
            ShowCurProg(); /* show updated contents of current location */
            ++PC;
          /* give user a chance to see current contents before stepping to next location
            -- this is a nicety the original calculator did not have */
            BGTask.postDelayed(DelayTask, 250);
          }
        else
          {
            SetProgMode(false);
          } /*if*/
      } /*StoreInstr*/

    public void StorePrevInstr
      (
        int Instr
      )
      {
        if (PC > 0)
          {
            --PC;
            StoreInstr(Instr);
          }
        else
          {
            SetErrorState();
          } /*if*/
      } /*StorePrevInstr*/

    public void InsertAtCurInstr()
      {
        for (int i = MaxProgram; i > PC + 1; --i)
          {
            Program[i - 1] = Program[i - 2];
          } /*for*/
        Program[PC] = (byte)0;
        ResetLabels();
        ShowCurProg();
      } /*InsertAtCurInstr*/

    public void DeleteCurInstr()
      {
        for (int i = PC; i < MaxProgram - 1; ++i)
          {
            Program[i] = Program[i + 1];
          } /*for*/
        Program[MaxProgram - 1] = (byte)0;
        ResetLabels();
        ShowCurProg();
      } /*DeleteCurInstr*/

    public void StepProgram()
      {
        FillInLabels();
        Interpret(true);
      } /*StepProgram*/

    class ProgRunner implements Runnable
      {
        public void run()
          {
            if (ProgRunning)
              {
                Interpret(true);
                ContinueProgRunner();
              } /*if*/
          } /*run*/
      } /*ProgRunner*/

    void ContinueProgRunner()
      {
        if (ProgRunning && !InErrorState())
          {
            if (ProgRunningSlowly)
              {
                BGTask.postDelayed(ExecuteTask, 250);
              }
            else
              {
              /* run as fast as possible */
                BGTask.post(ExecuteTask);
              } /*if*/
          } /*if*/
      } /*ContinueProgRunner*/

    public void StartProgram()
      {
        ClearDelayedStep();
        FillInLabels();
        if (ExecuteTask == null)
          {
            ExecuteTask = new ProgRunner();
          } /*if*/
        ProgRunningSlowly = false; /* just in case */
        ProgRunning = true;
        TheDisplay.SetShowingRunning();
        ContinueProgRunner();
      } /*StartProgram*/

    public void StopProgram()
      {
        ProgRunning = false;
        ClearDelayedStep();
        if (ExecuteTask != null)
          {
            BGTask.removeCallbacks(ExecuteTask);
          } /*if*/
        if (CurState == ErrorState)
          {
            TheDisplay.SetShowingError();
          }
        else
          {
            TheDisplay.SetShowing(LastShowing);
          } /*if*/
      } /*StopProgram*/

    public void SetSlowExecution
      (
        boolean Slow
      )
      {
        if (ProgRunningSlowly != Slow)
          {
            ProgRunningSlowly = Slow;
            if (!ProgRunningSlowly)
              {
                TheDisplay.SetShowingRunning();
              } /*if*/
          } /*if*/
      } /*SetSlowExecution*/

    public void ResetProg()
      {
        for (int i = 0; i < MaxFlags; ++i)
          {
            Flag[i] = false;
          } /*for*/
        PC = 0;
        ReturnLast = -1;
      } /*ResetProg*/

    int GetProg
      (
        boolean Executing
      )
      /* returns the next program instruction byte, or -1 if run off the end. */
      {
        byte Result;
        if (PC < MaxProgram)
          {
            Result = Program[PC++];
          }
        else
          {
            PC = 0;
            Result = -1;
            if (Executing)
              {
                StopProgram();
              } /*if*/
          } /*if*/
        return
            (int)Result;
      } /*GetProg*/

    int GetLoc
      (
        boolean Executing
      )
      /* fetches a program location from the instruction stream, or -1 on failure.
        Assumes Labels has been filled in. */
      {
        int Result = -1;
        final int NextByte = GetProg(Executing);
        if (NextByte >= 0)
          {
            if (NextByte < 10)
              {
              /* 3-digit location */
                final int NextByte2 = GetProg(Executing);
                if (NextByte2 >= 0)
                  {
                    Result = NextByte * 100 + NextByte2;
                  } /*if*/
              }
            else if (NextByte == 40) /*Ind*/
              {
                final int Reg = GetProg(Executing);
                if (Reg >= 0 && Reg < MaxMemories)
                  {
                    Result = (int)Memory[Reg];
                    if (Result < 0 || Result >= MaxProgram)
                      {
                        Result = -1;
                      } /*if*/
                  } /*if*/
              }
            else /* symbolic label */
              {
                if (Labels.containsKey(NextByte))
                  {
                    Result = Labels.get(NextByte);
                  } /*if*/
              } /*if*/
          } /*if*/
        return
            Result;
      } /*GetLoc*/

    int GetUnitOp
      (
        boolean Executing
      )
      /* fetches one or two bytes from the instruction stream; if the
        first (only) byte is < 10, then that's the value; otherwise
        a value of 40 indicates indirection through the register
        number in the next byte. Any other value is invalid. */
      {
        int Result = -1;
        final int NextByte = GetProg(Executing);
        if (NextByte >= 0)
          {
            boolean OK;
            if (NextByte < 10)
              {
                Result = NextByte;
                OK = true;
              }
            else if (NextByte == 40)
              {
                final int Reg = GetProg(Executing);
                if (Reg >= 0 && Reg < MaxMemories)
                  {
                    Result = (int)Memory[Reg];
                    OK = Result >= 0 && Result < 10;
                    if (!OK)
                      {
                        Result = -1;
                      } /*if*/
                  }
                else
                  {
                    OK = false;
                  } /*if*/
              }
            else
              {
                OK = false;
              } /*if*/
            if (!OK)
              {
                SetErrorState();
                StopProgram();
              } /*if*/
          } /*if*/
        return
            Result;
      } /*GetUnitOp*/

    public void Transfer
      (
        boolean Call,
        boolean FromInteractive,
        int Loc,
        boolean Symbolic,
        boolean Ind
      )
      /* implements GTO and SBR. */
      {
        if (Loc >= 0)
          {
            boolean OK = false;
            do /*once*/
              {
                if (Ind)
                  {
                    if (Loc >= MaxMemories)
                        break;
                    Loc = (int)Memory[Loc];
                  }
                else if (Symbolic)
                  {
                    if (!Labels.containsKey(Loc))
                        break;
                    Loc = Labels.get(Loc);
                  } /*if*/
                if (Loc < 0 || Loc >= MaxProgram)
                    break;
                if (Call)
                  {
                    if (ReturnLast == MaxReturnStack)
                        break;
                    ReturnStack[++ReturnLast] = new ReturnStackEntry(PC, FromInteractive);
                  } /*if*/
                PC = Loc;
                if (Call && FromInteractive)
                  {
                    StartProgram();
                  } /*if*/
              /* all successfully done */
                OK = true;
              }
            while (false);
            if (!OK)
              {
                SetErrorState();
                StopProgram();
              } /*if*/
          } /*if*/
      } /*Transfer*/

    void Return()
      /* returns to the last-saved location on the return stack. */
      {
        boolean OK = false;
        do /*once*/
          {
            if (ReturnLast < 0)
                break;
            final ReturnStackEntry ReturnTo = ReturnStack[ReturnLast--];
            if (ReturnTo.Addr < 0 || ReturnTo.Addr >= MaxProgram)
                break;
            PC = ReturnTo.Addr;
            if (ReturnTo.FromInteractive)
              {
                StopProgram();
              } /*if*/
          /* all successfully done */
            OK = true;
          }
        while (false);
        if (!OK)
          {
            SetErrorState();
            StopProgram();
          } /*if*/
      } /*Return*/

    public void SetFlag
      (
        int FlagNr,
        boolean Ind,
        boolean Set
      )
      {
        if (FlagNr >= 0)
          {
            if (Ind)
              {
                if (FlagNr < MaxMemories)
                  {
                    FlagNr = (int)Memory[FlagNr];
                  }
                else
                  {
                    FlagNr = -1;
                  } /*if*/
              } /*if*/
            if (FlagNr >= 0 && FlagNr < MaxFlags)
              {
                Flag[FlagNr] = Set;
              }
            else
              {
                SetErrorState();
                StopProgram();
              } /*if*/
          } /*if*/
      } /*SetFlag*/

    public void BranchIfFlag
      (
        int FlagNr,
        boolean FlagNrInd,
        int Target,
        boolean TargetSymbolic,
        boolean TargetInd
      )
      {
        if (FlagNr >= 0 && Target >= 0)
          {
            if (FlagNrInd)
              {
                if (FlagNr < MaxMemories)
                  {
                    FlagNr = (int)Memory[FlagNr];
                  }
                else
                  {
                    FlagNr = -1;
                  } /*if*/
              } /*if*/
            if (FlagNr >= 0 && FlagNr < MaxFlags)
              {
                if (InvState != Flag[FlagNr])
                  {
                    Transfer(false, false, Target, TargetSymbolic, TargetInd);
                  } /*if*/
              }
            else
              {
                SetErrorState();
                StopProgram();
              } /*if*/
          } /*if*/
      } /*BranchIfFlag*/

    public void CompareBranch
      (
        boolean Greater,
        int NewPC,
        boolean Ind
      )
      {
        if (NewPC >= 0)
          {
            if
              (
                InvState ?
                    Greater ?
                        X < T
                    :
                        X != T
                :
                    Greater ?
                        X >= T
                    :
                        X == T
              )
              {
                Transfer(false, false, NewPC, false, Ind);
              } /*if*/
          } /*if*/
      } /*CompareBranch*/

    public void DecrementSkip
      (
        int Reg,
        boolean RegInd,
        int Target,
        boolean TargetSymbolic,
        boolean TargetInd
      )
      {
        if (Reg >= 0 && Target >= 0)
          {
            if (RegInd)
              {
                if (Reg < MaxMemories)
                  {
                    Reg = (int)Memory[Reg];
                  }
                else
                  {
                    Reg = -1;
                  } /*if*/
              } /*if*/
            if (Reg >= 0 && Reg < MaxMemories)
              {
                Memory[Reg] -= 1.0;
                if (InvState == (Memory[Reg] == 0.0))
                  {
                    Transfer(false, false, Target, TargetSymbolic, TargetInd);
                  } /*if*/
              }
            else
              {
                SetErrorState();
                StopProgram();
              } /*if*/
          } /*if*/
      } /*DecrementSkip*/

    void Interpret
      (
        boolean Execute /* false to just collect labels */
      )
      /* main program interpreter loop: interprets one instruction and
        advances/jumps the PC accordingly. */
      {
        final int Op = GetProg(Execute);
        if (Op >= 0)
          {
            boolean WasModifier = false;
            if (Execute)
              {
                switch (Op)
                  {
                case 01:
                case 02:
                case 03:
                case 04:
                case 05:
                case 06:
                case 07:
                case 8:
                case 9:
                case 00:
                    Digit((char)(Op + 48));
                break;
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
                    Transfer(true, false, Op, true, false);
                break;
                case 22:
                case 27:
                    InvState = !InvState;
                    WasModifier = true;
                break;
                case 23:
                    Ln();
                break;
                case 24:
                    ClearEntry();
                break;
                case 25:
                case 20:
                    ClearAll();
                break;
              /* 26 same as 21 */
              /* 27 same as 22 */
                case 28:
                    Log();
                break;
                case 29: /*CP*/
                    T = 0.0;
                break;
              /* 20 same as 25 */
                case 32:
                    SwapT();
                break;
                case 33:
                    Square();
                break;
                case 34:
                    Sqrt();
                break;
                case 35:
                    Reciprocal();
                break;
                case 36: /*Pgm*/
                    SelectProgram(GetProg(true), false); /* TBD only for duration of following instr */
                break;
                case 37:
                    Polar();
                break;
                case 38:
                    Sin();
                break;
                case 39:
                    Cos();
                break;
                case 30:
                    Tan();
                break;
                case 42:
                    MemoryOp(MEMOP_STO, GetProg(true), false);
                break;
                case 43:
                    MemoryOp(MEMOP_RCL, GetProg(true), false);
                break;
                case 44:
                    MemoryOp(MEMOP_ADD, GetProg(true), false);
                break;
                case 45:
                    Operator(STACKOP_EXP);
                break;
                case 47:
                    ClearMemories();
                break;
                case 48:
                    MemoryOp(MEMOP_EXC, GetProg(true), false);
                break;
                case 49:
                    MemoryOp(MEMOP_MUL, GetProg(true), false);
                break;
                case 52:
                    EnterExponent();
                break;
                case 53:
                    LParen();
                break;
                case 54:
                    RParen();
                break;
                case 55:
                    Operator(STACKOP_DIV);
                break;
                case 57:
                    SetDisplayMode
                      (
                        InvState ? FORMAT_FIXED : FORMAT_ENG,
                        -1
                      );
                break;
                case 58: /*Fix*/
                    if (InvState)
                      {
                        SetDisplayMode(FORMAT_FIXED, -1);
                      }
                    else
                      {
                        SetDisplayMode(FORMAT_FIXED, GetProg(true));
                      } /*if*/
                break;
                case 59:
                    Int();
                break;
                case 50:
                    Abs();
                break;
                case 61: /*GTO*/
                    Transfer(false, false, GetLoc(true), false, false);
                break;
                case 62: /*Pgm Ind*/
                    SelectProgram(GetProg(true), true); /* TBD only for duration of following instr */
                break;
                case 63:
                    MemoryOp(MEMOP_EXC, GetProg(true), true);
                break;
                case 64:
                    MemoryOp(MEMOP_MUL, GetProg(true), true);
                break;
                case 65:
                    Operator(STACKOP_MUL);
                break;
                case 66: /*Pause*/
                  /* TBD */
                break;
                case 67: /*x=t*/
                case 77: /*x≥t*/
                    CompareBranch(Op == 77, GetLoc(true), false);
                break;
                case 68: /*Nop*/
                  /* No effect */
                break;
                case 69:
                    SpecialOp(GetProg(true), false);
                break;
                case 60:
                    SetAngMode(ANG_DEG);
                break;
                case 71: /*SBR*/
                    if (InvState)
                      {
                        Return();
                      }
                    else
                      {
                        Transfer(true, false, GetLoc(true), false, false);
                      } /*if*/
                break;
                case 72:
                    MemoryOp(MEMOP_STO, GetProg(true), true);
                break;
                case 73:
                    MemoryOp(MEMOP_RCL, GetProg(true), true);
                break;
                case 74:
                    MemoryOp(MEMOP_ADD, GetProg(true), true);
                break;
                case 75:
                    Operator(STACKOP_SUB);
                break;
                case 76: /*Lbl*/
                    GetProg(true); /* just skip label, assume Labels already filled in */
                break;
              /* 77 handled above */
                case 78:
                    StatsSum();
                break;
                case 79:
                    StatsResult();
                break;
                case 70:
                    SetAngMode(ANG_RAD);
                break;
                case 81:
                    ResetProg();
                    StopProgram();
                break;
                case 83: /*GTO Ind*/
                    Transfer(false, false, GetLoc(true), false, true);
                break;
                case 84:
                    SpecialOp(GetProg(true), true);
                break;
                case 85:
                    Operator(STACKOP_ADD);
                break;
                case 86: /*St flg*/
                    SetFlag(GetUnitOp(true), false, !InvState);
                break;
                case 87: /*If flg*/
                      {
                        final int FlagNr = GetUnitOp(true);
                        final int Target = GetLoc(true);
                        BranchIfFlag(FlagNr, false, Target, false, false);
                      }
                break;
                case 88:
                    D_MS();
                break;
                case 89:
                    Pi();
                break;
                case 80:
                    SetAngMode(ANG_GRAD);
                break;
                case 91:
                case 96:
                    StopProgram();
                break;
                case 92: /*INV SBR*/
                    Return();
                break;
                case 93:
                    DecimalPoint();
                break;
                case 94:
                    ChangeSign();
                break;
                case 95:
                    Equals();
                break;
              /* 96 same as 91 */
                case 97: /*Dsz*/
                      {
                        final int Reg = GetUnitOp(true);
                        final int Target = GetLoc(true);
                        DecrementSkip(Reg, false, Target, false, false);
                      }
                break;
                case 98:
                  /* TBD */
                break;
                case 99:
                  /* TBD */
                break;
                case 90:
                  /* TBD */
                break;
                default:
                    SetErrorState();
                    StopProgram();
                break;
                  } /*switch*/
              }
            else
              {
              /* just advance PC past instruction and update Labels as appropriate */
                switch (Op)
                  {
                case 22:
                case 27:
                    WasModifier = true;
                break;
                case 36: /*Pgm*/
                case 42: /*STO*/
                case 43: /*RCL*/
                case 44: /*SUM*/
                case 48: /*Exc*/
                case 49: /*Prd*/
                case 62: /*Pgm Ind*/
                case 63: /*Exc Ind*/
                case 64: /*Prd Ind*/
                case 69: /*Op*/
                case 72: /*STO Ind*/
                case 73: /*RCL Ind*/
                case 74: /*SUM Ind*/
                case 83: /*GTO Ind*/
                case 84: /*Op Ind*/
                case 86: /*St flg*/
                  /* one byte following */
                    GetProg(false);
                break;
                case 61: /*GTO*/
                case 67: /*x=t*/
                case 77: /*x≥t*/
                    GetLoc(false);
                break;
                case 71: /*SBR*/
                    if (!InvState) /* in case it wasn't merged */
                      {
                        GetLoc(false);
                      } /*if*/
                break;
                case 76: /*Lbl*/
                      {
                        final int TheLabel = GetProg(false);
                        if (TheLabel >= 0 && PC >= 0 && !Labels.containsKey(TheLabel))
                          {
                            Labels.put(TheLabel, PC);
                          } /*if*/
                      }
                break;
                case 87: /*If flg*/
                case 97: /*Dsz*/
                    GetUnitOp(false); /*register/flag*/
                    GetLoc(false); /*branch target*/
                break;
                  } /*switch*/
              } /*if*/
            if (!WasModifier)
              {
                InvState = false;
              } /*if*/
          } /*if*/
      } /*Interpret*/

    public void FillInLabels()
      {
        if (!GotLabels)
          {
            final int SavePC = PC;
            final boolean SaveInvState = InvState;
            InvState = false; /*?*/
            PC = 0;
            do
              {
                Interpret(false);
              }
            while (PC != -1 && PC < MaxProgram);
            GotLabels = true;
            PC = SavePC;
            InvState = SaveInvState;
          } /*if*/
      } /*FillInLabels*/

  } /*State*/
