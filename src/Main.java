package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  {

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final ButtonGrid Buttons = (ButtonGrid)findViewById(R.id.buttons);
        Buttons.CalcState = new State((Display)findViewById(R.id.display));
      /* more TBD */
      } /*onCreate*/

  } /*Main*/
