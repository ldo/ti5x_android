package nz.gen.geek_central.ti5x;
/*
    Let the user enter a name for saving a new program file.
*/

public class SaveAs extends android.app.Activity
  {
    protected android.widget.EditText SaveAsText;

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        if
          (
                android.os.Environment.getExternalStorageState().intern()
            ==
                android.os.Environment.MEDIA_MOUNTED
          )
          {
            final android.view.ViewGroup TheViewGroup =
                (android.view.ViewGroup)getLayoutInflater().inflate(R.layout.save_as, null);
            setContentView(TheViewGroup);
            SaveAsText = (android.widget.EditText)findViewById(R.id.save_as_text);
            findViewById(R.id.save_as_confirm).setOnClickListener
              (
                new android.view.View.OnClickListener()
                  {
                    public void onClick
                      (
                        android.view.View TheView
                      )
                      {
                        final String TheOrigText =
                            ((android.widget.TextView)SaveAsText).getText().toString();
                      /* I can't seem to easily set a key listener to filter keystrokes
                        as they are entered into SaveAsText. So I filter illegal characters
                        here instead. */
                        StringBuilder Clean = new StringBuilder();
                        for (int i = 0; i < TheOrigText.length(); ++i)
                          {
                            char c = TheOrigText.charAt(i);
                            if (c != '/' && (c != '.' || i != 0))
                              {
                                Clean.append(c);
                              } /*if*/
                          } /*for*/
                        final String TheCleanedText = Clean.toString();
                        if (TheCleanedText.equals(TheOrigText))
                          {
                            if (TheCleanedText.length() != 0)
                              {
                                setResult
                                  (
                                    android.app.Activity.RESULT_OK,
                                    new android.content.Intent().setData
                                      (
                                        android.net.Uri.fromFile(new java.io.File(TheCleanedText))
                                      )
                                  );
                                finish();
                              }
                            else
                              {
                                android.widget.Toast.makeText
                                  (
                                    /*context =*/ SaveAs.this,
                                    /*text =*/ getString(R.string.please_enter_name),
                                    /*duration =*/ android.widget.Toast.LENGTH_SHORT
                                  ).show();
                              } /*if*/
                          }
                        else
                          {
                          /* show user cleaned text */
                            SaveAsText.setText(TheCleanedText);
                          } /*if*/
                      } /*onClick*/
                  }
              );
          }
        else
          {
            android.widget.Toast.makeText
              (
                /*context =*/ this,
                /*text =*/ getString(R.string.no_external_storage),
                /*duration =*/ android.widget.Toast.LENGTH_SHORT
              ).show();
            setResult(android.app.Activity.RESULT_CANCELED);
            finish();
          } /*if*/
      } /*onCreate*/

  } /*SaveAs*/
