package nz.gen.geek_central.ti5x;
/*
    Saving/loading of libraries and calculator state

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

import java.util.zip.ZipEntry;

class ZipComponentWriter
  /* convenient writing of components to a ZIP archive, with automatic
    calculation of size and CRC fields. */
  {
    protected java.util.zip.ZipOutputStream Parent;
    protected ZipEntry Entry;
    public java.io.ByteArrayOutputStream Out;

    public ZipComponentWriter
      (
        java.util.zip.ZipOutputStream Parent,
        String Name,
        boolean Compressed
      )
      {
        this.Parent = Parent;
        Entry = new ZipEntry(Name);
        Entry.setMethod
          (
            Compressed ?
                ZipEntry.DEFLATED
            :
                ZipEntry.STORED
          );
        Out = new java.io.ByteArrayOutputStream();
      } /*ZipComponentWriter*/

    public void write
      (
        byte[] buffer,
        int offset,
        int len
      )
      /* writes more data making up the component. */
      {
        Out.write(buffer, offset, len);
      } /*write*/

    public void write
      (
        byte[] buffer
      )
      {
        write(buffer, 0, buffer.length);
      } /*write*/

    public void write
      (
        String data
      )
      {
        write(data.getBytes());
      } /*write*/

    public void close()
      /* finalizes output of this archive component. */
        throws java.io.IOException
      {
        Out.close();
        final byte[] TheData = Out.toByteArray();
        Entry.setSize(TheData.length);
        final java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(TheData);
        Entry.setCrc(crc.getValue());
        Parent.putNextEntry(Entry);
        Parent.write(TheData, 0, TheData.length);
        Parent.closeEntry();
      } /*close*/
  
  } /*ZipComponentWriter*/

public class Persistent
  /* saving/loading of libraries and calculator state. */
  {
    public static final String CalcMimeType = "application/vnd.nz.gen.geek_central.ti5x";
    public static final String StateExt = ".ti5s"; /* for saved calculator state */
    public static final String ProgExt = ".ti5p"; /* for saved user program */
    public static final String LibExt = ".ti5l"; /* for library */
    public static final String ProgramsDir = "Programs"; /* where to save user programs */
    public static final String[] ExternalCalcDirectories =
      /* where to load programs/libraries from */
        {
            ProgramsDir,
            "Download",
        };
    public static final String SavedStateName = "state" + StateExt;

    public static class DataFormatException extends RuntimeException
      /* indicates a problem parsing a saved state file. */
      {

        public DataFormatException
          (
            String Message
          )
          {
            super(Message);
          } /*DataFormatException*/

      } /*DataFormatException*/

    static final java.util.Locale StdLocale = java.util.Locale.US;

    static void SaveBool
      (
        java.io.PrintStream POut,
        String Name,
        boolean Value,
        int Indent
      )
      {
        POut.printf(StdLocale, String.format(StdLocale, "%%%ds", Indent), " ");
        POut.printf(StdLocale, "<param name=\"%s\" value=\"%s\"/>\n", Name, Value ? "1" : "0");
      } /*SaveBool*/

    static void SaveInt
      (
        java.io.PrintStream POut,
        String Name,
        int Value,
        int Indent
      )
      {
        POut.printf(StdLocale, String.format(StdLocale, "%%%ds", Indent), " ");
        POut.printf(StdLocale, "<param name=\"%s\" value=\"%d\"/>\n", Name, Value);
      } /*SaveInt*/

    static void SaveDouble
      (
        java.io.PrintStream POut,
        String Name,
        double Value,
        int Indent
      )
      {
        POut.printf(StdLocale, String.format(StdLocale, "%%%ds", Indent), " ");
        POut.printf(StdLocale, "<param name=\"%s\" value=\"%.16e\"/>\n", Name, Value);
      } /*SaveDouble*/

    static boolean GetBool
      (
        String Value
      )
      {
        boolean Result;
        int IntValue;
        try
          {
            IntValue = Integer.parseInt(Value);
          }
        catch (NumberFormatException Bad)
          {
            throw new DataFormatException(String.format(StdLocale, "bad boolean value \"%s\"", Value));
          } /*try*/
        if (IntValue == 0)
          {
            Result = false;
          }
        else if (IntValue == 1)
          {
            Result = true;
          }
        else
          {
            throw new DataFormatException(String.format(StdLocale, "bad boolean value %d", IntValue));
          } /*if*/
        return
            Result;
      } /*GetBool*/

    static int GetInt
      (
        String Value
      )
      {
        int Result;
        try
          {
            Result = Integer.parseInt(Value);
          }
        catch (NumberFormatException Bad)
          {
            throw new DataFormatException(String.format(StdLocale, "bad integer value \"%s\"", Value));
          } /*try*/
        return
            Result;
      } /*GetInt*/

    static double GetDouble
      (
        String Value
      )
      {
        double Result;
        try
          {
            Result = Double.parseDouble(Value);
          }
        catch (NumberFormatException Bad)
          {
            throw new DataFormatException(String.format(StdLocale, "bad double value \"%s\"", Value));
          } /*try*/
        return
            Result;
      } /*GetDouble*/

    static void SaveProg
      (
        byte[] Program,
        java.io.PrintStream POut,
        int Indent
      )
      /* outputs a <prog> section to POut containing the contents of Program. Trailing
        zeroes are omitted. */
      {
        POut.print(String.format(StdLocale, String.format(StdLocale, "%%%ds<prog>\n", Indent), ""));
        int Cols = 0;
        int i = 0;
        int LastNonzero = -1;
        for (;;)
          {
            if (i == Program.length)
              {
                if (Cols != 0)
                  {
                    POut.println();
                  } /*if*/
                break;
              } /*if*/
            if (Program[i] != 0)
              {
                for (int j = LastNonzero + 1; j <= i; ++j)
                  {
                    if (Cols == 24)
                      {
                        POut.print("\n");
                        Cols = 0;
                      } /*if*/
                    if (Cols != 0)
                      {
                        POut.print(" ");
                      }
                    else
                      {
                        POut.print
                          (
                            String.format
                              (
                                StdLocale,
                                String.format(StdLocale, "%%%ds", Indent + 4),
                                ""
                              )
                          );
                      } /*if*/
                    POut.printf(StdLocale, "%02d", Program[j]);
                    ++Cols;
                  } /*for*/
                LastNonzero = i;
              } /*if*/
            ++i;
          } /*for*/
        POut.print
          (
            String.format(StdLocale, String.format(StdLocale, "%%%ds</prog>\n", Indent), "")
          );
      } /*SaveProg*/

    public static void Save
      (
        ButtonGrid Buttons, /* ignored unless AllState */
        State Calc,
        boolean Libs,
        boolean AllState,
          /* true to save entire calculator state, false to only save user-entered program */
        java.io.OutputStream RawOut
      )
      {
        try
          {
            java.util.zip.ZipOutputStream Out = new java.util.zip.ZipOutputStream(RawOut);
              {
              /* Follow ODF convention of an uncompressed "mimetype" entry at known offset
                to allow magic-number sniffing. Must be first. */
                final ZipComponentWriter MimeType = new ZipComponentWriter(Out, "mimetype", false);
                MimeType.write(CalcMimeType);
                MimeType.close();
              }
            if (AllState || Libs)
              {
              /* save library modules */
                if (Calc.Bank[0].Help != null)
                  {
                    final ZipComponentWriter LibHelp = new ZipComponentWriter(Out, "help", true);
                    LibHelp.write(Calc.Bank[0].Help);
                    LibHelp.close();
                  } /*if*/
                for (int BankNr = 1; BankNr < Calc.MaxBanks; ++BankNr)
                  {
                    if (Calc.Bank[BankNr] != null)
                      {
                        final State.ProgBank Bank = Calc.Bank[BankNr];
                        if (Bank.Card != null)
                          {
                            final ZipComponentWriter CardOut =
                                new ZipComponentWriter
                                  (
                                    Out,
                                    String.format(StdLocale, "card%02d", BankNr),
                                    true
                                  );
                            Bank.Card.compress
                              (
                                /*format =*/ android.graphics.Bitmap.CompressFormat.PNG,
                                  /* good enough, won't be large */
                                /*quality =*/ 100, /* ignored */
                                /*stream =*/ CardOut.Out
                              );
                            CardOut.close();
                          } /*if*/
                        if (Bank.Program != null)
                          {
                            final ZipComponentWriter ProgOut =
                                new ZipComponentWriter
                                  (
                                    Out,
                                    String.format(StdLocale, "prog%02d", BankNr),
                                    true
                                  );
                            java.io.PrintStream POut = new java.io.PrintStream(ProgOut.Out);
                            POut.println("<state>");
                            POut.println("    <calc>");
                            SaveProg(Bank.Program, POut, 8);
                            POut.println("    </calc>");
                            POut.println("</state>");
                            POut.flush();
                            ProgOut.close();
                          } /*if*/
                        if (Bank.Help != null)
                          {
                            final ZipComponentWriter BankHelp = new ZipComponentWriter
                              (
                                Out,
                                String.format(StdLocale, "help%02d", BankNr),
                                true
                              );
                            BankHelp.write(Bank.Help);
                            BankHelp.close();
                          } /*if*/
                      } /*if*/
                  } /*for*/
              } /*if*/
            if (AllState || !Libs)
              {
                final ZipComponentWriter StateOut = new ZipComponentWriter(Out, "prog00", true);
                java.io.PrintStream POut = new java.io.PrintStream(StateOut.Out);
                POut.println("<state>");
                if (AllState)
                  {
                    POut.println("    <buttons>");
                    SaveBool(POut, "alt", Buttons.AltState, 8);
                    SaveBool(POut, "overlay", Buttons.OverlayVisible, 8);
                    SaveInt(POut, "selected_button", Buttons.SelectedButton, 8);
                    SaveInt(POut, "digits_needed", Buttons.DigitsNeeded, 8);
                    SaveBool(POut, "accept_symbolic", Buttons.AcceptSymbolic, 8);
                    SaveBool(POut, "accept_ind", Buttons.AcceptInd, 8);
                    SaveBool(POut, "next_literal", Buttons.NextLiteral, 8);
                    SaveInt(POut, "accum_digits", Buttons.AccumDigits, 8);
                    SaveInt(POut, "first_operand", Buttons.FirstOperand, 8);
                    SaveBool(POut, "got_first_operand", Buttons.GotFirstOperand, 8);
                    SaveBool(POut, "got_first_ind", Buttons.GotFirstInd, 8);
                    SaveBool(POut, "is_symbolic", Buttons.IsSymbolic, 8);
                    SaveBool(POut, "got_ind", Buttons.GotInd, 8);
                    SaveInt(POut, "collecting_for_function", Buttons.CollectingForFunction, 8);
                    POut.println("    </buttons>");
                  } /*if*/
                POut.println("    <calc>");
                if (AllState)
                  {
                      {
                        String StateName;
                        switch (Calc.CurState)
                          {
                        case State.EntryState:
                            StateName = "entry";
                        break;
                        case State.DecimalEntryState:
                            StateName = "decimal_entry";
                        break;
                        case State.ExponentEntryState:
                            StateName = "exponent_entry";
                        break;
                        case State.ResultState:
                            StateName = "result";
                        break;
                        case State.ErrorState:
                            StateName = "error";
                        break;
                        default:
                            throw new RuntimeException
                              (
                                String.format
                                  (
                                    StdLocale,
                                    "unrecognized Calc.CurState = %d",
                                    Calc.CurState
                                  )
                              );
                      /* break; */
                          } /*switch*/
                        POut.printf
                          (
                            StdLocale,
                            "        <param name=\"state\" value=\"%s\"/>\n",
                            StateName
                          );
                      }
                    SaveBool(POut, "exponent_entered", Calc.ExponentEntered, 8);
                    if (Calc.CurState != State.ResultState && Calc.CurState != State.ErrorState)
                      {
                        POut.printf
                          (
                            StdLocale,
                            "        <param name=\"display\" value=\"%s\"/>\n",
                            Calc.CurDisplay
                          );
                      } /*if*/
                    SaveBool(POut, "inv", Calc.InvState, 8);
                      {
                        String FmtName;
                        switch (Calc.CurFormat)
                          {
                        case State.FORMAT_FIXED:
                            FmtName = "fixed";
                        break;
                        case State.FORMAT_FLOAT:
                            FmtName = "float";
                        break;
                        case State.FORMAT_ENG:
                            FmtName = "eng";
                        break;
                        default:
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized Calc.CurFormat = %d", Calc.CurFormat)
                              );
                      /* break; */
                          } /*switch*/
                        POut.printf(StdLocale, "        <param name=\"format\" value=\"%s\"/>\n", FmtName);
                      }
                    SaveInt(POut, "nr_decimals", Calc.CurNrDecimals, 8);
                      {
                        String Name;
                        switch (Calc.CurAng)
                          {
                        case State.ANG_RAD:
                            Name = "radians";
                        break;
                        case State.ANG_DEG:
                            Name = "degrees";
                        break;
                        case State.ANG_GRAD:
                            Name = "gradians";
                        break;
                        default:
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized Calc.Ang = %d", Calc.CurAng)
                              );
                      /* break; */
                          } /*switch*/
                        POut.printf(StdLocale, "        <param name=\"angle_units\" value=\"%s\"/>\n", Name);
                      }
                    POut.println("        <opstack>");
                    for (int i = 0; i < Calc.OpStackNext; ++i)
                      {
                        final State.OpStackEntry Op = Calc.OpStack[i];
                        String OpName;
                        switch (Op.Operator)
                          {
                        case State.STACKOP_ADD:
                            OpName = "add";
                        break;
                        case State.STACKOP_SUB:
                            OpName = "sub";
                        break;
                        case State.STACKOP_MUL:
                            OpName = "mul";
                        break;
                        case State.STACKOP_DIV:
                            OpName = "div";
                        break;
                        case State.STACKOP_EXP:
                            OpName = "exp";
                        break;
                        case State.STACKOP_ROOT:
                            OpName = "root";
                        break;
                        default:
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized stacked op %d at pos %d", Op.Operator, i)
                              );
                      /* break; */
                          } /*switch*/
                        POut.printf
                          (
                            StdLocale,
                            "            <op name=\"%s\" opnd=\"%.16e\" parens=\"%d\"/>\n",
                            OpName,
                            Op.Operand,
                            Op.ParenFollows
                          );
                      } /*for*/
                    POut.println("        </opstack>");
                    SaveDouble(POut, "X", Calc.X, 8);
                    SaveDouble(POut, "T", Calc.T, 8);
                    SaveBool(POut, "learn_mode", Calc.ProgMode, 8);
                    POut.println("        <mem>");
                    for (int i = 0; i < Calc.Memory.length; ++i)
                      {
                        POut.printf(StdLocale, "            %.16e\n", Calc.Memory[i]);
                      } /*for*/
                    POut.println("        </mem>");
                  } /*if AllState*/
                SaveProg(Calc.Program, POut, 8);
                if (AllState)
                  {
                    POut.print("        <flags>\n            ");
                    for (int i = 0; i < Calc.Flag.length; ++i)
                      {
                        if (i != 0)
                          {
                            POut.print(" ");
                          } /*if*/
                        POut.print(Calc.Flag[i] ? "1" : "0");
                      } /*for*/
                    POut.print("\n        </flags>\n");
                    SaveInt(POut, "PC", Calc.PC, 8);
                    SaveInt(POut, "bank", Calc.CurBank, 8);
                    POut.println("        <retstack>");
                    for (int i = 0; i <= Calc.ReturnLast; ++i)
                      {
                        final State.ReturnStackEntry Ret = Calc.ReturnStack[i];
                        POut.printf
                          (
                            "            <ret bank=\"%d\" addr=\"%d\" from_interactive=\"%s\"/>\n",
                            Ret.BankNr,
                            Ret.Addr,
                            Ret.FromInteractive ? "1" : "0"
                          );
                      } /*for*/
                    POut.println("        </retstack>");
                  } /*if*/
                POut.println("    </calc>");
                POut.println("</state>");
                POut.flush();
                StateOut.close();
              } /*if*/
            Out.finish();
          }
        catch (java.io.IOException Failed)
          {
            throw new RuntimeException("ti5x.Persistent.Save error " + Failed.toString());
          } /*try*/
      } /*Save*/

    public static void Save
      (
        ButtonGrid Buttons, /* ignored unless AllState */
        State Calc,
        boolean Libs,
        boolean AllState, /* true to save entire calculator state, false to only save program */
        String ToFile
      )
      {
        java.io.FileOutputStream Out;
        try
          {
            Out = new java.io.FileOutputStream(ToFile);
          }
        catch (java.io.FileNotFoundException Failed)
          {
            throw new RuntimeException
              (
                "ti5x.Persistent.Save create error " + Failed.toString()
              );
          } /*try*/
        Save(Buttons, Calc, Libs, AllState, Out);
        try
          {
            Out.flush();
            Out.close();
          }
        catch (java.io.IOException Failed)
          {
            throw new RuntimeException("ti5x.Persistent.Save error " + Failed.toString());
          } /*try*/
      } /*Save*/

    public static byte[] ReadAll
      (
        java.io.InputStream From
      )
      /* reads all available data from From. */
    throws java.io.IOException
      {
        java.io.ByteArrayOutputStream Result = new java.io.ByteArrayOutputStream();
        final byte[] Buf = new byte[256]; /* just to reduce number of I/O operations */
        for (;;)
          {
            final int BytesRead = From.read(Buf);
            if (BytesRead < 0)
                break;
            Result.write(Buf, 0, BytesRead);
          } /*for*/
        return
            Result.toByteArray();
      } /*ReadAll*/

    static class CalcStateLoader extends org.xml.sax.helpers.DefaultHandler
      {
        protected Display Disp;
        protected ButtonGrid Buttons;
        protected State Calc;
        protected int BankNr;
        protected boolean AllState;

        private final int AtTopLevel = 0;
        private final int DoingState = 1;
        private final int DoingButtons = 2;
        private final int DoingCalc = 3;
        private final int DoingOpStack = 10;
        private final int DoingMem = 11;
        private final int DoingProg = 12;
        private final int DoingFlags = 13;
        private final int DoingRetStack = 14;
        private int ParseState = AtTopLevel;
        private boolean DoneState = false;
        private boolean AllowContent;
        java.io.ByteArrayOutputStream Content = null;

        public CalcStateLoader
          (
            Display Disp,
            ButtonGrid Buttons,
            State Calc,
            int BankNr,
            boolean AllState
          )
          {
            super();
            this.Disp = Disp;
            this.Buttons = Buttons;
            this.Calc = Calc;
            this.BankNr = BankNr;
            this.AllState = AllState;
          } /*CalcStateLoader*/

        private void StartContent()
          {
            Content = new java.io.ByteArrayOutputStream();
            AllowContent = true;
          } /*StartContent*/

        @Override
        public void startElement
          (
            String uri,
            String localName,
            String qName,
            org.xml.sax.Attributes attributes
          )
          {
            localName = localName.intern();
            boolean Handled = false;
            AllowContent = false; /* to begin with */
            if (ParseState == AtTopLevel)
              {
                if (localName == "state" && !DoneState)
                  {
                    ParseState = DoingState;
                    Handled = true;
                  } /*if*/
              }
            else if (ParseState == DoingState)
              {
                if (AllState && localName == "buttons")
                  {
                    ParseState = DoingButtons;
                    Handled = true;
                  }
                else if (localName == "calc")
                  {
                    ParseState = DoingCalc;
                    Handled = true;
                  } /*if*/
              }
            else if (ParseState == DoingButtons)
              {
                if (localName == "param")
                  {
                    final String Name = attributes.getValue("name").intern();
                    final String Value = attributes.getValue("value");
                    if (Name == "alt")
                      {
                        Buttons.AltState = GetBool(Value);
                      }
                    else if (Name == "overlay")
                      {
                        Buttons.OverlayVisible = GetBool(Value);
                      }
                    else if (Name == "selected_button")
                      {
                        Buttons.SelectedButton = GetInt(Value);
                      }
                    else if (Name == "digits_needed")
                      {
                        Buttons.DigitsNeeded = GetInt(Value);
                      }
                    else if (Name == "accept_symbolic")
                      {
                        Buttons.AcceptSymbolic = GetBool(Value);
                      }
                    else if (Name == "accept_ind")
                      {
                        Buttons.AcceptInd = GetBool(Value);
                      }
                    else if (Name == "next_literal")
                      {
                        Buttons.NextLiteral = GetBool(Value);
                      }
                    else if (Name == "accum_digits")
                      {
                        Buttons.AccumDigits = GetInt(Value);
                      }
                    else if (Name == "first_operand")
                      {
                        Buttons.FirstOperand = GetInt(Value);
                      }
                    else if (Name == "got_first_operand")
                      {
                        Buttons.GotFirstOperand = GetBool(Value);
                      }
                    else if (Name == "got_first_ind")
                      {
                        Buttons.GotFirstInd = GetBool(Value);
                      }
                    else if (Name == "is_symbolic")
                      {
                        Buttons.IsSymbolic = GetBool(Value);
                      }
                    else if (Name == "got_ind")
                      {
                        Buttons.GotInd = GetBool(Value);
                      }
                    else if (Name == "collecting_for_function")
                      {
                        Buttons.CollectingForFunction = GetInt(Value);
                      }
                    else
                      {
                        throw new DataFormatException
                          (
                            String.format(StdLocale, "unrecognized <buttons> param \"%s\"", Name)
                          );
                      } /*if*/
                    Handled = true;
                  } /*if*/
              }
            else if (ParseState == DoingCalc)
              {
                if (AllState && localName == "param")
                  {
                    final String Name = attributes.getValue("name").intern();
                    final String Value = attributes.getValue("value").intern();
                    if (Name == "state")
                      {
                        if (Value == "entry")
                          {
                            Calc.CurState = State.EntryState;
                          }
                        else if (Value == "decimal_entry")
                          {
                            Calc.CurState = State.DecimalEntryState;
                          }
                        else if (Value == "exponent_entry")
                          {
                            Calc.CurState = State.ExponentEntryState;
                          }
                        else if (Value == "result")
                          {
                            Calc.CurState = State.ResultState;
                          }
                        else if (Value == "error")
                          {
                            Calc.CurState = State.ErrorState;
                          }
                        else
                          {
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized calc state \"%s\"", Value)
                              );
                          } /*if*/
                      }
                    else if (Name == "exponent_entered")
                      {
                        Calc.ExponentEntered = GetBool(Value);
                      }
                    else if (Name == "display")
                      {
                        Calc.CurDisplay = Value;
                      }
                    else if (Name == "inv")
                      {
                        Calc.InvState = GetBool(Value);
                      }
                    else if (Name == "format")
                      {
                        if (Value == "fixed")
                          {
                            Calc.CurFormat = State.FORMAT_FIXED;
                          }
                        else if (Value == "float")
                          {
                            Calc.CurFormat = State.FORMAT_FLOAT;
                          }
                        else if (Value == "eng")
                          {
                            Calc.CurFormat = State.FORMAT_ENG;
                          }
                        else
                          {
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized calc format \"%s\"", Value)
                              );
                          } /*if*/
                      }
                    else if (Name == "nr_decimals")
                      {
                        Calc.CurNrDecimals = GetInt(Value);
                      }
                    else if (Name == "angle_units")
                      {
                        if (Value == "radians")
                          {
                            Calc.CurAng = State.ANG_RAD;
                          }
                        else if (Value == "degrees")
                          {
                            Calc.CurAng = State.ANG_DEG;
                          }
                        else if (Value == "gradians")
                          {
                            Calc.CurAng = State.ANG_GRAD;
                          }
                        else
                          {
                            throw new RuntimeException
                              (
                                String.format(StdLocale, "unrecognized calc angle_units \"%s\"", Value)
                              );
                          } /*if*/
                      }
                    else if (Name == "X")
                      {
                        Calc.X = GetDouble(Value);
                      }
                    else if (Name == "T")
                      {
                        Calc.T = GetDouble(Value);
                      }
                    else if (Name == "learn_mode")
                      {
                        Calc.ProgMode = GetBool(Value);
                      }
                    else if (Name == "PC")
                      {
                        Calc.PC = GetInt(Value);
                      }
                    else if (Name == "bank")
                      {
                        Calc.CurBank = GetInt(Value);
                      }
                    else
                      {
                        throw new DataFormatException
                          (
                            String.format(StdLocale, "unrecognized <calc> param \"%s\"", Name)
                          );
                      } /*if*/
                    Handled = true;
                  }
                else if (AllState && localName == "opstack")
                  {
                    Calc.OpStackNext = 0;
                    ParseState = DoingOpStack;
                    Handled = true;
                  }
                else if (AllState && localName == "mem")
                  {
                    ParseState = DoingMem;
                    StartContent();
                    Handled = true;
                  }
                else if (localName == "prog") /* only one allowed if not AllState */
                  {
                    ParseState = DoingProg;
                    StartContent();
                    Handled = true;
                  }
                else if (AllState && localName == "flags")
                  {
                    ParseState = DoingFlags;
                    StartContent();
                    Handled = true;
                  }
                else if (AllState && localName == "retstack")
                  {
                    Calc.ReturnLast = -1;
                    ParseState = DoingRetStack;
                    Handled = true;
                  } /*if*/
              }
            else if (ParseState == DoingOpStack)
              {
                if (localName == "op")
                  {
                    final String OpName = attributes.getValue("name").intern();
                    int Op;
                    if (OpName == "add")
                      {
                        Op = State.STACKOP_ADD;
                      }
                    else if (OpName == "sub")
                      {
                        Op = State.STACKOP_SUB;
                      }
                    else if (OpName == "mul")
                      {
                        Op = State.STACKOP_MUL;
                      }
                    else if (OpName == "div")
                      {
                        Op = State.STACKOP_DIV;
                      }
                    else if (OpName == "exp")
                      {
                        Op = State.STACKOP_EXP;
                      }
                    else if (OpName == "root")
                      {
                        Op = State.STACKOP_ROOT;
                      }
                    else
                      {
                        throw new DataFormatException
                          (
                            String.format(StdLocale, "unrecognized <op> operator \"%s\"", OpName)
                          );
                      } /*if*/
                    if (Calc.OpStackNext == Calc.MaxOpStack)
                      {
                        throw new DataFormatException
                          (
                            String.format(StdLocale, "too many <op> entries -- only %d allowed", Calc.MaxOpStack)
                          );
                      } /*if*/
                    Calc.OpStack[Calc.OpStackNext++] = new State.OpStackEntry
                      (
                        GetDouble(attributes.getValue("opnd")),
                        Op,
                        GetInt(attributes.getValue("parens"))
                      );
                    Handled = true;
                  } /*if*/
              }
            else if (ParseState == DoingRetStack)
              {
                if (localName == "ret")
                  {
                    if (Calc.ReturnLast == Calc.MaxReturnStack - 1)
                      {
                        throw new DataFormatException
                          (
                            String.format(StdLocale, "too many <ret> entries -- only %d allowed", Calc.MaxReturnStack)
                          );
                      } /*if*/
                    Calc.ReturnStack[++Calc.ReturnLast] = new State.ReturnStackEntry
                      (
                        GetInt(attributes.getValue("bank")),
                        GetInt(attributes.getValue("addr")),
                        GetBool(attributes.getValue("from_interactive"))
                      );
                    Handled = true;
                  } /*if*/
              } /*if*/
            if (!Handled)
              {
                throw new DataFormatException("unexpected XML tag " + localName + " in state " + ParseState);
              } /*if*/
          } /*startElement*/

        @Override
        public void characters
          (
            char[] ch,
            int start,
            int length
          )
          {
            if (AllowContent)
              {
                try
                  {
                    Content.write(new String(ch, start, length).getBytes());
                  }
                catch (java.io.IOException Failed)
                  {
                    throw new RuntimeException("ti5x XML content parse error " + Failed.toString());
                  } /*try*/
              } /*if*/
            /* else ignore */
          } /*characters*/

        @Override
        public void endElement
          (
            String uri,
            String localName,
            String qName
          )
          {
            localName = localName.intern();
            final String ContentStr = Content != null ? Content.toString() : null;
            Content = null;
            AllowContent = false;
            switch (ParseState)
              {
            case DoingState:
                if (localName == "state")
                  {
                    ParseState = AtTopLevel;
                    if (Calc != null)
                      {
                        switch (Calc.CurState)
                          {
                        case State.ResultState:
                            Calc.SetX(Calc.X);
                        break;
                        case State.ErrorState:
                            Disp.SetShowingError();
                        break;
                        default: /* assume in the middle of number entry */
                            Calc.SetProgMode(Calc.ProgMode);
                        break;
                          } /*switch*/
                      } /*if*/
                    if (Buttons != null)
                      {
                        Buttons.invalidate();
                      } /*if*/
                  } /*if*/
            break;
            case DoingButtons:
                if (localName == "buttons")
                  {
                    ParseState = DoingState;
                  } /*if*/
            case DoingCalc:
                if (localName == "calc")
                  {
                    ParseState = DoingState;
                  } /*if*/
            break;
            case DoingOpStack:
                if (localName == "opstack")
                  {
                    ParseState = DoingCalc;
                  } /*if*/
            break;
            case DoingRetStack:
                if (localName == "retstack")
                  {
                    ParseState = DoingCalc;
                  } /*if*/
            break;
            case DoingMem:
                if (localName == "mem")
                  {
                    int Reg = 0;
                    int i = 0;
                    for (;;)
                      {
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) > ' ')
                                break;
                            ++i;
                          } /*for*/
                        final int Start = i;
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) <= ' ')
                                break;
                            ++i;
                          } /*for*/
                        if (i > Start)
                          {
                            if (Reg == Calc.MaxMemories)
                              {
                                throw new DataFormatException
                                  (
                                    String.format(StdLocale, "too many memories, only %d allowed", Calc.MaxMemories)
                                  );
                              } /*if*/
                            Calc.Memory[Reg++] = GetDouble(ContentStr.substring(Start, i));
                          } /*if*/
                        if (i == ContentStr.length())
                            break;
                      } /*for*/
                    ParseState = DoingCalc;
                  } /*if*/
            break;
            case DoingProg:
                if (localName == "prog")
                  {
                    java.util.ArrayList<Byte> Prog = null;
                    if (BankNr == 0)
                      {
                        for (int i = 0; i < Calc.MaxProgram; ++i)
                          {
                            Calc.Program[i] = (byte)0;
                          } /*for*/
                      }
                    else
                      {
                        Prog = new java.util.ArrayList<Byte>();
                          /* can be any length, I suppose I should really restrict it to 1000 steps */
                      } /*if*/
                    int Addr = 0;
                    int i = 0;
                    for (;;)
                      {
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) > ' ')
                                break;
                            ++i;
                          } /*for*/
                        final int Start = i;
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) <= ' ')
                                break;
                            ++i;
                          } /*for*/
                        if (i > Start)
                          {
                            if (BankNr == 0 ? Addr == Calc.MaxProgram : Addr == 1000)
                              {
                                throw new DataFormatException
                                  (
                                    String.format
                                      (
                                        StdLocale,
                                        "too many program steps, only %d allowed",
                                        BankNr == 0 ? Calc.MaxProgram : 1000
                                      )
                                  );
                              } /*if*/
                            final byte val = (byte)GetInt(ContentStr.substring(Start, i));
                            if (BankNr != 0)
                              {
                                Prog.add(val);
                              }
                            else
                              {
                                Calc.Program[Addr++] = val;
                              } /*if*/
                          } /*if*/
                        if (i == ContentStr.length())
                            break;
                      } /*for*/
                    if (BankNr != 0)
                      {
                        Calc.Bank[BankNr].Program = new byte[Prog.size()];
                        for (i = 0; i < Prog.size(); ++i)
                          {
                            Calc.Bank[BankNr].Program[i] = Prog.get(i);
                          } /*for*/
                      } /*if*/
                    ParseState = DoingCalc;
                  } /*if*/
            break;
            case DoingFlags:
                if (localName == "flags")
                  {
                    int Flag = 0;
                    int i = 0;
                    for (;;)
                      {
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) > ' ')
                                break;
                            ++i;
                          } /*for*/
                        final int Start = i;
                        for (;;)
                          {
                            if (i == ContentStr.length())
                                break;
                            if (ContentStr.charAt(i) <= ' ')
                                break;
                            ++i;
                          } /*for*/
                        if (i > Start)
                          {
                            if (Flag == Calc.MaxFlags)
                              {
                                throw new DataFormatException
                                  (
                                    String.format(StdLocale, "too many flags, only %d allowed", Calc.MaxFlags)
                                  );
                              } /*if*/
                            Calc.Flag[Flag++] = GetBool(ContentStr.substring(Start, i));
                          } /*if*/
                        if (i == ContentStr.length())
                            break;
                      } /*for*/
                    ParseState = DoingCalc;
                  } /*if*/
            break;
              } /*switch*/
          } /*endElement*/

      } /*CalcStateLoader*/

    public static void Load
      (
        String FromFile,
        boolean Libs, /* true to load nonzero program banks, false to load bank 0 */
        boolean AllState, /* true to load all state (including all available program banks) */
        Display Disp,
        HelpCard Help,
        ButtonGrid Buttons,
        State Calc
      )
    throws DataFormatException
      {
        try
          {
            final java.util.zip.ZipFile In = new java.util.zip.ZipFile
              (
                new java.io.File(FromFile),
                java.util.zip.ZipFile.OPEN_READ
              );
            final ZipEntry MimeType = In.getEntry("mimetype");
            if (MimeType == null)
              {
                throw new DataFormatException
                  (
                    "missing mandatory archive component: mimetype"
                  );
              } /*if*/
            if
              (
                    In.entries().nextElement().getName().intern() != "mimetype"
                ||
                    MimeType.getMethod() != ZipEntry.STORED
              )
              {
                throw new DataFormatException("mimetype must be uncompressed and first in archive");
              } /*if*/
            if (new String(ReadAll(In.getInputStream(MimeType))).intern() != CalcMimeType)
              {
                throw new DataFormatException("wrong MIME type");
              } /*if*/
            if (!Libs || AllState)
              {
                Buttons.Reset();
                Calc.Reset(AllState);
              }
            else
              {
                Calc.ResetLabels(); /* at least */
              } /*if*/
            if (Libs || AllState)
              {
                final ZipEntry LibHelpEntry = In.getEntry("help");
                if (LibHelpEntry != null)
                  {
                    Calc.Bank[0].Help = ReadAll(In.getInputStream(LibHelpEntry));
                  } /*if*/
              } /*if*/
            for (int BankNr = 0;;)
              {
                if (BankNr == Calc.MaxBanks)
                    break;
                if (AllState || Libs == (BankNr != 0))
                  {
                    final ZipEntry CardEntry =
                        BankNr != 0 ?
                            In.getEntry(String.format(StdLocale, "card%02d", BankNr))
                        :
                            null;
                    final ZipEntry StateEntry =
                        In.getEntry(String.format(StdLocale, "prog%02d", BankNr));
                    final ZipEntry HelpEntry =
                        In.getEntry
                          (
                            BankNr != 0 ? String.format(StdLocale, "help%02d", BankNr) : "help"
                          );
                    if (StateEntry != null)
                      {
                        android.graphics.Bitmap CardImage = null;
                        byte[] BankHelp = null;
                        if (CardEntry != null)
                          {
                            CardImage = android.graphics.BitmapFactory.decodeStream
                              (
                                In.getInputStream(CardEntry)
                              );
                          } /*if*/
                        if (HelpEntry != null)
                          {
                            BankHelp = ReadAll(In.getInputStream(HelpEntry));
                          } /*if*/
                        if (BankNr != 0)
                          {
                            Calc.Bank[BankNr] =
                                new State.ProgBank(null /* filled in below */, CardImage, BankHelp);
                          }
                        else
                          {
                            /* ignore any CardImage */
                            Calc.Bank[0].Help = BankHelp;
                          } /*if*/
                        try
                          {
                            javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().parse
                              (
                                In.getInputStream(StateEntry),
                                new CalcStateLoader(Disp, Buttons, Calc, BankNr, AllState)
                              );
                          }
                        catch (javax.xml.parsers.ParserConfigurationException Bug)
                          {
                            throw new RuntimeException("SAX parser error: " + Bug.toString());
                          }
                        catch (org.xml.sax.SAXException Bad)
                          {
                            throw new DataFormatException("SAX parser error: " + Bad.toString());
                          } /*try*/
                      }
                    else if (BankNr == 0)
                      {
                        throw new DataFormatException
                          (
                            "missing mandatory archive component: prog00"
                          );
                      } /*if*/
                  } /*if*/
                if (!Libs && !AllState)
                    break;
                ++BankNr;
              } /*for*/
          }
        catch (java.io.IOException IOError)
          {
            throw new DataFormatException("I/O error: " + IOError.toString());
          } /*try*/
      } /*Load*/

    public static void SaveState
      (
        android.content.Context ctx,
        ButtonGrid Buttons,
        State Calc
      )
      /* saves the entire current calculator state for later restoration. */
      {
        ctx.deleteFile(SavedStateName); /* if it exists */
        java.io.FileOutputStream CurSave;
        try
          {
            CurSave = ctx.openFileOutput(SavedStateName, ctx.MODE_WORLD_READABLE);
          }
        catch (java.io.FileNotFoundException Eh)
          {
            throw new RuntimeException("ti5x save-state create error " + Eh.toString());
          } /*try*/
        Save(Buttons, Calc, true, true, CurSave); /* catch RuntimeException? */
        try
          {
            CurSave.flush();
            CurSave.close();
          }
        catch (java.io.IOException Failed)
          {
            throw new RuntimeException
              (
                "ti5x state save error " + Failed.toString()
              );
          } /*try*/
      } /*SaveState*/

    public static void RestoreState
      (
        android.content.Context ctx,
        Display Disp,
        HelpCard Help,
        ButtonGrid Buttons,
        State Calc
      )
      /* restores the entire calculator state, using the previously-saved
        state if available, otherwise (re)initializes to default state. */
      {
        boolean RestoredState = false;
          {
            final String StateFile = ctx.getFilesDir().getAbsolutePath() + "/" + SavedStateName;
            if (new java.io.File(StateFile).exists())
              {
                try
                  {
                    Load
                      (
                        /*FromFile =*/ StateFile,
                        /*Libs =*/ true,
                        /*AllState =*/ true,
                        /*Disp =*/ Disp,
                        /*Help =*/ Help,
                        /*Buttons =*/ Buttons,
                        /*Calc =*/ Calc
                      );
                    RestoredState = true;
                  }
                catch (DataFormatException Bad)
                  {
                    System.err.printf
                      (
                        "ti5x failure to reload state from file \"%s\": %s\n",
                        StateFile,
                        Bad.toString()
                      ); /* debug  */
                  } /*try*/
              } /*if*/
          }
        if (!RestoredState)
          {
          /* initialize state to include Master Library */
          /* unfortunately java.util.zip.ZipFile can't read from an arbitrary InputStream,
            so I need to make a temporary copy of the master library out of my raw resources. */
            final String TempLibName = "temp.ti5x";
            final String TempLibFile = ctx.getFilesDir().getAbsolutePath() + "/" + TempLibName;
            try
              {
                final java.io.InputStream LibFile = ctx.getResources().openRawResource(R.raw.ml);
                final java.io.OutputStream TempLib =
                    ctx.openFileOutput(TempLibName, ctx.MODE_WORLD_READABLE);
                  {
                    byte[] Buffer = new byte[2048]; /* some convenient size */
                    for (;;)
                      {
                        final int NrBytes = LibFile.read(Buffer);
                        if (NrBytes <= 0)
                            break;
                        TempLib.write(Buffer, 0, NrBytes);
                      } /*for*/
                  }
                TempLib.flush();
                TempLib.close();
              }
            catch (java.io.FileNotFoundException Failed)
              {
                throw new RuntimeException("ti5x Master Library load failed: " + Failed.toString());
              }
            catch (java.io.IOException Failed)
              {
                throw new RuntimeException("ti5x Master Library load failed: " + Failed.toString());
              } /*try*/
            Load
              (
                /*FromFile =*/ TempLibFile,
                /*Libs =*/ true,
                /*AllState =*/ false,
                /*Disp =*/ Disp,
                /*Help =*/ Help,
                /*Buttons =*/ Buttons,
                /*Calc =*/ Calc
              );
            ctx.deleteFile(TempLibName);
          } /*if*/
      } /*RestoreState*/

  } /*Persistent*/
