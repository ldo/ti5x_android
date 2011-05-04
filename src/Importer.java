package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- data importer context

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

public class Importer
  {

    class ImportDataFeeder extends State.ImportFeeder
      {
        java.io.InputStream Data;
        int LineNr, ColNr; /* for reporting locations of errors */
        boolean WasNL, EOF;

        public ImportDataFeeder
          (
            java.io.InputStream Data
          )
          {
            this.Data = Data;
            LineNr = 0;
            ColNr = 0;
            WasNL = true; /* so LineNr gets incremented to 1 on first character */
            EOF = false;
          } /*ImportDataFeeder*/

        @Override
        public double Next()
            throws State.ImportEOFException
          {
            double Result = 0.0;
            StringBuilder LastNum = null;
            for (;;)
              {
                int ch;
                if (EOF)
                  {
                    ch = '\n';
                  }
                else
                  {
                    try
                      {
                        ch = Data.read();
                      }
                    catch (java.io.IOException Failed)
                      {
                        throw new Persistent.DataFormatException
                          (
                            String.format
                              (
                                Global.StdLocale,
                                "Couldn’t read input file at line %d, col %d: %s",
                                LineNr,
                                ColNr,
                                Failed.toString()
                              )
                          );
                       } /*try*/
                    if (ch < 0)
                      {
                        EOF = true;
                        ch = '\n';
                      } /*if*/
                    if (WasNL)
                      {
                        ++LineNr;
                        ColNr = 0;
                      } /*if*/
                    ++ColNr;
                  } /*if*/
                if (ch == ' ' || ch == '\t' || ch == '\n' || ch == ',' || ch == ';')
                  {
                    if (LastNum != null)
                      {
                        try
                          {
                            Result = Double.parseDouble(LastNum.toString());
                          }
                        catch (NumberFormatException Bad)
                          {
                            throw new Persistent.DataFormatException
                              (
                                String.format
                                  (
                                    Global.StdLocale,
                                    "Bad number \"%s\" on line %d, col %d",
                                    LastNum.toString(),
                                    LineNr,
                                    ColNr
                                  )
                              );
                          } /*try*/
                        break;
                      }
                    else if (EOF)
                      {
                        if (Data != null)
                          {
                            try
                              {
                                Data.close();
                              }
                            catch (java.io.IOException WhoCares)
                              {
                              /* I mean, really? */
                              } /*try*/
                            Data = null;
                          } /*if*/
                        throw new State.ImportEOFException("no more numbers");
                      } /*if*/
                  }
                else if
                  (
                        ch >= '0' && ch <= '9'
                    ||
                        ch == '.'
                    ||
                        ch == '-'
                    ||
                        ch == '+'
                    ||
                        ch == 'e'
                    ||
                        ch == 'E'
                  )
                  {
                    if (LastNum == null)
                      {
                        LastNum = new StringBuilder();
                      } /*if*/
                    LastNum.appendCodePoint(ch);
                  }
                else
                  {
                    throw new Persistent.DataFormatException
                      (
                        String.format
                          (
                            Global.StdLocale,
                            "Bad character \"%s\" at line %d, col %d",
                            new String(new byte[] {(byte)ch}),
                            LineNr,
                            ColNr
                          )
                      );
                  } /*if*/
                WasNL = ch == '\n';
              } /*for*/
            return
                Result;
          } /*Next*/

      } /*ImportFeeder*/

    public void ImportData
      (
        String FileName
      )
    throws Persistent.DataFormatException
      /* starts importing numbers from the specified file. */
      {
        java.io.FileInputStream Data;
        try
          {
            Data = new java.io.FileInputStream(FileName);
          }
        catch (java.io.FileNotFoundException NotFound)
          {
            throw new Persistent.DataFormatException
              (
                String.format
                  (
                    Global.StdLocale,
                    "Couldn’t open input file \"%s\": %s",
                    FileName,
                    NotFound.toString()
                  )
              );
          } /*try*/
        Global.Calc.SetImport(new ImportDataFeeder(Data));
      } /*ImportData*/

  } /*Importer*/
