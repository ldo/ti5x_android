package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- data exporter context

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

public class Exporter
  {
    final android.content.Context ctx;
    java.io.OutputStream Out;
    java.io.PrintStream PrintOut;
    public boolean NumbersOnly = true; /* actually initial value irrelevant */

    public Exporter
      (
        android.content.Context ctx
      )
      {
        this.ctx = ctx;
        Out = null;
        PrintOut = null;
      } /*Exporter*/

    public boolean IsOpen()
      {
        return
            Out != null;
      } /*IsOpen*/

    public void Open
      (
        String FileName,
        boolean Append,
        boolean NumbersOnly
      )
    throws RuntimeException
      {
        try
          {
            Out = new java.io.FileOutputStream(FileName, Append);
            this.NumbersOnly = NumbersOnly;
            if (this.NumbersOnly)
              {
                PrintOut = new java.io.PrintStream(Out);
              } /*if*/
          }
        catch (java.io.FileNotFoundException DirErr)
          {
            throw new RuntimeException(DirErr.toString());
          }
        catch (SecurityException PermErr)
          {
            throw new RuntimeException(PermErr.toString());
          } /*try*/
      } /*Open*/

    public void Flush()
      {
        if (Out != null)
          {
            try
              {
                if (PrintOut != null)
                  {
                    PrintOut.flush();
                  } /*if*/
                Out.flush();
              }
            catch (java.io.IOException WriteErr)
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ ctx,
                    /*text =*/
                        String.format
                          (
                            Global.StdLocale,
                            ctx.getString(R.string.export_error),
                            WriteErr.toString()
                          ),
                    /*duration =*/ android.widget.Toast.LENGTH_LONG
                  ).show();
                PrintOut = null;
                Out = null;
              } /*try*/
          } /*if*/
      } /*Flush*/

    public void Close()
      {
        if (Out != null)
          {
            try
              {
                if (PrintOut != null)
                  {
                    PrintOut.flush();
                    PrintOut.close();
                  } /*if*/
                Out.flush();
                Out.close();
              }
            catch (java.io.IOException WriteErr)
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ ctx,
                    /*text =*/
                        String.format
                          (
                            Global.StdLocale,
                            ctx.getString(R.string.export_error),
                            WriteErr.toString()
                          ),
                    /*duration =*/ android.widget.Toast.LENGTH_LONG
                  ).show();
              } /*try*/
            PrintOut = null;
            Out = null;
          } /*if*/
      } /*Close*/

    public void WriteLine
      (
        String Line
      )
      /* writes another line to the export data file. */
      {
        if (Out != null)
          {
            try
              {
                Out.write(Line.getBytes());
                Out.write("\n".getBytes());
              }
            catch (java.io.IOException WriteErr)
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ ctx,
                    /*text =*/
                        String.format
                          (
                            Global.StdLocale,
                            ctx.getString(R.string.export_error),
                            WriteErr.toString()
                          ),
                    /*duration =*/ android.widget.Toast.LENGTH_LONG
                  ).show();
                Out = null;
              } /*try*/
          } /*if*/
      } /*Write*/

    public void WriteNum
      (
        double Num
      )
      /* writes a number to the export data file in a standard form that can
        be read in again by myself or other programs. */
      {
        if (PrintOut != null)
          {
            PrintOut.printf
              (
                Global.StdLocale,
                String.format(Global.StdLocale, "%%.%de\n", Global.NrSigFigures),
                Num
              );
          } /*if*/
      } /*WriteNum*/

  } /*Exporter*/
