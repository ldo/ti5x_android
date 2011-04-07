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
    int CurState = EntryState;

    Display TheDisplay;
    String CurDisplay;

    public static final int FORMAT_FIXED = 0;
    public static final int FORMAT_FLOATING = 1;
    public static final int FORMAT_ENG = 2;
    int CurFormat = FORMAT_FIXED;

    public final static int STACKOP_ADD = 1;
    public final static int STACKOP_SUB = 2;
    public final static int STACKOP_MUL = 3;
    public final static int STACKOP_DIV = 4;
    public final static int STACKOP_EXP = 5;

    class StackEntry
      {
        double Operand;
        int Operator;
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
      {
        if (CurState != ResultState)
          {
            int Exp;
            boolean HasExp;
            switch (CurState)
              {
            case EntryStateExponential:
            case DecimalEntryStateExponential:
            case ExponentEntryState:
                HasExp = true;
                Exp = Integer.parseInt(CurDisplay.substring(CurDisplay.length() - 3));
                if (CurDisplay.charAt(CurDisplay.length() - 2) == '-')
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
                                    CurDisplay.length() - 3
                                :
                                    CurDisplay.length() - 1
                              )
                        +
                            new String(new char[] {TheDigit})
                        +
                            (CurState == EntryStateExponential ?
                                CurDisplay.substring(CurDisplay.length() - 3)
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
            TheDisplay.SetShowing(CurDisplay);
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
        if
          (
                CurFormat == FORMAT_FIXED
            &&
                (Math.abs(X) < Math.pow(10.0, -7.0) || Math.abs(X) > Math.pow(10.0, 7.0))
          )
          {
            CurFormat = FORMAT_FLOATING;
          } /*if*/
        switch (CurFormat)
          {
        case FORMAT_FIXED:
        default:
            Exp = 0;
        break;
        case FORMAT_FLOATING:
            Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(10.0));
        break;
        case FORMAT_ENG:
            Exp = (int)Math.floor(Math.log(Math.abs(X)) / Math.log(1000.0)) * 3;
        break;
          } /*switch*/
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

  /* more TBD */

  } /*State*/

