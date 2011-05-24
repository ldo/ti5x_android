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
    static android.view.View Extra = null;
    static String SaveWhat = null;
    static String SaveWhere = null;
    static String FileExt = null;

    static boolean Reentered = false; /* sanity check */
    public static SaveAs Current = null;

    android.view.ViewGroup MainViewGroup;
    android.widget.EditText SaveAsText;
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
        SaveAs.Current = this;
        if
          (
                android.os.Environment.getExternalStorageState().intern()
            ==
                android.os.Environment.MEDIA_MOUNTED
          )
          {
            MainViewGroup =
                (android.view.ViewGroup)getLayoutInflater().inflate(R.layout.save_as, null);
            setContentView(MainViewGroup);
            final android.widget.TextView SaveAsPrompt = (android.widget.TextView)findViewById(R.id.save_as_prompt);
            SaveAsPrompt.setText
              (
                String.format(Global.StdLocale, getString(R.string.save_as_prompt), SaveWhat)
              );
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
                                            android.os.Environment.getExternalStorageDirectory()
                                                .getAbsolutePath()
                                        +
                                            "/"
                                        +
                                            SaveWhere
                                        +
                                            "/"
                                        +
                                            TheCleanedText
                                        +
                                            FileExt
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
                                    /*text =*/
                                        String.format
                                          (
                                            Global.StdLocale,
                                            getString(R.string.please_enter_name),
                                            SaveWhat
                                          ),
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

    @Override
    public void onDestroy()
      {
        super.onDestroy();
        SaveAs.Current = null;
      } /*onDestroy*/

    @Override
    public void onPause()
      {
        super.onPause();
        if (Extra != null)
          {
          /* so it can be properly added again should the orientation change */
            MainViewGroup.removeView(Extra);
          } /*if*/
      } /*onPause*/

    @Override
    public void onResume()
      {
        super.onResume();
        if (Extra != null)
          {
            MainViewGroup.addView
              (
                Extra,
                new android.view.ViewGroup.LayoutParams
                  (
                    android.view.ViewGroup.LayoutParams.FILL_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                  )
              );
          } /*if*/
      } /*onResume*/

    public static void Launch
      (
        android.app.Activity Acting,
        int RequestCode,
        String SaveWhat,
        String SaveWhere, /* directory within external storage, for overwrite checking */
        android.view.View Extra,
        String FileExt
      )
      {
        if (!Reentered)
          {
            Reentered = true; /* until SaveAs activity terminates */
            SaveAs.SaveWhat = SaveWhat;
            SaveAs.SaveWhere = SaveWhere;
            SaveAs.Extra = Extra;
            SaveAs.FileExt = FileExt;
            Acting.startActivityForResult
              (
                new android.content.Intent(android.content.Intent.ACTION_PICK)
                    .setClass(Acting, SaveAs.class),
                RequestCode
              );
          }
        else
          {
          /* can happen if user gets impatient and selects from menu twice, just ignore */
          } /*if*/
      } /*Launch*/

    public static void Cleanup()
      /* Client must call this to do explicit cleanup; I tried doing it in
        onDestroy, but of course that gets called when user rotates screen,
        which means activity context is lost. */
      {
        SaveWhat = null;
        SaveWhere = null;
        Extra = null;
        FileExt = null;
        Reentered = false;
      } /*Cleanup*/

  } /*SaveAs*/
