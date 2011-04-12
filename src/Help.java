package nz.gen.geek_central.ti5x;

public class Help extends android.app.Activity
  /* ti5x calculator emulator -- online help */
  {
    android.webkit.WebView HelpView;

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        HelpView = (android.webkit.WebView)findViewById(R.id.help_view);
        HelpView.loadUrl("file:///android_asset/help/index.html");
      } /*onCreate*/

  } /*Help*/

