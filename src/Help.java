package nz.gen.geek_central.ti5x;
/*
    Help-page display

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

public class Help extends android.app.Activity
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

