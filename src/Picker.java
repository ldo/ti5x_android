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
    android.widget.ListView PickerListView;
    SelectedItemAdapter PickerList;
    String SelectedExt;

    class PickerItem
      {
        String FullPath;
        boolean Selected;

        public PickerItem
          (
            String FullPath
          )
          {
            this.FullPath = FullPath;
            this.Selected = false;
          } /*PickerItem*/

        public String toString()
          {
          /* display name for item is leaf filename */
            return
                FullPath != null ?
                    new java.io.File(FullPath).getName()
                :
                    getString(R.string.master_library);
          } /*toString*/

      } /*PickerItem*/

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
                    LastChecked = (android.widget.RadioButton)
                        ((android.view.ViewGroup)TheView).findViewById(R.id.prog_item_checked);
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
          /* the only constructor I need */
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
            ((android.widget.TextView)TheView.findViewById(R.id.select_prog_name))
                .setText(ThisItem.toString());
            android.widget.RadioButton ThisChecked =
                (android.widget.RadioButton)TheView.findViewById(R.id.prog_item_checked);
            ThisChecked.setChecked(ThisItem.Selected);
            TheView.setOnClickListener(new OnSetCheck(ThisItem));
            return
                TheView;
          } /*getView*/

      } /*SelectedItemAdapter*/

    class OnSelectCategory implements android.view.View.OnClickListener
      /* handler for radio buttons selecting which category of files to display */
      {
        final String Ext;
        final boolean IncludeMasterLibrary;

        OnSelectCategory
          (
            String Ext,
            boolean IncludeMasterLibrary
          )
          {
            this.Ext = Ext;
            this.IncludeMasterLibrary = IncludeMasterLibrary;
          } /*OnSelectCategory*/

        public void onClick
          (
            android.view.View TheView
          )
          {
            PopulatePickerList(Ext, IncludeMasterLibrary);
          } /*onClick*/

      } /*OnSelectCategory*/

    void PopulatePickerList
      (
        String Ext,
        boolean IncludeMasterLibrary
      )
      {
        SelectedExt = Ext;
        PickerList.clear();
          {
            final String ExternalStorage =
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            for (String Here : Persistent.ExternalCalcDirectories)
              {
                final java.io.File ThisDir = new java.io.File(ExternalStorage + "/" + Here);
                if (ThisDir.isDirectory())
                  {
                    for (java.io.File Item : ThisDir.listFiles())
                      {
                        if (Item.getName().endsWith(SelectedExt))
                          {
                            PickerList.add(new PickerItem(Item.getAbsolutePath()));
                          } /*if*/
                      } /*for*/
                  } /* if*/
              } /*for*/
          }
        if (IncludeMasterLibrary)
          {
            PickerList.add(new PickerItem(null));
          } /*if*/
        PickerList.notifyDataSetChanged();
      } /*PopulatePickerList*/

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picker);
        PickerList = new SelectedItemAdapter(this, R.layout.picker_item, getLayoutInflater());
        PickerListView = (android.widget.ListView)findViewById(R.id.prog_list);
        PickerListView.setAdapter(PickerList);
          {
            final android.widget.RadioButton SelectSaved =
                (android.widget.RadioButton)findViewById(R.id.select_saved);
            final android.widget.RadioButton SelectLib =
                (android.widget.RadioButton)findViewById(R.id.select_libraries);
            SelectedExt = Persistent.ProgExt;
            SelectSaved.setChecked(Persistent.ProgExt == SelectedExt);
            SelectSaved.setOnClickListener(new OnSelectCategory(Persistent.ProgExt, false));
            SelectLib.setChecked(Persistent.LibExt == SelectedExt);
            SelectLib.setOnClickListener(new OnSelectCategory(Persistent.LibExt, true));
            PopulatePickerList(SelectedExt, Persistent.LibExt == SelectedExt);
          }
        findViewById(R.id.prog_select).setOnClickListener
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
                                .putExtra(ExtID, SelectedExt)
                          );
                        finish();
                      } /*if*/
                  } /*onClick*/
              } /*OnClickListener*/
          );
      } /*onCreate*/

  } /*Picker*/
