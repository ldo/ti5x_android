package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- virtual printer display

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

public class PrinterView extends android.app.Activity
  {
    android.widget.ScrollView PaperScroll;
    PaperView ThePaper;
    boolean FirstView = true;

    class PaperChangedListener implements Printer.Notifier
      {

        public void PaperChanged()
          {
            PaperScroll.scrollTo(0, ThePaper.GetViewHeight());
            ThePaper.invalidate();
          } /*PaperChanged*/

      } /*PaperChangedListener*/

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.printer);
        PaperScroll = (android.widget.ScrollView)findViewById(R.id.paper_scroll);
        ThePaper = (PaperView)findViewById(R.id.paper);
      } /*onCreate*/

    @Override
    public void onPause()
      {
        super.onPause();
        if (Global.Print != null)
          {
            Global.Print.PrintListener = null;
          } /*if*/
      } /*onPause*/

    @Override
    public void onResume()
      {
        super.onResume();
        if (Global.Print != null)
          {
            Global.Print.PrintListener = new PaperChangedListener();
          } /*if*/
      } /*onResume*/

    @Override
    public void onWindowFocusChanged
      (
        boolean HasFocus
      )
      {
        super.onWindowFocusChanged(HasFocus);
        if (HasFocus && FirstView)
          {
            PaperScroll.fullScroll(android.view.View.FOCUS_DOWN);
            FirstView = false;
          } /*if*/
      } /*onWindowFocusChanged*/

  } /*PrinterView*/
