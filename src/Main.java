package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  /* ti5x calculator emulator -- mainline */
  {
    Display Disp;
    HelpCard Help;
    ButtonGrid Buttons;
    State Calc;
    protected android.view.MenuItem ToggleOverlayItem;
    protected android.view.MenuItem ShowHelpItem;
    protected android.view.MenuItem PowerOffItem;
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
