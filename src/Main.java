package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- mainline

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

public class Main extends android.app.Activity
  {
    Display Disp;
    HelpCard Help;
    ButtonGrid Buttons;
    State Calc;
    protected android.view.MenuItem ToggleOverlayItem;
    protected android.view.MenuItem ShowHelpItem;
    protected android.view.MenuItem SaveProgramItem;
    protected android.view.MenuItem PowerOffItem;
    protected final int SaveProgramRequest = 2; /* arbitrary code */
    Boolean ShuttingDown = false;

    final String SavedStateName = "state" + Persistent.CalcExt;

    void SaveState()
      {
        deleteFile(SavedStateName); /* if it exists */
        java.io.FileOutputStream CurSave;
        try
          {
            CurSave = openFileOutput(SavedStateName, MODE_WORLD_READABLE);
          }
        catch (java.io.FileNotFoundException Eh)
          {
            throw new RuntimeException("ti5x save-state create error " + Eh.toString());
          } /*try*/
        Persistent.Save(Buttons, Calc, true, true, CurSave); /* catch RuntimeException? */
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

    @Override
    public boolean onCreateOptionsMenu
      (
        android.view.Menu TheMenu
      )
      {
        ToggleOverlayItem = TheMenu.add(R.string.show_overlay);
        ToggleOverlayItem.setCheckable(true);
        ShowHelpItem = TheMenu.add(R.string.show_help);
        SaveProgramItem = TheMenu.add(getString(R.string.save_as));
        PowerOffItem = TheMenu.add(R.string.turn_off);
        return
            true;
      } /*onCreateOptionsMenu*/

    @Override
    public boolean onOptionsItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        if (TheItem == ToggleOverlayItem)
          {
            Buttons.OverlayVisible = !Buttons.OverlayVisible;
            Buttons.invalidate();
            ToggleOverlayItem.setChecked(Buttons.OverlayVisible);
          }
        else if (TheItem == ShowHelpItem)
          {
            startActivity
              (
                new android.content.Intent(android.content.Intent.ACTION_VIEW)
                    .setClass(this, Help.class)
              );
          }
        else if (TheItem == SaveProgramItem)
          {
            startActivityForResult
              (
                new android.content.Intent(android.content.Intent.ACTION_PICK)
                    .setClass(this, SaveAs.class),
                SaveProgramRequest
              );
          }
        else if (TheItem == PowerOffItem)
          {
            ShuttingDown = true;
            deleteFile(SavedStateName); /* lose any saved state */
            finish(); /* start afresh next time */
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onOptionsItemSelected*/

    @Override
    public void onActivityResult
      (
        int RequestCode,
        int ResultCode,
        android.content.Intent Data
      )
      {
        if
          (
                RequestCode == SaveProgramRequest
            &&
                Data != null
          )
          {
            final String TheName =
                    Data.getData().getPath().substring(1) /* ignoring leading slash */
                +
                    Persistent.CalcExt;
            try
              {
                final String SaveDir =
                        android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    +
                        "/"
                    +
                        Persistent.ProgramsDir;
                new java.io.File(SaveDir).mkdirs();
                Persistent.Save
                  (
                    /*Buttons =*/ Buttons,
                    /*Calc =*/ Calc,
                    /*Libs =*/ false,
                    /*AllState =*/ false,
                    /*ToFile =*/ SaveDir + "/" + TheName
                  );
                android.widget.Toast.makeText
                  (
                    /*context =*/ this,
                    /*text =*/ String.format(getString(R.string.program_saved), TheName),
                    /*duration =*/ android.widget.Toast.LENGTH_SHORT
                  ).show();
              }
            catch (RuntimeException Failed)
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ this,
                    /*text =*/
                        String.format(getString(R.string.program_save_error), Failed.toString()),
                    /*duration =*/ android.widget.Toast.LENGTH_LONG
                  ).show();
              } /*try*/
          } /*if*/
      } /*onActivityResult*/

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Disp = (Display)findViewById(R.id.display);
        Help = (HelpCard)findViewById(R.id.help_card);
        Buttons = (ButtonGrid)findViewById(R.id.buttons);
        Calc = new State(Disp, Help);
        Buttons.Calc = Calc;
      } /*onCreate*/

    @Override
    public void onPause()
      {
        super.onPause();
        if (!ShuttingDown)
          {
            SaveState();
          } /*if*/
      } /*onPause*/

    @Override
    public void onResume()
      {
        super.onResume();
        boolean RestoredState = false;
          {
            final String StateFile = getFilesDir().getAbsolutePath() + "/" + SavedStateName;
            if (new java.io.File(StateFile).exists())
              {
                try
                  {
                    Persistent.Load
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
                catch (Persistent.DataFormatException Bad)
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
            final String TempLibFile = getFilesDir().getAbsolutePath() + "/" + TempLibName;
            try
              {
                final java.io.InputStream LibFile = getResources().openRawResource(R.raw.ml);
                final java.io.OutputStream TempLib =
                    openFileOutput(TempLibName, MODE_WORLD_READABLE);
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
            Persistent.Load
              (
                /*FromFile =*/ TempLibFile,
                /*Libs =*/ true,
                /*AllState =*/ false,
                /*Disp =*/ Disp,
                /*Help =*/ Help,
                /*Buttons =*/ Buttons,
                /*Calc =*/ Calc
              );
            deleteFile(TempLibName);
          } /*if*/
      } /*onResume*/

  } /*Main*/
