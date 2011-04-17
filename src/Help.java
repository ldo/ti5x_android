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
        final android.content.Intent MyIntent = getIntent();
        final byte[] Content = MyIntent.getByteArrayExtra("content");
        if (Content != null)
          {
          /* show the specified module help */
            HelpView.loadDataWithBaseURL
              (
                /*baseUrl =*/ null,
                /*data =*/ new String(Content),
                /*mimeType =*/ null, /* text/html */
                /*encoding =*/ "utf-8",
                /*history =*/ null
              );
          }
        else
          {
          /* show calculator help */
            HelpView.loadUrl("file:///android_asset/help/index.html");
          } /*if*/
      } /*onCreate*/

  } /*Help*/

