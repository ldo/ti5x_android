package nz.gen.geek_central.ti5x;
/*
    let the user choose a program or library to load

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

public class Picker extends android.app.Activity
  {
    public static String ExtID = "nz.gen.geek-central.ti5x.PickerExt";

    static boolean Reentered = false; /* sanity check */
    public static Picker Current = null;

    public static class PickerAltList
      /* defining alternative lists of files for picker to display */
      {
        int RadioButtonID;
        String Prompt;
        String NoneFound;
        String FileExt;
        String SpecialItem; /* special item to add to list, null for none */

        public PickerAltList
          (
            int RadioButtonID,
            String Prompt,
            String NoneFound,
            String FileExt,
            String SpecialItem
          )
          {
            this.RadioButtonID = RadioButtonID;
            this.Prompt = Prompt;
            this.NoneFound = NoneFound;
            this.FileExt = FileExt;
            this.SpecialItem = SpecialItem;
          } /*PickerAltList*/
      } /*PickerAltList*/

    static String SelectLabel = null;
    static android.view.View Extra = null;
    static String[] LookIn;
    static PickerAltList[] AltLists = null;

    android.view.ViewGroup MainViewGroup;
    android.widget.TextView PromptView;
    android.widget.ListView PickerListView;
    SelectedItemAdapter PickerList;
    int SelectedAlt; /* index into AltLists */

    public static class PickerItem
      {
        String FullPath, DisplayName;
        boolean Selected;

        public PickerItem
          (
            String FullPath,
            String DisplayName
          )
          {
            this.FullPath = FullPath;
            this.DisplayName = DisplayName;
            this.Selected = false;
          } /*PickerItem*/

        public String toString()
          {
            return
                DisplayName != null ?
                    DisplayName
                :
                    new java.io.File(FullPath).getName();
          } /*toString*/

      } /*PickerItem*/

    class DeleteConfirm
        extends android.app.AlertDialog
        implements android.content.DialogInterface.OnClickListener
      {
        final PickerItem TheFile;

        public DeleteConfirm
          (
            android.content.Context ctx,
            PickerItem TheFile
          )
          {
            super(ctx);
            this.TheFile = TheFile;
            setIcon(android.R.drawable.ic_delete); /* doesn't work? */
            setMessage
              (
                String.format
                  (
                    Global.StdLocale,
                    ctx.getString(R.string.query_delete),
                    TheFile.toString()
                  )
              );
            setButton
              (
                android.content.DialogInterface.BUTTON_POSITIVE,
                ctx.getString(R.string.delete),
                this
              );
            setButton
              (
                android.content.DialogInterface.BUTTON_NEGATIVE,
                ctx.getString(R.string.cancel),
                this
              );
          } /*DeleteConfirm*/

        @Override
        public void onClick
          (
            android.content.DialogInterface TheDialog,
            int WhichButton
          )
          {
            if (WhichButton == android.content.DialogInterface.BUTTON_POSITIVE)
              {
                boolean Deleted;
                try
                  {
                    new java.io.File(TheFile.FullPath).delete();
                    Deleted = true;
                  }
                catch (SecurityException AccessDenied)
                  {
                    android.widget.Toast.makeText
                      (
                        /*context =*/ Picker.this,
                        /*text =*/
                            String.format
                              (
                                Global.StdLocale,
                                getString(R.string.file_delete_error),
                                AccessDenied.toString()
                              ),
                        /*duration =*/ android.widget.Toast.LENGTH_LONG
                      ).show();
                    Deleted = false;
                  } /*try*/
                if (Deleted)
                  {
                    android.widget.Toast.makeText
                      (
                        /*context =*/ Picker.this,
                        /*text =*/
                            String.format
                              (
                                Global.StdLocale,
                                getString(R.string.file_deleted),
                                TheFile.toString()
                              ),
                        /*duration =*/ android.widget.Toast.LENGTH_SHORT
                      ).show();
                    PopulatePickerList(SelectedAlt);
                  } /*if*/
              } /*if*/
            dismiss();
          } /*onClick*/

      } /*DeleteConfirm*/

    class SelectedItemAdapter extends android.widget.ArrayAdapter<PickerItem>
      {
        final int ResID;
        final android.view.LayoutInflater TemplateInflater;
        PickerItem CurSelected;
        android.widget.RadioButton LastChecked;

        class OnSetCheck implements android.view.View.OnClickListener
          {
            final PickerItem MyItem;

            public OnSetCheck
              (
                PickerItem TheItem
              )
              {
                MyItem = TheItem;
              } /*OnSetCheck*/

            public void onClick
              (
                android.view.View TheView
              )
              {
                if (MyItem != CurSelected)
                  {
                  /* only allow one item to be selected at a time */
                    if (CurSelected != null)
                      {
                        CurSelected.Selected = false;
                        LastChecked.setChecked(false);
                      } /*if*/
                    LastChecked =
                        TheView instanceof android.widget.RadioButton ?
                            (android.widget.RadioButton)TheView
                        :
                            (android.widget.RadioButton)
                            ((android.view.ViewGroup)TheView).findViewById(R.id.file_item_checked);
                    CurSelected = MyItem;
                    MyItem.Selected = true;
                    LastChecked.setChecked(true);
                  } /*if*/
              } /*onClick*/
          } /*OnSetCheck*/

        SelectedItemAdapter
          (
            android.content.Context TheContext,
            int ResID,
            android.view.LayoutInflater TemplateInflater
          )
          {
            super(TheContext, ResID);
            this.ResID = ResID;
            this.TemplateInflater = TemplateInflater;
            CurSelected = null;
            LastChecked = null;
          } /*SelectedItemAdapter*/

        @Override
        public android.view.View getView
          (
            int Position,
            android.view.View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            android.view.View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            final PickerItem ThisItem = (PickerItem)this.getItem(Position);
            ((android.widget.TextView)TheView.findViewById(R.id.select_file_name))
                .setText(ThisItem.toString());
            android.widget.RadioButton ThisChecked =
                (android.widget.RadioButton)TheView.findViewById(R.id.file_item_checked);
            ThisChecked.setChecked(ThisItem.Selected);
            final OnSetCheck ThisSetCheck = new OnSetCheck(ThisItem);
            ThisChecked.setOnClickListener(ThisSetCheck);
              /* otherwise radio button can get checked but I don't notice */
            TheView.setOnClickListener(ThisSetCheck);
            TheView.setOnLongClickListener
              (
                new android.view.View.OnLongClickListener()
                  {
                    public boolean onLongClick
                      (
                        android.view.View TheView
                      )
                      {
                        if (ThisItem.FullPath != null) /* cannot delete built-in item */
                          {
                            new DeleteConfirm(Picker.this, ThisItem).show();
                          } /*if*/
                        return
                            true;
                      } /*onLongClick*/
                  } /*OnLongClickListener*/
              );
            return
                TheView;
          } /*getView*/

      } /*SelectedItemAdapter*/

    class OnSelectCategory implements android.view.View.OnClickListener
      /* handler for radio buttons selecting which category of files to display */
      {
        final int SelectAlt;

        OnSelectCategory
          (
            int SelectAlt
          )
          {
            this.SelectAlt = SelectAlt;
          } /*OnSelectCategory*/

        public void onClick
          (
            android.view.View TheView
          )
          {
            PopulatePickerList(SelectAlt);
          } /*onClick*/

      } /*OnSelectCategory*/

    void PopulatePickerList
      (
        int NewAlt /* index into AltLists */
      )
      {
        SelectedAlt = NewAlt;
        final PickerAltList Alt = AltLists[SelectedAlt];
        PickerList.clear();
          {
            final String ExternalStorage =
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            for (String Here : LookIn)
              {
                final java.io.File ThisDir = new java.io.File(ExternalStorage + "/" + Here);
                if (ThisDir.isDirectory())
                  {
                    for (java.io.File Item : ThisDir.listFiles())
                      {
                        if (Item.getName().endsWith(Alt.FileExt))
                          {
                            PickerList.add(new PickerItem(Item.getAbsolutePath(), null));
                          } /*if*/
                      } /*for*/
                  } /* if*/
              } /*for*/
          }
        if (Alt.SpecialItem != null)
          {
            PickerList.add(new PickerItem(null, Alt.SpecialItem));
          } /*if*/
        PromptView.setText
          (
            PickerList.getCount() != 0 ?
                Alt.Prompt
            :
                Alt.NoneFound
          );
        PickerList.notifyDataSetChanged();
      } /*PopulatePickerList*/

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        Picker.Current = this;
        MainViewGroup = (android.view.ViewGroup)getLayoutInflater().inflate(R.layout.picker, null);
        setContentView(MainViewGroup);
      /* ExtraViewGroup = (android.view.ViewGroup)MainViewGroup.findViewById(R.id.picker_extra); */ /* doesn't work -- things added here don't show up */
        PromptView = (android.widget.TextView)findViewById(R.id.picker_prompt);
        PickerList = new SelectedItemAdapter(this, R.layout.picker_item, getLayoutInflater());
        PickerListView = (android.widget.ListView)findViewById(R.id.prog_list);
        PickerListView.setAdapter(PickerList);
        final android.widget.Button SelectButton =
            (android.widget.Button)findViewById(R.id.prog_select);
        SelectButton.setText(SelectLabel);
        SelectButton.setOnClickListener
          (
            new android.view.View.OnClickListener()
              {
                public void onClick
                  (
                    android.view.View TheView
                  )
                  {
                    PickerItem Selected = null;
                    for (int i = 0;;)
                      {
                        if (i == PickerList.getCount())
                            break;
                        final PickerItem ThisItem =
                            (PickerItem)PickerListView.getItemAtPosition(i);
                        if (ThisItem.Selected)
                          {
                            Selected = ThisItem;
                            break;
                          } /*if*/
                        ++i;
                      } /*for*/
                    if (Selected != null)
                      {
                        setResult
                          (
                            android.app.Activity.RESULT_OK,
                            new android.content.Intent()
                                .setData
                                  (
                                    android.net.Uri.fromFile
                                      (
                                        new java.io.File
                                          (
                                            Selected.FullPath != null ?
                                                Selected.FullPath
                                            :
                                                ""
                                          )
                                      )
                                  )
                                .putExtra(ExtID, AltLists[SelectedAlt].FileExt)
                          );
                        finish();
                      } /*if*/
                  } /*onClick*/
              } /*OnClickListener*/
          );
        SelectedAlt = 0;
      } /*onCreate*/

    @Override
    public void onDestroy()
      {
        super.onDestroy();
        Picker.Current = null;
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
            for (int i = 0; i < AltLists.length; ++i)
              {
                final PickerAltList ThisAlt = AltLists[i];
                if (ThisAlt.RadioButtonID != 0)
                  {
                    final android.widget.RadioButton SelectThis =
                        (android.widget.RadioButton)Extra.findViewById(ThisAlt.RadioButtonID);
                    SelectThis.setChecked(i == SelectedAlt);
                    SelectThis.setOnClickListener(new OnSelectCategory(i));
                  } /*if*/
              } /*for*/
          } /*if*/
        PopulatePickerList(SelectedAlt);
      } /*onResume*/

    public static void Launch
      (
        android.app.Activity Acting,
        String SelectLabel,
        int RequestCode,
        android.view.View Extra,
        String[] LookIn, /* array of names of subdirectories within external storage */
        PickerAltList[] AltLists
      )
      {
        if (!Reentered)
          {
            Reentered = true; /* until Picker activity terminates */
            Picker.SelectLabel = SelectLabel;
            Picker.Extra = Extra;
            Picker.LookIn = LookIn;
            Picker.AltLists = AltLists;
            Acting.startActivityForResult
              (
                new android.content.Intent(android.content.Intent.ACTION_PICK)
                    .setClass(Acting, Picker.class),
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
        which means picker context is lost. */
      {
        Extra = null;
        LookIn = null;
        AltLists = null;
        Reentered = false;
      } /*Cleanup*/

  } /*Picker*/
