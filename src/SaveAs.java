package nz.gen.geek_central.ti5x;
/*
    Let the user enter a name for saving a new program file.

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

public class SaveAs extends android.app.Activity
  {
    protected android.widget.EditText SaveAsText;
    String TheCleanedText;

    class OverwriteConfirm
        extends android.app.AlertDialog
        implements android.content.DialogInterface.OnClickListener
      {
        public OverwriteConfirm
          (
            android.content.Context ctx,
            String FileName
          )
          {
            super(ctx);
            setIcon(android.R.drawable.ic_dialog_alert); /* doesn't work? */
            setMessage
              (
                String.format(Global.StdLocale, ctx.getString(R.string.file_exists), FileName)
              );
            setButton
              (
                android.content.DialogInterface.BUTTON_POSITIVE,
                ctx.getString(R.string.overwrite),
                this
              );
            setButton
              (
                android.content.DialogInterface.BUTTON_NEGATIVE,
                ctx.getString(R.string.cancel),
                this
              );
          } /*OverwriteConfirm*/

        @Override
        public void onClick
          (
            android.content.DialogInterface TheDialog,
            int WhichButton
          )
          {
            if (WhichButton == android.content.DialogInterface.BUTTON_POSITIVE)
              {
                ReturnResult();
              } /*if*/
            dismiss();
          } /*onClick*/

      } /*OverwriteConfirm*/

    void ReturnResult()
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
      } /*ReturnResult*/

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
                        TheCleanedText = Clean.toString();
                        if (TheCleanedText.equals(TheOrigText))
                          {
                            if (TheCleanedText.length() != 0)
                              {
                                if
                                  (
                                    new java.io.File
                                      (
                                            android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                                        +
                                            "/"
                                        +
                                            Persistent.ProgramsDir
                                        +
                                            "/"
                                        +
                                            TheCleanedText
                                        +
                                            Persistent.ProgExt
                                      ).exists()
                                  )
                                  {
                                    new OverwriteConfirm(SaveAs.this, TheCleanedText).show();
                                  }
                                else
                                  {
                                    ReturnResult();
                                  } /*if*/
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
