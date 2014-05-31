package nz.gen.geek_central.ti5x;
/*
    The calculation state, number entry and programs.

    Copyright 2011, 2012 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    This program is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
    A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

class Arith
  /* useful arithmetic-related stuff */
  {

    public static double RoundTo
      (
        double X,
        int NrFigures
      )
      /* returns X rounded to the specified number of significant figures. */
      {
        final double RoundFactor =
            X != 0.0 ?
                Math.pow
                  (
                    10,
                    NrFigures - 1 - (int)Math.floor(Math.log(Math.abs(X)) / Math.log(10.0))
                  )
            :
                1.0;
        return
            Math.rint(X * RoundFactor) / RoundFactor;
      } /*RoundTo*/

    public static int FiguresBeforeDecimal
      (
        double X,
        int Exp
      )
      /* returns the number of figures before the decimal point in the
        formatted representation of X scaled by Exp. This has to be
        at least 1, because the decimal point is part of the display
        of the preceding digit. */
      {
        int BeforeDecimal;
        if (X != 0.0)
          {
            BeforeDecimal = Math.max
              (
                (int)Math.ceil(Math.log10(Math.abs(X) / Math.pow(10.0, Exp))),
                1
              );
          }
        else
          {
            BeforeDecimal = 1;
          } /*if*/
        return
            BeforeDecimal;
      } /*FiguresBeforeDecimal*/

    public static final int MaxPrec = 15;
      /* fudge for roundoff caused by binary versus decimal arithmetic */

    public static double AbsIntPart
      (
        double X
      )
      /* returns the absolute integer part of X. */
      {
        return
            Math.floor(Math.abs(RoundTo(X, MaxPrec)));
      } /*AbsIntPart*/

  } /*Arith*/

public class State
  /* the calculator state, number entry and programs */
  {
    android.content.Context ctx;
  /* number-entry state */
    public final static int EntryState = 0;
    public final static int DecimalEntryState = 1;
    public final static int ExponentEntryState = 2;
    public final static int ResultState = 10;
    public final static int ErrorState = 11;
    public int CurState = EntryState;
    public boolean ExponentEntered = false;

    public boolean InvState = false; /* INV has been pressed/executed */

    String CurDisplay; /* current number display */
    android.os.Handler BGTask;
    Runnable DelayTask = null;
    Runnable ExecuteTask;
    Runnable RunProg = null;
    public Runnable OnStop = null;

    public static class ImportEOFException extends RuntimeException
      /* indicates no more data to import. */
      {

        public ImportEOFException
          (
            String Message
          )
          {
            super(Message);
          } /*ImportEOFException*/

      } /*ImportEOFException*/

    public static abstract class ImportFeeder
      {
        abstract double Next()
            throws
                ImportEOFException,
                Persistent.DataFormatException;
          /* returns the next input value or raises ImportEOFException if none. */

        public void End()
          /* stops further invocations of the task. Subclass may add
            further cleanup, but must also invoke this superclass method. */
          {
            if (Global.Calc != null)
              {
                Global.Calc.Import = null;
              } /*if*/
          } /*End*/
      } /*ImportFeeder*/
    ImportFeeder Import = null;

    java.security.SecureRandom Random = new java.security.SecureRandom();

  /* number-display format */
    public static final int FORMAT_FIXED = 0;
    public static final int FORMAT_FLOAT = 1;
    public static final int FORMAT_ENG = 2;
    public int CurFormat;
    public int CurNrDecimals;

  /* angle units */
    public static final int ANG_RAD = 1;
    public static final int ANG_DEG = 2;
    public static final int ANG_GRAD = 3;
    public int CurAng;

  /* pending-operation stack */
    public final static int STACKOP_ADD = 1;
    public final static int STACKOP_SUB = 2;
    public final static int STACKOP_MUL = 3;
    public final static int STACKOP_DIV = 4;
    public final static int STACKOP_MOD = 5;
    public final static int STACKOP_EXP = 6;
    public final static int STACKOP_ROOT = 7;

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

    public static class ProgBank
      {
        byte[] Program;
        java.util.Map<Integer, Integer> Labels;
          /* mapping from symbolic codes to program locations */
        android.graphics.Bitmap Card; /* card image to display when bank is selected, can be null */
        byte[] Help; /* HTML help to display, can be null */

        public ProgBank
          (
            byte[] Program,
            android.graphics.Bitmap Card,
            byte[] Help
          )
          {
            this.Program = Program;
            this.Card = Card;
            this.Help = Help;
            this.Labels = null;
          } /*ProgBank*/

      } /*ProgBank*/

    public boolean ProgMode; /* true for program-entry mode, false for calculation mode */
    public final int MaxMemories = 100; /* maximum addressable */
    public final int MaxProgram = 960; /* absolute max on original model */
    public final int MaxBanks = 100;
      /* 00 is user-entered program, others are loaded from library modules */
    public final int MaxFlags = 10;
    public final double[] Memory;
    public final byte[] Program;
    public final ProgBank[] Bank; /* Bank[0].Program always points to Program */
    public byte[] ModuleHelp; /* overall help for loaded library module */
    public final boolean[] Flag;

  /* special flag numbers: */
    public final static int FLAG_ERROR_COND = 7;
      /* can be set by Op 18/19 to indicate error/no-error, and
        by Op 40 to indicate printer present */
    public final static int FLAG_STOP_ON_ERROR = 8; /* if set, program stops on error */
    public final static int FLAG_TRACE_PRINT = 9; /* if set, calculation is traced on printer */

    public int PC, RunPC, CurBank, RunBank, NextBank;
    public int RegOffset;
    public boolean TaskRunning; /* program currently executing */
    boolean ProgRunningSlowly; /* executing program pauses to show intermediate result */
    boolean AllowRunningSlowly;
    boolean SaveRunningSlowly; /* for one-off pauses */

  /* use of memories for stats operations */
    public static final int STATSREG_SIGMAY = 1;
    public static final int STATSREG_SIGMAY2 = 2;
    public static final int STATSREG_N = 3;
    public static final int STATSREG_SIGMAX = 4;
    public static final int STATSREG_SIGMAX2 = 5;
    public static final int STATSREG_SIGMAXY = 6;
    public static final int STATSREG_FIRST = 1; /* lowest-numbered memory used for stats */
    public static final int STATSREG_LAST = 6; /* highest-numbered memory used for stats */

    public static class ReturnStackEntry
      {
        public int BankNr, Addr;
        public boolean FromInteractive;

        public ReturnStackEntry
          (
            int BankNr,
            int Addr,
            boolean FromInteractive
          )
          {
            this.BankNr = BankNr;
            this.Addr = Addr;
            this.FromInteractive = FromInteractive;
          } /*ReturnStackEntry*/

      } /*ReturnStackEntry*/

    public final int MaxReturnStack = 6;
    public ReturnStackEntry[] ReturnStack; /* for subroutine calls */
    public int ReturnLast; /* top of ReturnStack */

    String LastShowing = null;

    public final byte[] PrintRegister;

    public void Reset
      (
        boolean ClearLibs /* wipe out loaded library module as well */
      )
      /* resets to power-up/blank state. */
      {
        CurFormat = FORMAT_FIXED;
        CurNrDecimals = -1;
        CurAng = ANG_DEG;
        OpStackNext = 0;
        X = 0.0;
        T = 0.0;
        PC = 0;
        RunPC = 0;
        CurBank = 0;
        RegOffset = 0;
        ReturnLast = -1;
        ClearImport();
        TaskRunning = false;
        ProgRunningSlowly = false;
        AllowRunningSlowly = false;
        ResetLabels();
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
        if (ClearLibs)
          {
            ResetLibs();
          } /*if*/
        ProgMode = false;
        for (int i = 0; i < PrintRegister.length; ++i)
          {
            PrintRegister[i] = 0;
          } /*for*/
        ResetEntry();
      } /*Reset*/

    public void ResetLibs()
      /* wipes out loaded library modules */
      {
        for (int i = 1; i < MaxBanks; ++i)
          {
            if (Bank[i] != null && Bank[i].Card != null)
              {
              /* avoid "bitmap allocation exceeds budget" crashes */
                if (CurBank == i)
                  {
                    Global.Label.SetHelp(null, null);
                  } /*if*/
                Bank[i].Card.recycle();
              } /*if*/
            Bank[i] = null;
          } /*for*/
        ModuleHelp = null;
      } /*ResetLibs*/

    public State
      (
        android.content.Context ctx
      )
      {
        this.ctx = ctx;
        OpStack = new OpStackEntry[MaxOpStack];
        Memory = new double[MaxMemories];
        Program = new byte[MaxProgram];
        Bank = new ProgBank[MaxBanks];
        Bank[0] = new ProgBank(Program, null, null);
        Flag = new boolean[MaxFlags];
        BGTask = new android.os.Handler();
        ExecuteTask = new TaskRunner();
        ReturnStack = new ReturnStackEntry[MaxReturnStack];
        PrintRegister = new byte[Printer.CharColumns];
        Reset(true);
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
        if (!TaskRunning || ProgRunningSlowly)
          {
            if (InErrorState())
              {
                Global.Disp.SetShowingError(ToDisplay);
              }
            else
              {
                Global.Disp.SetShowing(ToDisplay);
              } /*if*/
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

    public void SetErrorState
      (
        boolean AlsoStopProgram
      )
      {
        ClearDelayedStep();
        if (!TaskRunning || ProgRunningSlowly)
          {
            Global.Disp.SetShowingError(LastShowing);
          } /*if*/
        CurState = ErrorState;
        if (AlsoStopProgram || Flag[FLAG_STOP_ON_ERROR])
          {
            StopProgram();
          } /*if*/
      } /*SetErrorState*/

    public boolean InErrorState()
      {
        return
            CurState == ErrorState;
      } /*InErrorState*/

    public boolean ImportInProgress()
      {
        return
            Import != null;
      } /*ImportInProgress*/

    public void ClearAll()
      {
        OpStackNext = 0;
        ResetEntry();
      } /*ClearAll*/

    public void ClearEntry()
      {
        if (CurState != ResultState)
          {
            if (CurState == ErrorState)
              {
                CurState = ResultState;
                SetX(X);
              }
            else
              {
                ResetEntry();
              } /*if*/
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
        SetShowing(CurDisplay);
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

    static int FormatToUse
      (
        double X,
        int OrigFormat
      )
      /* returns OrigFormat, or replaces FORMAT_FIXED with FORMAT_FLOAT if X
        is outside the suitable range for FORMAT_FIXED. */
      {
        int UseFormat = OrigFormat;
        if
          (
                OrigFormat == FORMAT_FIXED
            &&
                X != 0
            &&
                (
                    Math.abs(X) < 5.0 * Math.pow(10.0, -9.0)
                ||
                    Math.abs(X) >= Math.pow(10.0, 10.0)
                )
          )
          {
            UseFormat = FORMAT_FLOAT;
          } /*if*/
        return
            UseFormat;
      } /*FormatToUse*/

    static int ScaleExp
      (
        double X,
        int UsingFormat
      )
      /* returns the exponent scale to display X using the given format. */
      {
        int Exp = 0;
        X = Math.abs(X);
        if (X != 0.0)
          {
            switch (UsingFormat)
              {
            case FORMAT_FLOAT:
                Exp = (int)Math.floor(Math.log(X) / Math.log(10.0));
            break;
            case FORMAT_ENG:
                Exp = (int)Math.floor(Math.log(X) / Math.log(1000.0)) * 3;
            break;
              } /*switch*/
          } /*if*/
        return
            Exp;
      } /*ScaleExp*/

    public void EnterExponent()
      {
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
            if (InvState)
              {
                if (CurFormat != FORMAT_FIXED)
                  {
                    CurFormat = FORMAT_FIXED;
                    CurNrDecimals = -1;
                    SetX(X); /* will cause redisplay */
                  } /*if*/
              }
            else
              {
               /* as per manual */
                SetX
                  (
                    Arith.RoundTo
                      (
                        X,
                        CurFormat == FORMAT_FIXED ?
                            CurNrDecimals >= 0 ? CurNrDecimals : 10
                        :
                            8
                      )
                  );
                CurFormat = FORMAT_FLOAT; /* but display of X is not changed yet */
              } /*if*/
        break;
          } /*switch*/
      } /*EnterExponent*/

    static String FormatNumber
      (
        double X,
        int UseFormat,
        int NrDecimals,
        boolean ExponentPad /* leave spaces if exponent is omitted */
      )
      /* formats X for display according to the specified settings. */
      {
        String Result = null;
        UseFormat = FormatToUse(X, UseFormat);
        final int Exp = ScaleExp(X, UseFormat);
        final int BeforeDecimal = Arith.FiguresBeforeDecimal(X, Exp);
        switch (UseFormat)
          {
        case FORMAT_FLOAT:
        case FORMAT_ENG:
            Result = String.format
              (
                Global.StdLocale,
                String.format(Global.StdLocale, "%%.%df", Math.max(8 - BeforeDecimal, 0)),
                X / Math.pow(10.0, Exp)
              );
          /* assume there will always be a decimal point? */
            Result += (Exp < 0 ? "-" : " ") + String.format(Global.StdLocale, "%02d", Math.abs(Exp));
        break;
        case FORMAT_FIXED:
            if (NrDecimals >= 0)
              {
                Result = String.format
                  (
                    Global.StdLocale,
                    String.format
                      (
                        Global.StdLocale,
                        "%%.%df",
                        Math.max(Math.min(NrDecimals, 10 - BeforeDecimal), 0)
                      ),
                    X
                  );
              }
            else
              {
                final int UseNrDecimals = Math.max(10 - BeforeDecimal, 0);
                Result = String.format
                  (
                    Global.StdLocale,
                    String.format(Global.StdLocale, "%%.%df", UseNrDecimals),
                    X
                  );
                if (UseNrDecimals > 0)
                  {
                    while
                      (
                            Result.length() != 0
                        &&
                            Result.charAt(Result.length() - 1) == '0'
                      )
                      {
                        Result = Result.substring(0, Result.length() - 1);
                      } /*while*/
                  }
                else
                  {
                    Result += ".";
                  } /*if*/
              } /*if*/
            if (Result.length() == 0)
              {
                Result = "0.";
              } /*if*/
            if (ExponentPad)
              {
                Result += "   ";
              } /*if*/
        break;
          } /*switch*/
        return
            Result;
      } /*FormatNumber*/

    public void SetX
      (
        double NewX
      )
      /* sets the display to show the specified value. */
      {
        if (CurState != ErrorState)
          {
            CurState = ResultState;
          } /*if*/
        if (!Double.isNaN(NewX) && !Double.isInfinite(NewX))
          {
            X = NewX;
            CurDisplay = FormatNumber(X, CurFormat, CurNrDecimals, false);
            SetShowing(CurDisplay);
          }
        else
          {
            SetErrorState(false);
          } /*if*/
        if (Flag[FLAG_TRACE_PRINT] && Global.Print != null) /* fixme: there are some cases where I don't want to trigger this */
          {
            PrintDisplay(false);
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
        CurNrDecimals = NewNrDecimals >= 0 && NewNrDecimals < 9 ? NewNrDecimals : -1;
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
        case STACKOP_MOD:
            X = Math.IEEEremainder(ThisOp.Operand, X);
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
        case STACKOP_MOD:
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
            SetErrorState(false);
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
        double Scale = 0.0;
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
        if (InvState) /* extension! */
          {
            SetX(TrigScale());
          }
        else
          {
            SetX(Math.PI);
          } /*if*/
      } /*Pi*/

    public void Int()
      {
        Enter();
        final double IntPart = Arith.AbsIntPart(X);
        if (InvState)
          {
            SetX
              (
                Arith.RoundTo
                  (
                    (Math.abs(X) - IntPart) * Math.signum(X),
                    Math.max(Arith.MaxPrec - Arith.FiguresBeforeDecimal(X, 0), 0)
                  )
              );
          }
        else
          {
            SetX(IntPart * Math.signum(X));
          } /*if*/
      } /*Int*/

    public void Abs()
      {
        Enter();
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
        final double Scale = TrigScale();
        double NewX, NewY;
        if (InvState)
          {
            NewX = Math.hypot(X, T);
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
        final double Sign = Math.signum(X);
        final double Degrees = Math.floor(Math.abs(X));
        final double Fraction = Math.abs(X) - Degrees;
        if (InvState)
          {
            final double Minutes = Math.floor(Fraction * 60.0 + 0.001 /*fudge for rounding errors */);
            SetX((Degrees + Minutes / 100.0 + (Fraction * 60.0 - Minutes) * 6 / 1000.0) * Sign);
          }
        else
          {
            final double Minutes = Math.floor(Fraction * 100.0 + 0.1 /*fudge for rounding errors */);
            SetX((Degrees + Minutes / 60.0 + (Fraction * 100.0 - Minutes) / 36.0) * Sign);
          } /*if*/
      } /*D_MS*/

    void ShowCurProg()
      {
        SetShowing
          (
            CurBank != 0 ?
                String.format
                  (
                    Global.StdLocale,
                    "%02d %03d %02d",
                    CurBank,
                    PC,
                    (int)Bank[CurBank].Program[PC]
                  )
            :
                String.format(Global.StdLocale, "%03d %02d", PC, (int)Program[PC])
          );
      } /*ShowCurProg*/

    public void PrintDisplay
      (
        boolean Labelled
      )
      {
        if (Global.Print != null && CurDisplay != null)
          {
            final byte[] Translated = new byte[Printer.CharColumns];
            Global.Print.Translate
              (
                    String.format
                      (
                        Global.StdLocale,
                        String.format
                          (
                            Global.StdLocale,
                            "%%%ds",
                            Math.max(1, 14 - CurDisplay.length())
                          ),
                        " "
                      ).substring(1) /* because I can't have a 0-length format width */
                +
                    CurDisplay
                +
                    (InErrorState() ? "?" : " "),
                Translated
              );
            if (Labelled)
              {
                for (int i = Printer.CharColumns - 5; i < Printer.CharColumns; ++i)
                  {
                    Translated[i] = PrintRegister[i];
                  } /*for*/
              } /*if*/
            Global.Print.Render(Translated);
          } /*if*/
      } /*PrintDisplay*/

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
        ResetLabels();
        T = 0.0;
        for (int i = 0; i < MaxFlags; ++i)
          {
            Flag[i] = false;
          } /*for*/
      /* wipe any loaded help as well */
        if (CurBank == 0)
          {
            Global.Label.SetHelp(null, null);
          } /*if*/
        if (Bank[0].Card != null)
          {
          /* avoid "bitmap allocation exceeds budget" crashes */
            Bank[0].Card.recycle();
          } /*if*/
        Bank[0].Card = null;
        Bank[0].Help = null;
      } /*ClearProgram*/

    public void SelectProgram
      (
        int ProgNr,
        boolean Indirect
      )
      {
        if (ProgNr >= 0)
          {
            boolean OK = false;
            do /*once*/
              {
                if (Indirect)
                  {
                    ProgNr = (ProgNr + RegOffset) % 100;
                    if (ProgNr >= MaxMemories)
                        break;
                    ProgNr = (int)Memory[ProgNr];
                    if (ProgNr < 0)
                        break;
                  } /*if*/
                if (ProgNr >= MaxBanks || Bank[ProgNr] == null)
                    break;
                FillInLabels(ProgNr); /* if not done already */
                if (TaskRunning)
                  {
                    NextBank = ProgNr;
                  }
                else
                  {
                    CurBank = ProgNr;
                    if (Global.Label != null)
                      {
                        Global.Label.SetHelp(Bank[ProgNr].Card, Bank[ProgNr].Help);
                      } /*if*/
                  } /*if*/
              /* all done */
                OK = true;
              }
            while (false);
            if (!OK)
              {
                SetErrorState(true);
              } /*if*/
          } /*if*/
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
                RegNr = (RegNr + RegOffset) % 100;
                if (RegNr >= MaxMemories)
                    break;
                if (Indirect)
                  {
                    RegNr = ((int)Math.round(Memory[RegNr]) + RegOffset) % 100;
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
                SetErrorState(true);
              } /*if*/
          } /*if*/
      } /*MemoryOp*/

    boolean StatsRegsAvailable()
      /* ensures the statistics registers are accessible with the current
        partition/offset setting. */
      {
        return
          /* sufficient to check that first & last reg are within accessible range */
                (RegOffset + STATSREG_FIRST) % 100 < MaxMemories
            &&
                (RegOffset + STATSREG_LAST) % 100 < MaxMemories;
      } /*StatsRegsAvailable*/

    double StatsSlope()
      /* estimated slope from linear regression, used in a lot of other results. */
      {
        double Result;
        if (StatsRegsAvailable())
          {
            Result =
                    (
                        Memory[(RegOffset + STATSREG_SIGMAXY) % 100]
                    -
                            Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                        *
                            Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                        /
                            Memory[(RegOffset + STATSREG_N) % 100]
                    )
                /
                    (
                        Memory[(RegOffset + STATSREG_SIGMAX2) % 100]
                    -
                            Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                        *
                            Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                        /
                            Memory[(RegOffset + STATSREG_N) % 100]
                    );
          }
        else
          {
            SetErrorState(true);
            Result = Double.NaN;
          } /*if*/
        return
            Result;
      } /*StatsSlope*/

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
                    OpNr = (OpNr + RegOffset) % 100;
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
                case 0:
                    for (int i = 0; i < PrintRegister.length; ++i)
                      {
                        PrintRegister[i] = 0;
                      } /*for*/
                    OK = true;
                break;
                case 1:
                case 2:
                case 3:
                case 4:
                      {
                        final int ColStart = (OpNr - 1) * 5;
                        long Contents = (long)Arith.AbsIntPart(X);
                        SetX(Contents); /* manual says fractional part of display is discarded as a side-effect */
                        for (int i = 5;;)
                          {
                            if (i == 0)
                                break;
                            --i;
                            PrintRegister[i + ColStart] = (byte)(Contents % 100);
                            Contents /= 100;
                          } /*for*/
                      }
                    OK = true;
                break;
                case 5:
                    if (Global.Print != null)
                      {
                        Global.Print.Render(PrintRegister);
                      } /*if*/
                    OK = true;
                break;
                case 6:
                    PrintDisplay(true);
                    OK = true;
                break;
                case 7:
                    if (Global.Print != null)
                      {
                        Enter();
                        final int PlotX = (int)X;
                        if (PlotX >= 0 && PlotX < Printer.CharColumns)
                          {
                            final byte[] Plot = new byte[Printer.CharColumns];
                            for (int i = 0; i < Printer.CharColumns; ++i)
                              {
                                Plot[i] = (byte)(i == PlotX ? 51 : 0);
                              } /*for*/
                            Global.Print.Render(Plot);
                          }
                        else
                          {
                            SetErrorState(false);
                          } /*if*/
                      } /*if*/
                    OK = true;
                break;
                case 8:
                    if (!TaskRunning)
                      {
                        StartLabelListing();
                      } /*if*/
                    OK = true;
                break;
                case 9:
                    if (!TaskRunning && CurBank > 0)
                      {
                        final ProgBank Bank = this.Bank[CurBank];
                        if
                          (
                                Bank != null
                            &&
                                Bank.Program != null
                            &&
                                Bank.Program.length <= MaxProgram
                          )
                          {
                            for (int i = 0; i < Bank.Program.length; ++i)
                              {
                                Program[i] = Bank.Program[i];
                              } /*for*/
                            for (int i = Bank.Program.length; i < MaxProgram; ++i)
                              {
                                Program[i] = 0;
                              } /*for*/
                            ResetLabels();
                          }
                        else
                          {
                            SetErrorState(true);
                          } /*if*/
                      } /*if*/
                    OK = true;
                break;
                case 10:
                    SetX(Math.signum(X));
                    OK = true;
                break;
                case 11:
                  /* sample variance */
                    if (StatsRegsAvailable())
                      {
                        T =
                                    Memory[(RegOffset + STATSREG_SIGMAX2) % 100]
                                /
                                    Memory[(RegOffset + STATSREG_N) % 100]
                            -
                                    Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                *
                                    Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                /
                                    (
                                        Memory[(RegOffset + STATSREG_N) % 100]
                                    *
                                        Memory[(RegOffset + STATSREG_N) % 100]
                                    );
                        SetX
                          (
                                    Memory[(RegOffset + STATSREG_SIGMAY2) % 100]
                                /
                                    Memory[(RegOffset + STATSREG_N) % 100]
                            -
                                    Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                *
                                    Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                /
                                    (
                                        Memory[(RegOffset + STATSREG_N) % 100]
                                    *
                                        Memory[(RegOffset + STATSREG_N) % 100]
                                    )
                          );
                        OK = true;
                      } /*if*/
                break;
                case 12:
                  /* slope and intercept */
                    if (StatsRegsAvailable())
                      {
                        final double m = StatsSlope();
                        T = m;
                        SetX
                          (
                                (
                                    Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                -
                                    m * Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                )
                            /
                                Memory[(RegOffset + STATSREG_N) % 100]
                          );
                        OK = true;
                      } /*if*/
                break;
                case 13:
                  /* correlation coefficient */
                    if (StatsRegsAvailable())
                      {
                        SetX
                          (
                                StatsSlope()
                            *
                                Math.sqrt
                                  (
                                        Memory[(RegOffset + STATSREG_SIGMAX2) % 100]
                                    -
                                            Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                        *
                                            Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                        /
                                            Memory[(RegOffset + STATSREG_N) % 100]
                                  )
                            /
                                Math.sqrt
                                  (
                                        Memory[(RegOffset + STATSREG_SIGMAY2) % 100]
                                    -
                                            Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                        *
                                            Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                        /
                                            Memory[(RegOffset + STATSREG_N) % 100]
                                  )
                          );
                        OK = true;
                      } /*if*/
                break;
                case 14:
                  /* estimated y from x */
                    if (StatsRegsAvailable())
                      {
                        final double m = StatsSlope();
                        SetX
                          (
                                m * X
                            +
                                    (
                                        Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                    -
                                        m * Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                    )
                                /
                                    Memory[(RegOffset + STATSREG_N) % 100]
                          );
                        OK = true;
                      } /*if*/
                break;
                case 15:
                  /* estimated x from y */
                    if (StatsRegsAvailable())
                      {
                        final double m = StatsSlope();
                        SetX
                          (
                                (
                                    X
                                -
                                        (
                                            Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                        -
                                            m * Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                        )
                                    /
                                        Memory[(RegOffset + STATSREG_N) % 100]
                                )
                            /
                                m
                          );
                        OK = true;
                      } /*if*/
                break;
                case 17:
                  /* not implemented, fall through */
                case 16:
                    SetX(MaxProgram - 1.0 + (MaxMemories - 1.0) / 100.0);
                    OK = true;
                break;
                case 18:
                case 19:
                    if (OpNr == (InErrorState() ? 19 : 18))
                      {
                        Flag[FLAG_ERROR_COND] = !InvState;
                      /* meaning of INV Op 18/19 taken from “52 Notes” volume 2 number 8 */
                      } /*if*/
                    OK = true;
                break;
              /* 20-39 handled above */
                case 40:
                    if (Global.Print != null)
                      {
                        Flag[FLAG_ERROR_COND] = true;
                      } /*if*/
                    OK = true;
                break;
                case 50: /* extension! */
                    SetX(System.currentTimeMillis() / 1000.0);
                    OK = true;
                break;
                case 51: /* extension! */
                      {
                        byte[] V = new byte[7];
                        Random.nextBytes(V);
                        SetX
                          (
                                (double)(
                                    ((long)V[0] & 255)
                                |
                                    ((long)V[1] & 255) << 8
                                |
                                    ((long)V[2] & 255) << 16
                                |
                                    ((long)V[3] & 255) << 24
                                |
                                    ((long)V[4] & 255) << 32
                                |
                                    ((long)V[5] & 255) << 40
                                |
                                    ((long)V[6] & 255) << 48
                                )
                            /
                                (double)0x0100000000000000L
                          );
                      }
                    OK = true;
                break;
                case 52: /* extension! */
                    SetX(RegOffset);
                    OK = true;
                break;
                case 53: /* extension! */
                      {
                        final int NewRegOffset = (int)Math.round(X);
                        if (NewRegOffset >= 0 && NewRegOffset < 100)
                          {
                            RegOffset = NewRegOffset;
                            OK = true;
                          } /*if*/
                      }
                break;
                  } /*switch*/
              }
            while (false);
            if (!OK)
              {
                SetErrorState(true);
              } /*if*/
          } /*if*/
      } /*SpecialOp*/

    public void StatsSum()
      {
        Enter();
        if (StatsRegsAvailable())
          {
            if (InvState)
              {
              /* remove sample */
                Memory[(RegOffset + STATSREG_SIGMAY) % 100] -= X;
                Memory[(RegOffset + STATSREG_SIGMAY2) % 100] -= X * X;
                Memory[(RegOffset + STATSREG_N) % 100] -= 1.0;
                Memory[(RegOffset + STATSREG_SIGMAX) % 100] -= T;
                Memory[(RegOffset + STATSREG_SIGMAX2) % 100] -= T * T;
                Memory[(RegOffset + STATSREG_SIGMAXY) % 100] -= X * T;
                T -= 1.0;
              }
            else
              {
              /* accumulate sample */
                Memory[(RegOffset + STATSREG_SIGMAY) % 100] += X;
                Memory[(RegOffset + STATSREG_SIGMAY2) % 100] += X * X;
                Memory[(RegOffset + STATSREG_N) % 100] += 1.0;
                Memory[(RegOffset + STATSREG_SIGMAX) % 100] += T;
                Memory[(RegOffset + STATSREG_SIGMAX2) % 100] += T * T;
                Memory[(RegOffset + STATSREG_SIGMAXY) % 100] += X * T;
                T += 1.0;
              } /*if*/
            SetX(Memory[(RegOffset + STATSREG_N) % 100]);
          }
        else
          {
            SetErrorState(true);
          } /*if*/
      } /*StatsSum*/

    public void StatsResult()
      {
        if (StatsRegsAvailable())
          {
            if (InvState)
              {
              /* estimated population standard deviation */
                T =
                    Math.sqrt
                        (
                            (
                                Memory[(RegOffset + STATSREG_SIGMAX2) % 100]
                            -
                                    Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                *
                                    Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                                /
                                    Memory[(RegOffset + STATSREG_N) % 100]
                            )
                        /
                            (Memory[(RegOffset + STATSREG_N) % 100] - 1.0)
                        );
                SetX
                  (
                    Math.sqrt
                        (
                            (
                                Memory[(RegOffset + STATSREG_SIGMAY2) % 100]
                            -
                                    Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                *
                                    Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                                /
                                    Memory[(RegOffset + STATSREG_N) % 100]
                            )
                        /
                            (Memory[(RegOffset + STATSREG_N) % 100] - 1.0)
                        )
                  );
              }
            else
              {
              /* sample mean */
                T =
                        Memory[(RegOffset + STATSREG_SIGMAX) % 100]
                    /
                        Memory[(RegOffset + STATSREG_N) % 100];
                SetX
                  (
                        Memory[(RegOffset + STATSREG_SIGMAY) % 100]
                    /
                        Memory[(RegOffset + STATSREG_N) % 100]
                  );
              } /*if*/
          }
        else
          {
            SetErrorState(true);
          } /*if*/
      } /*StatsResult*/

    public void GetNextImport()
      /* gets next value from current importer, if any. */
      {
        boolean OK = true;
        boolean EOF = true;
        double Value;
        do /*once*/
          {
            if (Import == null)
                break;
            try
              {
                Value = Import.Next();
                EOF = false;
              }
            catch (ImportEOFException Done)
              {
                Import.End();
                break;
              }
            catch (Persistent.DataFormatException Bad)
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ ctx,
                    /*text =*/
                        String.format
                          (
                            Global.StdLocale,
                            ctx.getString(R.string.import_error),
                            Bad.toString()
                          ),
                    /*duration =*/ android.widget.Toast.LENGTH_LONG
                  ).show();
                OK = false;
                Import.End();
                break;
              } /*try*/
            SetX(Value);
            OK = true;
          }
        while (false);
        Flag[FLAG_ERROR_COND] = EOF;
        if (!OK)
          {
            SetErrorState(true);
          } /*if*/
      } /*GetNextImport*/

    public void StepPC
      (
        boolean Forward
      )
      {
        if (Forward)
          {
            if (PC < Bank[CurBank].Program.length - 1)
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

    public void ResetLabels()
      /* invalidates labels because of a change to user-entered program contents. */
      {
        Bank[0].Labels = null;
      } /*ResetLabels*/

    public boolean ProgramWritable()
      /* can the user enter code into the currently-selected bank. */
      {
        return CurBank == 0;
      } /*ProgramWritable*/

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
            SetErrorState(true);
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
        SetProgramStarted(false);
        Interpret(true);
        PC = RunPC;
        SetProgramStopped();
      /* fixme: if I just executed a Pgm nn instruction, this setting
        of NextBank will not be properly passed to the next instruction */
      /* fixme: should single-stepping to a subroutine call cause execution
        of the complete subroutine, stopping when it returns? */
      } /*StepProgram*/

    class TaskRunner implements Runnable
      {
        public void run()
          {
            if (TaskRunning && RunProg != null)
              {
                RunProg.run();
                ContinueTaskRunner();
              } /*if*/
          } /*run*/
      } /*TaskRunner*/

    void ContinueTaskRunner()
      {
        if (TaskRunning)
          {
            if (ProgRunningSlowly)
              {
                Global.Disp.SetShowing(LastShowing);
                BGTask.postDelayed(ExecuteTask, 600);
              }
            else
              {
              /* run as fast as possible */
                BGTask.post(ExecuteTask);
              } /*if*/
          } /*if*/
      } /*ContinueTaskRunner*/

    class ProgRunner implements Runnable
      {
        public void run()
          {
            Interpret(true);
          } /*run*/
      } /*ProgRunner*/

    class LabelLister implements Runnable
      {
        class LabelDef
          {
            int Code;
            int Loc;

            public LabelDef
              (
                int Code,
                int Loc
              )
              {
                this.Code = Code;
                this.Loc = Loc;
              } /*LabelDef*/

          } /*LabelDef*/

        final LabelDef[] SortedLabels;
        int Index;

        public LabelLister()
          {
            super();
            final java.util.TreeSet<LabelDef> SortedLabelsTemp =
                new java.util.TreeSet<LabelDef>
                  (
                    new java.util.Comparator<LabelDef>()
                      {
                        @Override
                        public int compare
                          (
                            LabelDef Label1,
                            LabelDef Label2
                          )
                          {
                            return
                                new Integer(Label1.Loc).compareTo(Label2.Loc);
                          } /*compare*/
                      } /*Comparator*/
                  );
            for (java.util.Map.Entry<Integer, Integer> ThisLabel : Bank[0].Labels.entrySet())
              {
                if (ThisLabel.getValue() >= PC + 2)
                  {
                    SortedLabelsTemp.add(new LabelDef(ThisLabel.getKey(), ThisLabel.getValue()));
                  } /*if*/
              } /*for*/
            SortedLabels = new LabelDef[SortedLabelsTemp.size()];
            int i = 0;
            for (LabelDef ThisLabel : SortedLabelsTemp)
              {
                SortedLabels[i++] = ThisLabel;
              } /*for*/
            Index = 0;
          } /*LabelLister*/

        public void run()
          {
            if (Index < SortedLabels.length)
              {
                final LabelDef ThisLabel = SortedLabels[Index];
                final byte[] Translated = new byte[Printer.CharColumns];
                Global.Print.Translate
                  (
                    String.format
                      (
                        Global.StdLocale,
                        "      %03d  %02d %3s",
                        ThisLabel.Loc - 1,
                          /* seems to match original, pointing at label symbol
                            (location of "Lbl" + 1) */
                        ThisLabel.Code,
                        Printer.KeyCodeSym(ThisLabel.Code)
                      ),
                    Translated
                  );
                Global.Print.Render(Translated);
                ++Index;
              } /*if*/
            if (Index == SortedLabels.length)
              {
                StopTask();
              } /*if*/
          } /*run*/

      } /*LabelLister*/

    class RegisterLister implements Runnable
      {
        int CurReg;
        final int EndReg;
        boolean Wrapped;

        public RegisterLister
          (
            int StartReg
          )
          {
            CurReg = (StartReg + RegOffset) % 100;
            EndReg = (RegOffset + MaxMemories) % 100;
            Wrapped = CurReg < EndReg;
          } /*RegisterLister*/

        public void run()
          {
            if (CurReg == MaxMemories && !Wrapped)
              {
                CurReg = 0;
                Wrapped = true;
              } /*if*/
            if (CurReg < (Wrapped ? EndReg : MaxMemories))
              {
                final byte[] Translated = new byte[Printer.CharColumns];
                Global.Print.Translate
                  (
                    String.format
                      (
                        Global.StdLocale,
                        "%16s  %02d",
                        FormatNumber(Memory[CurReg], FORMAT_FIXED, -1, true),
                        CurReg /* post-RegOffset number */
                      ),
                    Translated
                  );
                Global.Print.Render(Translated);
                ++CurReg;
              } /*if*/
            if (Wrapped && CurReg >= EndReg)
              {
                StopTask();
              } /*if*/
          } /*run*/

      } /*RegisterLister*/

    class ProgramLister implements Runnable
      {
        int ListPC, EndPC;
        int Expecting;
        boolean InvState;
      /* following original, state machine is somewhat simpler than full interpreter/disassembler */
        final int ExpectOpcode = 0;
        final int ExpectTwoDigits = 1;
        final int ExpectLoc = 2;
        final int ExpectRegFlag = 3;
        final int ExpectTwoDigitsPlusLoc = 4;
        final int ExpectRegPlusLoc = 5;
        final int ExpectSym = 6;

        public ProgramLister()
          {
            ListPC = PC;
            EndPC = MaxProgram;
            for (;;) /* omit trailing zero bytes */
              {
                if (EndPC == 0)
                    break;
                --EndPC;
                if (Program[EndPC] != 0)
                    break;
              } /*for*/
            Expecting = ExpectOpcode;
            InvState = false;
          } /*ProgramLister*/

        public void run()
          {
            if (ListPC < MaxProgram)
              {
                final int Val = Program[ListPC];
                String Symbol = Printer.KeyCodeSym(Val);
                int NextExpecting = ExpectOpcode;
                boolean WasModifier = false;
                switch (Expecting)
                  {
                case ExpectOpcode:
                    if (Val < 10)
                      {
                      /* digit entry */
                        Symbol = String.format(Global.StdLocale, " %1d ", Val);
                      } /*if*/
                    switch (Val)
                      {
                    case 22: /*INV*/
                  /* case 27: */ /*?*/
                        InvState = !InvState;
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
                        NextExpecting = ExpectTwoDigits;
                    break;
                    case 57: /*Fix*/
                        if (!InvState)
                          {
                            NextExpecting = ExpectRegFlag;
                          } /*if*/
                    break;
                    case 61: /*GTO*/
                        NextExpecting = ExpectLoc;
                    break;
                    case 71: /*SBR*/
                        if (!InvState)
                          {
                            NextExpecting = ExpectLoc;
                          } /*if*/
                    break;
                    case 67: /*x=t*/
                    case 77: /*x≥t*/
                        NextExpecting = ExpectLoc;
                    break;
                    case 76: /*Lbl*/
                        NextExpecting = ExpectSym;
                    break;
                    case 86: /*St flg*/
                        NextExpecting = ExpectRegFlag;
                    break;
                    case 87: /*If flg*/
                    case 97: /*Dsz*/
                        NextExpecting = ExpectRegPlusLoc;
                    break;
                      } /*switch*/
                break;
                case ExpectTwoDigits:
                    Symbol = String.format(Global.StdLocale, " %02d", Val);
                break;
                case ExpectLoc:
                    if (Val < 10 || Val == 40)
                      {
                        NextExpecting = ExpectTwoDigits;
                      } /*if*/
                break;
                case ExpectRegFlag:
                    if (Val == 40)
                      {
                        NextExpecting = ExpectTwoDigits;
                      }
                    else
                      {
                        Symbol = String.format(Global.StdLocale, " %02d", Val);
                      } /*if*/
                break;
                case ExpectTwoDigitsPlusLoc:
                    Symbol = String.format(Global.StdLocale, " %02d", Val);
                    NextExpecting = ExpectLoc;
                break;
                case ExpectRegPlusLoc:
                    if (Val == 40)
                      {
                        NextExpecting = ExpectTwoDigitsPlusLoc;
                      }
                    else
                      {
                        Symbol = String.format(Global.StdLocale, " %02d", Val);
                        NextExpecting = ExpectLoc;
                      } /*if*/
                break;
                case ExpectSym:
                    Symbol = Printer.KeyCodeSym(Val);
                break;
                  } /*switch*/
                if (!WasModifier)
                  {
                    InvState = false;
                  } /*if*/
                final byte[] Translated = new byte[Printer.CharColumns];
                Global.Print.Translate
                  (
                    String.format
                      (
                        Global.StdLocale,
                        "       %03d  %02d %3s  ",
                        ListPC,
                        Val,
                        Symbol
                      ),
                    Translated
                  );
                Global.Print.Render(Translated);
                Expecting = NextExpecting;
                ++ListPC;
              } /*if*/
            if (ListPC == MaxProgram || ListPC > EndPC && Expecting == ExpectOpcode)
              {
                StopTask();
              } /*if*/
          } /*run*/

      } /*ProgramLister*/

    void SetShowingRunning()
      {
        Global.Disp.SetShowingRunning(Import != null || Global.Export.IsOpen() ? 'c' : 'C');
      } /*SetShowingRunning*/

    void SetProgramStarted
      (
        boolean AllowRunningSlowly
      )
      {
        ClearDelayedStep();
        FillInLabels(CurBank);
        ProgRunningSlowly = false; /* just in case */
        this.AllowRunningSlowly = AllowRunningSlowly;
        SaveRunningSlowly = false;
        TaskRunning = true;
        SetShowingRunning();
        RunPC = PC;
        RunBank = CurBank;
        NextBank = RunBank;
      } /*SetProgramStarted*/

    void SetProgramStopped()
      {
        TaskRunning = false;
        ClearDelayedStep();
        RunBank = CurBank;
        if (InErrorState())
          {
            Global.Disp.SetShowingError(LastShowing);
          }
        else
          {
            Global.Disp.SetShowing(LastShowing);
          } /*if*/
      } /*SetProgramStopped*/

    void StartTask
      (
        Runnable TheTask,
        boolean AllowRunningSlowly
      )
      {
        RunProg = TheTask;
        SetProgramStarted(AllowRunningSlowly);
        ContinueTaskRunner();
      } /*StartTask*/

    void StopTask()
      {
        SetProgramStopped();
        BGTask.removeCallbacks(ExecuteTask);
        RunProg = null;
      } /*StopTask*/

    public void StartProgram()
      {
        StartTask(new ProgRunner(), true);
      } /*StartProgram*/

    public void StopProgram()
      {
        if (TaskRunning && OnStop != null)
          {
            OnStop.run();
          } /*if*/
        PC = RunPC;
        StopTask();
      } /*StopProgram*/

    void StartLabelListing()
      {
        FillInLabels(0); /* because that's the one I list */
        StartTask(new LabelLister(), false);
      } /*StartLabelListing*/

    public void StartRegisterListing()
      {
        Enter();
        StartTask(new RegisterLister((int)X), false);
      } /*StartRegisterListing*/

    public void StartProgramListing()
      {
        StartTask(new ProgramLister(), false);
      } /*StartProgramListing*/

    public void SetSlowExecution
      (
        boolean Slow
      )
      {
        if (AllowRunningSlowly && ProgRunningSlowly != Slow)
          {
            ProgRunningSlowly = Slow;
            SaveRunningSlowly = Slow;
            if (!ProgRunningSlowly)
              {
                SetShowingRunning();
              } /*if*/
          } /*if*/
      } /*SetSlowExecution*/

    public void SetImport
      (
        ImportFeeder NewImport
      )
      {
        if (Import != null)
          {
            throw new RuntimeException("attempt to queue multiple ImportFeeders");
          } /*if*/
        Import = NewImport;
      } /*SetImport*/

    public void ClearImport()
      {
        if (Import != null)
          {
            Import.End();
            Import = null;
          } /*if*/
      } /*ClearImport*/

    public void ResetReturns()
      /* clears the subroutine return stack. */
      {
        ReturnLast = -1;
      } /*ResetReturns*/

    public void ResetProg()
      {
        if (InvState) /* extension! */
          {
            ClearImport();
            if (Global.Export != null)
              {
                Global.Export.Close();
              } /*if*/
          }
        else
          {
            for (int i = 0; i < MaxFlags; ++i)
              {
                Flag[i] = false;
              } /*for*/
            if (TaskRunning)
              {
                RunPC = 0;
              }
            else
              {
                PC = 0;
              } /*if*/
            ResetReturns();
          } /*if*/
      } /*ResetProg*/

    int GetProg
      (
        boolean Executing
      )
      /* returns the next program instruction byte, or -1 if run off the end. */
      {
        byte Result;
        if (RunPC < Bank[RunBank].Program.length)
          {
            Result = Bank[RunBank].Program[RunPC++];
          }
        else
          {
            Result = -1;
            if (Executing)
              {
                RunPC = 0;
                StopProgram();
              } /*if*/
          } /*if*/
        return
            (int)Result;
      } /*GetProg*/

    int GetLoc
      (
        boolean Executing,
        int BankNr /* for interpreting symbolic label */
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
                final int Reg = (GetProg(Executing) + RegOffset) % 100;
                if (Reg >= 0 && Reg < MaxMemories)
                  {
                    Result = (int)Memory[Reg];
                    if (Result < 0 || Result >= Bank[RunBank].Program.length)
                      {
                        Result = -1;
                      } /*if*/
                  } /*if*/
              }
            else /* symbolic label */
              {
                if (Bank[BankNr].Labels.containsKey(NextByte))
                  {
                    Result = Bank[BankNr].Labels.get(NextByte);
                  } /*if*/
              } /*if*/
          } /*if*/
        if (Executing && Result < 0)
          {
            SetErrorState(true);
          } /*if*/
        return
            Result;
      } /*GetLoc*/

    int GetUnitOp
      (
        boolean Executing,
        boolean AllowLarge /* allow value > 9 */
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
            if (NextByte == 40)
              {
                final int Reg = (GetProg(Executing) + RegOffset) % 100;
                if (Reg >= 0 && Reg < MaxMemories)
                  {
                    Result = (int)Memory[Reg];
                    OK = Result >= 0 && Result < MaxMemories;
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
            else if (AllowLarge || NextByte < 10)
              {
                Result = NextByte;
                OK = true;
              }
            else
              {
                OK = false;
              } /*if*/
            if (Executing && !OK)
              {
                SetErrorState(true);
              } /*if*/
          } /*if*/
        return
            Result;
      } /*GetUnitOp*/

  /* transfer types */
    public static final int TRANSFER_TYPE_GTO = 1; /* goto that address */
    public static final int TRANSFER_TYPE_CALL = 2; /* call that address, return to current address */
    public static final int TRANSFER_TYPE_INTERACTIVE_CALL = 3;
      /* call that address, return to calculation mode */
    public static final int TRANSFER_TYPE_LEA = 4; /* extension: load that address into X */

  /* location types */
    public static final int TRANSFER_LOC_DIRECT = 1; /* Loc is integer address */
    public static final int TRANSFER_LOC_SYMBOLIC = 2; /* Loc is label keycode */
    public static final int TRANSFER_LOC_INDIRECT = 3; /* Loc is number of register containing address */

    public void Transfer
      (
        int Type, /* one of the above TRANSFER_TYPE_xxx values */
        int BankNr,
        int Loc,
        int LocType /* one of the above TRANSFER_LOC_xxx values */
      )
      /* implements GTO and SBR. Also called from other functions to implement branches. */
      {
        if (Loc >= 0)
          {
            boolean OK = false;
            do /*once*/
              {
                if (LocType == TRANSFER_LOC_INDIRECT)
                  {
                    Loc = (Loc + RegOffset) % 100;
                    if (Loc >= MaxMemories)
                        break;
                    Loc = (int)Memory[Loc];
                  }
                else if (LocType == TRANSFER_LOC_SYMBOLIC)
                  {
                    FillInLabels(BankNr); /* if not already done */
                    if (!Bank[BankNr].Labels.containsKey(Loc))
                        break;
                    Loc = Bank[BankNr].Labels.get(Loc);
                  } /*if*/
                if (Loc < 0 || Loc >= Bank[BankNr].Program.length)
                    break;
                if (Type == TRANSFER_TYPE_LEA) /* extension! */
                  {
                    SetX(Loc);
                  }
                else
                  {
                    if (Type == TRANSFER_TYPE_CALL || Type == TRANSFER_TYPE_INTERACTIVE_CALL)
                      {
                        if (ReturnLast == MaxReturnStack - 1)
                            break;
                        ReturnStack[++ReturnLast] =
                            new ReturnStackEntry(RunBank, RunPC, Type == TRANSFER_TYPE_INTERACTIVE_CALL);
                      } /*if*/
                    if (TaskRunning)
                      {
                        RunBank = BankNr;
                        RunPC = Loc;
                      }
                    else
                      {
                      /* interactive, assert BankNr = CurBank */
                        PC = Loc;
                      } /*if*/
                    if (Type == TRANSFER_TYPE_INTERACTIVE_CALL)
                      {
                        StartProgram();
                      } /*if*/
                  } /*if*/
              /* all successfully done */
                OK = true;
              }
            while (false);
            if (!OK)
              {
                SetErrorState(true);
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
            if
              (
                    ReturnTo.Addr < 0
                ||
                    Bank[ReturnTo.BankNr] == null
                ||
                    ReturnTo.Addr >= Bank[ReturnTo.BankNr].Program.length
              )
                break;
            if (TaskRunning)
              {
                RunBank = ReturnTo.BankNr;
                RunPC = ReturnTo.Addr;
              }
            else
              {
                CurBank = ReturnTo.BankNr;
                PC = ReturnTo.Addr;
              } /*if*/
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
            SetErrorState(true);
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
                FlagNr = (FlagNr + RegOffset) % 100;
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
                SetErrorState(true);
              } /*if*/
          } /*if*/
      } /*SetFlag*/

    public void BranchIfFlag
      (
        int FlagNr,
        boolean FlagNrInd,
        int Bank,
        int Target,
        int TargetType /* one of the above TRANSFER_LOC_xxx values */
      )
      {
        if (FlagNr >= 0 && Target >= 0)
          {
            if (FlagNrInd)
              {
                FlagNr = (FlagNr + RegOffset) % 100;
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
                    Transfer
                      (
                        /*Type =*/ TRANSFER_TYPE_GTO,
                        /*BankNr =*/ Bank,
                        /*Loc =*/ Target,
                        /*LocType =*/ TargetType
                      );
                  } /*if*/
              }
            else
              {
                SetErrorState(true);
              } /*if*/
          } /*if*/
      } /*BranchIfFlag*/

    public void CompareBranch
      (
        boolean Greater,
        int Bank,
        int NewPC,
        boolean Ind
      )
      {
        if (NewPC >= 0)
          {
            Enter();
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
                Transfer
                  (
                    /*Type =*/ TRANSFER_TYPE_GTO,
                    /*BankNr =*/ Bank,
                    /*Loc =*/ NewPC,
                    /*LocType =*/ Ind ? TRANSFER_LOC_INDIRECT : TRANSFER_LOC_DIRECT
                  );
              } /*if*/
          } /*if*/
      } /*CompareBranch*/

    public void DecrementSkip
      (
        int Reg,
        boolean RegInd,
        int Bank,
        int Target,
        int TargetType /* one of the above TRANSFER_LOC_xxx values */
      )
      {
        if (Reg >= 0 && Target >= 0)
          {
            Reg = (Reg + RegOffset) % 100;
            if (RegInd)
              {
                if (Reg < MaxMemories)
                  {
                    Reg = ((int)Memory[Reg] + RegOffset) % 100;
                  }
                else
                  {
                    Reg = -1;
                  } /*if*/
              } /*if*/
            if (Reg >= 0 && Reg < MaxMemories)
              {
                Memory[Reg] = Math.max(Math.abs(Memory[Reg]) - 1.0, 0.0) * Math.signum(Memory[Reg]);
                if (InvState == (Memory[Reg] == 0.0))
                  {
                    Transfer
                      (
                        /*Type =*/ TRANSFER_TYPE_GTO,
                        /*BankNr =*/ Bank,
                        /*Loc =*/ Target,
                        /*LocType =*/ TargetType
                      );
                  } /*if*/
              }
            else
              {
                SetErrorState(true);
              } /*if*/
          } /*if*/
      } /*DecrementSkip*/

    void Interpret
      (
        boolean Execute /* false to just collect labels */
      )
      /* main program interpreter loop: interprets one instruction and
        advances/jumps RunPC accordingly. */
      {
        final int Op = GetProg(Execute);
        if (Op >= 0)
          {
            boolean WasModifier = false;
            if (Execute)
              {
                boolean BankSet = false;
                ProgRunningSlowly = SaveRunningSlowly; /* undo previous Pause, if any */
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
                    Transfer(TRANSFER_TYPE_CALL, NextBank, Op, TRANSFER_LOC_SYMBOLIC);
                break;
              /* 21 invalid */
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
              /* 26 invalid */
              /* 27 same as 22 */
                case 28:
                    Log();
                break;
                case 29: /*CP*/
                    T = 0.0;
                break;
              /* 20 same as 25 */
              /* 31 invalid */
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
                    SelectProgram(GetProg(true), false);
                    BankSet = true;
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
              /* 41 invalid */
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
              /* 46 invalid */
                case 47:
                    ClearMemories();
                break;
                case 48:
                    MemoryOp(MEMOP_EXC, GetProg(true), false);
                break;
                case 49:
                    MemoryOp(MEMOP_MUL, GetProg(true), false);
                break;
              /* 51 invalid */
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
                    if (InvState) /* extension! */
                      {
                        Operator(STACKOP_MOD);
                      }
                    else
                      {
                        Operator(STACKOP_DIV);
                      } /*if*/
                break;
              /* 56 invalid */
                case 57: /*Eng*/
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
                    Transfer
                      (
                        /*Type =*/
                            InvState ?
                                TRANSFER_TYPE_LEA /*extension!*/
                            :
                                TRANSFER_TYPE_GTO,
                        /*BankNr =*/ NextBank,
                        /*Loc =*/ GetLoc(true, NextBank),
                        /*LocType =*/ TRANSFER_LOC_DIRECT
                      );
                break;
                case 62: /*Pgm Ind*/
                    SelectProgram(GetProg(true), true);
                    BankSet = true;
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
                    ProgRunningSlowly = true; /* will revert to SaveRunningSlowly next time */
                break;
                case 67: /*x=t*/
                case 77: /*x≥t*/
                    CompareBranch(Op == 77, RunBank, GetLoc(true, RunBank), false);
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
                        Transfer(TRANSFER_TYPE_CALL, NextBank, GetLoc(true, NextBank), TRANSFER_LOC_DIRECT);
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
                break;
              /* 82 invalid */
                case 83: /*GTO Ind*/
                    Transfer
                      (
                        /*Type =*/
                            InvState ?
                                TRANSFER_TYPE_LEA /*extension!*/
                            :
                                TRANSFER_TYPE_GTO,
                        /*BankNr =*/ NextBank,
                        /*Loc =*/ GetLoc(true, NextBank),
                        /*LocType =*/ TRANSFER_LOC_INDIRECT
                      );
                break;
                case 84:
                    SpecialOp(GetProg(true), true);
                break;
                case 85:
                    Operator(STACKOP_ADD);
                break;
                case 86: /*St flg*/
                    SetFlag(GetUnitOp(true, false), false, !InvState);
                break;
                case 87: /*If flg*/
                      {
                        final int FlagNr = GetUnitOp(true, false);
                        final int Target = GetLoc(true, RunBank);
                        BranchIfFlag(FlagNr, false, RunBank, Target, TRANSFER_LOC_DIRECT);
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
                        final int Reg = GetUnitOp(true, true);
                        final int Target = GetLoc(true, RunBank);
                        DecrementSkip(Reg, false, RunBank, Target, TRANSFER_LOC_DIRECT);
                      }
                break;
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
                    if (InvState) /* extension! */
                      {
                        GetNextImport();
                      }
                    else
                      {
                        if (Global.Export != null && Global.Export.NumbersOnly)
                          {
                            Global.Export.WriteNum(X);
                          } /*if*/
                        PrintDisplay(false);
                      } /*if*/
                break;
                case 90: /*List*/
                  /* no-op in program? */
                break;
                default:
                    SetErrorState(true);
                break;
                  } /*switch*/
                if (!BankSet)
                  {
                    NextBank = RunBank;
                  } /*if*/
              }
            else
              {
              /* just advance RunPC past instruction and update Labels as appropriate */
                switch (Op)
                  {
                case 22:
                case 27:
                    InvState = !InvState; /* needed to correctly parse INV Fix and unmerged INV SBR */
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
                  /* one byte following */
                    GetProg(false);
                break;
                case 57: /*Fix*/
                    if (!InvState)
                      {
                        GetUnitOp(false, false);
                      } /*if*/
                break;
                case 61: /*GTO*/
                    GetLoc(false, RunBank /* irrelevant */);
                break;
                case 67: /*x=t*/
                case 77: /*x≥t*/
                    GetLoc(false, RunBank);
                break;
                case 71: /*SBR*/
                    if (!InvState) /* in case it wasn't merged */
                      {
                        GetLoc(false, RunBank /* irrelevant */);
                      } /*if*/
                break;
                case 76: /*Lbl*/
                      {
                        final int TheLabel = GetProg(false);
                        if (TheLabel >= 0 && RunPC >= 0 && !Bank[RunBank].Labels.containsKey(TheLabel))
                          {
                            Bank[RunBank].Labels.put(TheLabel, RunPC);
                          } /*if*/
                      }
                break;
                case 86: /*St flg*/
                    GetUnitOp(false, false);
                break;
                case 87: /*If flg*/
                case 97: /*Dsz*/
                    GetUnitOp(false, true); /*register/flag*/
                    GetLoc(false, RunBank); /*branch target*/
                break;
                  } /*switch*/
              } /*if*/
            if (!WasModifier)
              {
                InvState = false;
              } /*if*/
          } /*if*/
      } /*Interpret*/

    void FillInLabels
      (
        int BankNr
      )
      {
        if (Bank[BankNr].Labels == null)
          {
            Bank[BankNr].Labels = new java.util.HashMap<Integer, Integer>();
            final boolean SaveInvState = InvState;
            final int SaveRunPC = RunPC;
            final int SaveRunBank = RunBank;
            InvState = false; /*?*/
            RunPC = 0;
            RunBank = BankNr;
            do
              {
                Interpret(false);
              }
            while (RunPC != -1 && RunPC < Bank[BankNr].Program.length);
            InvState = SaveInvState;
            RunPC = SaveRunPC;
            RunBank = SaveRunBank;
          } /*if*/
      } /*FillInLabels*/

    public void FillInLabels()
      {
        FillInLabels(CurBank);
      } /*FillInLabels*/

  } /*State*/
