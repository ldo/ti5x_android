package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  /* ti5x calculator emulator -- mainline */
  {
    ButtonGrid Buttons;
    protected android.view.MenuItem ToggleOverlayItem;

    @Override
    public boolean onCreateOptionsMenu
      (
        android.view.Menu TheMenu
      )
      {
        ToggleOverlayItem = TheMenu.add(R.string.show_overlay);
        ToggleOverlayItem.setCheckable(true);
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
        Buttons = (ButtonGrid)findViewById(R.id.buttons);
        Buttons.CalcState = new State((Display)findViewById(R.id.display));
      /* more TBD */
      } /*onCreate*/

  } /*Main*/
