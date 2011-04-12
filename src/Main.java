package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  /* ti5x calculator emulator -- mainline */
  {
    Display TheDisplay;
    ButtonGrid Buttons;
    State CalcState;
    protected android.view.MenuItem ToggleOverlayItem;
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
        Persistent.Save(Buttons, CalcState, true, CurSave); /* catch RuntimeException? */
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
        TheDisplay = (Display)findViewById(R.id.display);
        Buttons = (ButtonGrid)findViewById(R.id.buttons);
        CalcState = new State(TheDisplay);
        Buttons.CalcState = CalcState;
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
        try
          {
            Persistent.Load
              (
                /*FromFile =*/ getFilesDir().getAbsolutePath() + "/" + SavedStateName,
                /*ProgNr =*/ 0,
                /*TheDisplay =*/ TheDisplay,
                /*Buttons =*/ Buttons,
                /*Calc =*/ CalcState
              );
          }
        catch (Persistent.DataFormatException Bad)
          {
            System.err.printf("ti5x failure to reload state from file \"%s\": %s\n", getFilesDir().getAbsolutePath() + "/" + SavedStateName, Bad.toString()); /* debug  */
          } /*try*/
      } /*onResume*/

  } /*Main*/
