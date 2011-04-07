package nz.gen.geek_central.ti5x;

public class State
  /* the calculation state */
  {
    final static int EntryState = 0;
    final static int DecimalEntryState = 1;
    final static int ExponentEntryState = 2;
    final static int EntryStateExponential = 3;
    final static int DecimalEntryStateExponential = 4;
    final static int ResultState = 10;
    final static int ErrorState = 11;
    int CurState = EntryState;

    Display TheDisplay;
    String CurDisplay;

    public static final int FORMAT_FIXED = 0;
    public static final int FORMAT_FLOATING = 1;
    public static final int FORMAT_ENG = 2;
    int CurFormat = FORMAT_FIXED;

    public static final int ANG_RAD = 1;
    public static final int ANG_DEG = 2;
    public static final int ANG_GRAD = 3;
    int CurAng = ANG_DEG;

    public final static int STACKOP_ADD = 1;
    public final static int STACKOP_SUB = 2;
    public final static int STACKOP_MUL = 3;
    public final static int STACKOP_DIV = 4;
    public final static int STACKOP_EXP = 5;
    public final static int STACKOP_LOG = 6;
    final static int STACKOP_PAREN = 99;

    class StackEntry
      {
        double Operand;
        int Operator;

        public StackEntry
          (
            double Operand,
            int Operator
          )
          {
            this.Operand = Operand;
            this.Operator = Operator;
          } /*StackEntry*/

      } /*StackEntry*/

    final int MaxStack = 8;
    double X, T;
    StackEntry Stack[];
    int StackNext;

    public State
      (
        Display TheDisplay
      )
      {
        this.TheDisplay = TheDisplay;
        Stack = new StackEntry[MaxStack];
        StackNext = 0;
        X = 0.0;
        T = 0.0;
        ResetEntry();
      } /*State*/

    public void ResetEntry()
      {
        if (CurFormat == FORMAT_FIXED)
          {
            CurState = EntryState;
            CurDisplay = "0.";
          }
        else
          {
            CurState = EntryStateExponential;
            CurDisplay = "0. 00";
          } /*if*/
        TheDisplay.SetShowing(CurDisplay);
      } /*ResetEntry*/

    public void Enter()
      /* finishes the entry of the current number. */
      {
        if (CurState != ResultState && CurState != ErrorState)
          {
            int Exp;
            boolean HasExp;
            switch (CurState)
              {
            case EntryStateExponential:
            case DecimalEntryStateExponential:
            case ExponentEntryState:
                HasExp = true;
                Exp = Integer.parseInt(CurDisplay.substring(CurDisplay.length() - 2));
                if (CurDisplay.charAt(CurDisplay.length() - 3) == '-')
                  {
                    Exp = - Exp;
                  } /*if*/
            break;
            default:
                HasExp = false;
                Exp = 0;
            break;
              } /*switch*/
            X = Double.parseDouble
              (
                CurDisplay.substring
                  (
                    0,
                    HasExp ? CurDisplay.length() - 3 : CurDisplay.length()
                  )
              );
            if (HasExp)
              {
                X = X * Math.pow(10.0, Exp);
              } /*if*/
            SetX(X);
          } /*if*/
      } /*Enter*/

    public Runnable ClearAll()
      {
        return new Runnable()
          {
            public void run()
              {
                StackNext = 0;
                ResetEntry();
              } /*run*/
          };
      } /*ClearAll*/

    public Runnable ClearEntry()
      {
        return new Runnable()
          {
            public void run()
              {
                ResetEntry();
              } /*run*/
          };
      } /*ClearEntry*/

    public class Digit implements Runnable
      {
        char TheDigit;

        public Digit
          (
            char TheDigit
          )
          {
            this.TheDigit = TheDigit;
          } /*Digit*/

        public void run()
          {
            if (CurState == ResultState)
              {
                ResetEntry();
              } /*if*/
            switch (CurState)
              {
            case EntryState:
            case EntryStateExponential:
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
                            CurDisplay.substring
                              (
                                0,
                                CurState == EntryStateExponential ?
                                    CurDisplay.length() - 4
                                :
                                    CurDisplay.length() - 1
                              )
                        +
                            new String(new char[] {TheDigit})
                        +
                            (CurState == EntryStateExponential ?
                                CurDisplay.substring(CurDisplay.length() - 4)
                            :
                                CurDisplay.substring(CurDisplay.length() - 1)
                            );
                  } /*if*/
            break;
            case DecimalEntryState:
                CurDisplay = CurDisplay + new String(new char[] {TheDigit});
            break;
            case DecimalEntryStateExponential:
                CurDisplay =
                        CurDisplay.substring(0, CurDisplay.length() - 3)
                    +
                        new String(new char[] {TheDigit})
                    +
                        CurDisplay.substring(CurDisplay.length() - 3);
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
            if (CurState != ErrorState)
              {
                TheDisplay.SetShowing(CurDisplay);
              } /*if*/
          } /*run*/

      } /*Digit*/

    public Runnable DecimalPoint()
      {
        return new Runnable()
          {
            public void run()
              {
                if (CurState == ResultState)
                  {
                    ResetEntry();
                  } /*if*/
                if (CurState == EntryState)
                  {
                    CurState = DecimalEntryState;
                  }
                else if (CurState == EntryStateExponential)
                  {
                    CurState = DecimalEntryStateExponential;
                  } /*if*/
                /* otherwise ignore */
              } /*run*/
          };
      } /*DecimalPoint*/

    public Runnable EnterExponent()
      {
        return new Runnable()
          {
            public void run()
              {
                switch (CurState)
                  {
                case EntryState:
                case DecimalEntryState:
                    CurDisplay = CurDisplay + " 00";
                    CurState = ExponentEntryState;
                    TheDisplay.SetShowing(CurDisplay);
                break;
                case EntryStateExponential:
                case DecimalEntryStateExponential:
                    CurState = ExponentEntryState;
                break;
                case ResultState:
                    if (CurFormat != FORMAT_FLOATING)
                      {
                        CurFormat = FORMAT_FLOATING;
                        SetX(X); /* will cause redisplay */
                      } /*if*/
                break;
                  } /*switch*/
              } /*run*/
          };
      } /*EnterExponent*/

    public void SetX
      (
        double NewX
      )
      /* sets the display to show the specified value. */
      {
        int Exp;
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
                    (Math.abs(X) < Math.pow(10.0, -7.0) || Math.abs(X) > Math.pow(10.0, 7.0))
              )
              {
                CurFormat = FORMAT_FLOATING;
              } /*if*/
            Exp = 0;
            if (X != 0.0)
              {
                switch (CurFormat)
                  {
                case FORMAT_FLOATING:
                    Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(10.0));
                break;
                case FORMAT_ENG:
                    Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(1000.0)) * 3;
                break;
                  } /*switch*/
              } /*if*/
            CurDisplay = String.format("%.8f", X / Math.pow(10.0, Exp));
            while (CurDisplay.length() != 0 && CurDisplay.charAt(CurDisplay.length() - 1) == '0')
              {
                CurDisplay = CurDisplay.substring(0, CurDisplay.length() - 1);
              } /*while*/
            if (CurDisplay.length() == 0)
              {
                CurDisplay = "0.";
              } /*if*/
          /* assume there will always be a decimal point? */
            if (CurFormat != FORMAT_FIXED)
              {
                CurDisplay += (Exp < 0 ? "-" : " ") + String.format("%02d", Math.abs(Exp));
              } /*if*/
            TheDisplay.SetShowing(CurDisplay);
          }
        else
          {
            TheDisplay.SetShowingError();
            CurState = ErrorState;
          } /*if*/
      } /*SetX*/

    public Runnable ChangeSign()
      {
        return new Runnable()
          {
            public void run()
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
                    TheDisplay.SetShowing(CurDisplay);
                break;
                case ExponentEntryState:
                    CurDisplay =
                            CurDisplay.substring(0, CurDisplay.length() - 3)
                        +
                            (CurDisplay.charAt(CurDisplay.length() - 3) == '-' ? ' ' : '-')
                        +
                            CurDisplay.substring(CurDisplay.length() - 2);
                    TheDisplay.SetShowing(CurDisplay);
                break;
                case ResultState:
                    SetX(- X);
                break;
                  } /*switch*/
              } /*run*/
          };
      } /*ChangeSign*/

    public class SetDisplayMode implements Runnable
      {
        final int NewMode;

        public SetDisplayMode
          (
            int NewMode
          )
          {
            this.NewMode = NewMode;
          } /*SetDisplayMode*/

        public void run()
          {
            if (CurFormat != NewMode)
              {
                Enter();
                CurFormat = NewMode;
                SetX(X);
              } /*if*/
          } /*run*/

      } /*SetDisplayMode*/

    void DoStackTop()
      {
        final StackEntry ThisOp = Stack[--StackNext];
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
        case STACKOP_LOG:
            X = Math.log(ThisOp.Operand) / Math.log(X);
        break;
        case STACKOP_PAREN:
          /* no-op */
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
        case STACKOP_LOG:
            Result = 3;
        break;
        case STACKOP_PAREN:
            Result = 0;
        break;
          } /*OpCode*/
        return
            Result;
      } /*Precedence*/

    void StackPush
      (
        int OpCode
      )
      {
        if (StackNext == MaxStack)
          {
          /* overflow! lose bottom of stack */
            for (int i = 0; i < MaxStack; ++i)
              {
                Stack[i - 1] = Stack[i];
              } /*for*/
            --StackNext;
          } /*if*/
        Stack[StackNext++] = new StackEntry(X, OpCode);
      } /*StackPush*/

    public class Operator implements Runnable
      {
        final int OpCode;

        public Operator
          (
            int OpCode
          )
          {
            this.OpCode = OpCode;
          } /*Operator*/

        public void run()
          {
            Enter();
            boolean PoppedSomething = false;
            for (;;)
              {
                if (StackNext == 0)
                    break;
                if (Precedence(Stack[StackNext - 1].Operator) < Precedence(OpCode))
                    break;
                DoStackTop();
                PoppedSomething = true;
              } /*for*/
            if (PoppedSomething)
              {
                SetX(X);
              } /*if*/
            StackPush(OpCode);
          } /*run*/

      } /*Operator*/

    public Runnable LParen()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                StackPush(STACKOP_PAREN);
              } /*run*/
          };
      } /*LParen*/

    public Runnable RParen()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                boolean PoppedSomething = false;
                for (;;)
                  {
                    if (StackNext == 0)
                        break;
                    if (Stack[StackNext - 1].Operator == STACKOP_PAREN)
                      {
                        --StackNext;
                        break;
                      } /*if*/
                    DoStackTop();
                    PoppedSomething = true;
                  } /*for*/
                if (PoppedSomething)
                  {
                    SetX(X);
                  } /*if*/
              } /*run*/
          };
      } /*RParen*/

    public Runnable Equals()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                while (StackNext != 0)
                  {
                    DoStackTop();
                  } /*while*/
                SetX(X);
              } /*run*/
          };
      } /*Equals*/

    public class SetAngMode implements Runnable
      {
        final int NewMode;

        public SetAngMode
          (
            int NewMode
          )
          {
            this.NewMode = NewMode;
          } /*SetAngMode*/

        public void run()
          {
            CurAng = NewMode;
          } /*run*/

      } /*SetAngMode*/

    public Runnable Square()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                SetX(X * X);
              } /*run*/
          };
      } /*Square*/

    public Runnable Sqrt()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                SetX(Math.sqrt(X));
              } /*run*/
          };
      } /*Sqrt*/

    public Runnable Reciprocal()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                SetX(1.0 / X);
              } /*run*/
          };
      } /*Reciprocal*/

    public Runnable Sin()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                Double Scale, NewValue;
                switch (CurAng)
                  {
                case ANG_RAD:
                default:
                    Scale = 1.0;
                break;
                case ANG_DEG:
                    Scale = 180.0 / Math.PI;
                break;
                case ANG_GRAD:
                    Scale = 200.0 / Math.PI;
                break;
                  } /*CurAng*/
                if (Button.InvState)
                  {
                    NewValue = Math.asin(X) * Scale;
                  }
                else
                  {
                    NewValue = Math.sin(X / Scale);
                  } /*if*/
                SetX(NewValue);
              } /*run*/
          };
      } /*Sin*/

    public Runnable Cos()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                Double Scale, NewValue;
                switch (CurAng)
                  {
                case ANG_RAD:
                default:
                    Scale = 1.0;
                break;
                case ANG_DEG:
                    Scale = 180.0 / Math.PI;
                break;
                case ANG_GRAD:
                    Scale = 200.0 / Math.PI;
                break;
                  } /*CurAng*/
                if (Button.InvState)
                  {
                    NewValue = Math.acos(X) * Scale;
                  }
                else
                  {
                    NewValue = Math.cos(X / Scale);
                  } /*if*/
                SetX(NewValue);
              } /*run*/
          };
      } /*Cos*/

    public Runnable Tan()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                Double Scale, NewValue;
                switch (CurAng)
                  {
                case ANG_RAD:
                default:
                    Scale = 1.0;
                break;
                case ANG_DEG:
                    Scale = 180.0 / Math.PI;
                break;
                case ANG_GRAD:
                    Scale = 200.0 / Math.PI;
                break;
                  } /*CurAng*/
                if (Button.InvState)
                  {
                    NewValue = Math.atan(X) * Scale;
                  }
                else
                  {
                    NewValue = Math.tan(X / Scale);
                  } /*if*/
                SetX(NewValue);
              } /*run*/
          };
      } /*Tan*/

    public Runnable Log()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                if (Button.AltState)
                  {
                    if (Button.InvState)
                      {
                        SetX(Math.pow(10.0, X));
                      }
                    else
                      {
                        SetX(Math.log10(X));
                      } /*if*/
                  }
                else
                  {
                    if (Button.InvState)
                      {
                        SetX(Math.exp(X));
                      }
                    else
                      {
                        SetX(Math.log(X));
                      } /*if*/
                  } /*if*/
              } /*run*/
          };
      } /*Log*/

    public Runnable Pi()
      {
        return new Runnable()
          {
            public void run()
              {
                SetX(Math.PI);
              } /*run*/
          };
      } /*Pi*/

    public Runnable Int()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                final Double IntPart = Math.floor(Math.abs(X));
                if (Button.InvState)
                  {
                    SetX((Math.abs(X) - IntPart) * Math.signum(X));
                  }
                else
                  {
                    SetX(IntPart * Math.signum(X));
                  } /*if*/
              } /*run*/
          };
      } /*Int*/

    public Runnable Abs()
      {
        return new Runnable()
          {
            public void run()
              {
                SetX(Math.abs(X));
              } /*run*/
          };
      } /*Abs*/

    public Runnable SwapT()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                final double SwapTemp = X;
                SetX(T);
                T = SwapTemp;
              } /*run*/
          };
      } /*SwapT*/

    public Runnable Polar()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                Double Scale, NewX, NewY;
                switch (CurAng)
                  {
                case ANG_RAD:
                default:
                    Scale = 1.0;
                break;
                case ANG_DEG:
                    Scale = 180.0 / Math.PI;
                break;
                case ANG_GRAD:
                    Scale = 200.0 / Math.PI;
                break;
                  } /*CurAng*/
                if (Button.InvState)
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
              } /*run*/
          };
      } /*Polar*/

    public Runnable D_MS()
      {
        return new Runnable()
          {
            public void run()
              {
                Enter();
                final Double Sign = Math.signum(X);
                final Double Degrees = Math.floor(Math.abs(X));
                final Double Fraction = Math.abs(X) - Degrees;
                if (Button.InvState)
                  {
                    final double Minutes = Math.floor(Fraction * 60.0);
                    SetX((Degrees + Minutes / 100.0 + (Fraction * 60.0 - Minutes) * 6 / 1000.0) * Sign);
                  }
                else
                  {
                    final Double Minutes = Math.floor(Fraction * 100.0);
                    SetX((Degrees + Minutes / 60.0 + (Fraction * 100.0 - Minutes) / 36.0) * Sign);
                  } /*if*/
              } /*run*/
          };
      } /*D_MS*/

  /* more TBD */

  } /*State*/
