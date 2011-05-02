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
    java.util.Map<android.view.MenuItem, Runnable> OptionsMenu;

    interface RequestResponseAction /* response to an activity result */
      {
        public void Run
          (
            android.content.Intent Data
          );
      } /*RequestResponseAction*/

    java.util.Map<Integer, RequestResponseAction> ActivityResultActions;

    protected final int LoadProgramRequest = 1; /* arbitrary code */
    protected final int SaveProgramRequest = 2; /* arbitrary code */
    boolean ShuttingDown = false;
    boolean StateLoaded = false; /* will be reset to false every time activity is killed and restarted */

    @Override
    public boolean onCreateOptionsMenu
      (
        android.view.Menu TheMenu
      )
      {
        OptionsMenu = new java.util.HashMap<android.view.MenuItem, Runnable>();
        android.view.MenuItem ThisItem;
        OptionsMenu.put
          (
            TheMenu.add(R.string.show_calc_help),
            new Runnable()
              {
                public void run()
                  {
                    startActivity
                      (
                        new android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setClass(Main.this, Help.class)
                      );
                  } /*run*/
              } /*Runnable*/
          );
        ThisItem = TheMenu.add(R.string.show_overlay);
        OptionsMenu.put
          (
            ThisItem,
            new Runnable()
              {
                public void run()
                  {
                    Global.Buttons.OverlayVisible = !Global.Buttons.OverlayVisible;
                    Global.Buttons.invalidate();
                  /* ToggleOverlayItem.setChecked(Global.Buttons.OverlayVisible); */ /* apparently can't do this in initial part of options menu */
                  } /*run*/
              } /*Runnable*/
          );
      /* ThisItem.setCheckable(true); */ /* apparently can't do this in initial part of options menu */
        OptionsMenu.put
          (
            TheMenu.add(R.string.show_module_help),
            new Runnable()
              {
                public void run()
                  {
                    if (Global.Calc != null && Global.Calc.ModuleHelp != null)
                      {
                        final android.content.Intent ShowHelp =
                            new android.content.Intent(android.content.Intent.ACTION_VIEW);
                        ShowHelp.putExtra(nz.gen.geek_central.ti5x.Help.ContentID, Global.Calc.ModuleHelp);
                        ShowHelp.setClass(Main.this, Help.class);
                        startActivity(ShowHelp);
                      }
                    else
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/ getString(R.string.no_module_help),
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      } /*if*/
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.show_printer),
            new Runnable()
              {
                public void run()
                  {
                    startActivity
                      (
                        new android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setClass(Main.this, PrinterView.class)
                      );
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.load_prog),
            new Runnable()
              {
                public void run()
                  {
                    startActivityForResult
                      (
                        new android.content.Intent(android.content.Intent.ACTION_PICK)
                            .setClass(Main.this, Picker.class),
                        LoadProgramRequest
                      );
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.save_as),
            new Runnable()
              {
                public void run()
                  {
                    startActivityForResult
                      (
                        new android.content.Intent(android.content.Intent.ACTION_PICK)
                            .setClass(Main.this, SaveAs.class),
                        SaveProgramRequest
                      );
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.turn_off),
            new Runnable()
              {
                public void run()
                  {
                    ShuttingDown = true;
                    deleteFile(Persistent.SavedStateName); /* lose any saved state */
                    finish(); /* start afresh next time */
                  } /*run*/
              } /*Runnable*/
          );
        return
            true;
      } /*onCreateOptionsMenu*/

    void BuildActivityResultActions()
      {
        ActivityResultActions = new java.util.HashMap<Integer, RequestResponseAction>();
        ActivityResultActions.put
          (
            LoadProgramRequest,
            new RequestResponseAction()
              {
                public void Run
                  (
                    android.content.Intent Data
                  )
                  {
                    final String ProgName = Data.getData().getPath();
                    final String PickedExt = Data.getStringExtra(Picker.ExtID);
                    final boolean IsLib = PickedExt.intern() == Persistent.LibExt.intern();
                    final boolean LoadingMasterLibrary = IsLib && ProgName.intern() == "/";
                  /* It appears onActivityResult is liable to be called before
                    onResume. Therefore I do additional restoring/saving state
                    here to ensure the saved state includes the newly-loaded
                    program/library. */
                    if (!StateLoaded)
                      {
                        Persistent.RestoreState(Main.this); /* if not already done */
                        StateLoaded = true;
                      } /*if*/
                    try
                      {
                        if (LoadingMasterLibrary)
                          {
                            Persistent.LoadMasterLibrary(Main.this);
                          }
                        else
                          {
                            Persistent.Load
                              (
                                /*FromFile =*/ ProgName,
                                /*Libs =*/ IsLib,
                                /*AllState =*/ false,
                                /*Disp =*/ Global.Disp,
                                /*Help =*/ Global.Help,
                                /*Buttons =*/ Global.Buttons,
                                /*Calc =*/ Global.Calc
                              );
                          } /*if*/
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/
                                String.format
                                  (
                                    Global.StdLocale,
                                    getString
                                      (
                                        IsLib ?
                                            R.string.library_loaded
                                        :
                                            R.string.program_loaded
                                      ),
                                    LoadingMasterLibrary ?
                                        getString(R.string.master_library)
                                    :
                                        new java.io.File(ProgName).getName()
                                  ),
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      }
                    catch (Persistent.DataFormatException Failed)
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/
                                String.format
                                  (
                                    Global.StdLocale,
                                    getString(R.string.file_load_error),
                                    Failed.toString()
                                  ),
                            /*duration =*/ android.widget.Toast.LENGTH_LONG
                          ).show();
                      } /*try*/
                    Persistent.SaveState(Main.this);
                  } /*Run*/
              } /*RequestResponseAction*/
          );
        ActivityResultActions.put
          (
            SaveProgramRequest,
            new RequestResponseAction()
              {
                public void Run
                  (
                    android.content.Intent Data
                  )
                  {
                    final String TheName =
                            Data.getData().getPath().substring(1) /* ignoring leading slash */
                        +
                            Persistent.ProgExt;
                    try
                      {
                        final String SaveDir =
                                android.os.Environment.getExternalStorageDirectory()
                                    .getAbsolutePath()
                            +
                                "/"
                            +
                                Persistent.ProgramsDir;
                        new java.io.File(SaveDir).mkdirs();
                        Persistent.Save
                          (
                            /*Buttons =*/ Global.Buttons,
                            /*Calc =*/ Global.Calc,
                            /*Libs =*/ false,
                            /*AllState =*/ false,
                            /*ToFile =*/ SaveDir + "/" + TheName
                          );
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/ String.format
                              (
                                Global.StdLocale,
                                getString(R.string.program_saved),
                                TheName
                              ),
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      }
                    catch (RuntimeException Failed)
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/
                                String.format
                                  (
                                    Global.StdLocale,
                                    getString(R.string.program_save_error),
                                    Failed.toString()
                                  ),
                            /*duration =*/ android.widget.Toast.LENGTH_LONG
                          ).show();
                      } /*try*/
                  } /*Run*/
              } /*RequestResponseAction*/
          );
      } /*BuildActivityResultActions*/

    @Override
    public boolean onOptionsItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = OptionsMenu.get(TheItem);
        if (Action != null)
          {
            Action.run();
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
        if (ResultCode == android.app.Activity.RESULT_OK)
          {
            final RequestResponseAction Action = ActivityResultActions.get(RequestCode);
            if (Action != null)
              {
                Action.Run(Data);
              } /*if*/
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
        Global.Disp = (Display)findViewById(R.id.display);
        Global.Help = (HelpCard)findViewById(R.id.help_card);
        Global.Buttons = (ButtonGrid)findViewById(R.id.buttons);
        Global.Print = new Printer(this);
        Global.Calc = new State();
        BuildActivityResultActions();
      } /*onCreate*/

    @Override
    public void onPause()
      {
        super.onPause();
        if (!ShuttingDown)
          {
            Persistent.SaveState(this);
          } /*if*/
      } /*onPause*/

    @Override
    public void onResume()
      {
        super.onResume();
        if (!StateLoaded)
          {
            Persistent.RestoreState(this);
            StateLoaded = true;
          } /*if*/
      } /*onResume*/

  } /*Main*/
