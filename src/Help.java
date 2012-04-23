package nz.gen.geek_central.ti5x;
/*
    Help-page display

    Copyright 2011, 2012 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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
    public static String ContentID = "nz.gen.geek-central.ti5x.HelpContent";
    android.webkit.WebView HelpView;
  /* for remembering scroll position of last page displayed: */
    static String LastContent;
    static android.graphics.Point LastScroll = null;

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
        final String NewContent = new String(MyIntent.getByteArrayExtra(ContentID));
        HelpView.loadDataWithBaseURL
          (
            /*baseUrl =*/ null,
            /*data =*/ NewContent,
            /*mimeType =*/ null, /* text/html */
            /*encoding =*/ "utf-8",
            /*historyUrl =*/ null
          );
        if
          (
            (NewContent != null && LastContent != null ?
                !NewContent.equals(LastContent)
            :
                NewContent != LastContent
            )
          )
          {
            LastScroll = null;
          } /*if*/
        LastContent = NewContent;
        HelpView.setPictureListener
          (
            new android.webkit.WebView.PictureListener()
              {
                @Override
                public void onNewPicture
                  (
                    android.webkit.WebView HelpView,
                    android.graphics.Picture ThePicture
                  )
                  {
                    if (LastScroll != null)
                      {
                        HelpView.scrollTo(LastScroll.x, LastScroll.y);
                        LastScroll = null; /* only do once */
                      } /*if*/
                  } /*onNewPicture*/
              } /*PictureListener*/
          );
      } /*onCreate*/

    @Override
    public void onDestroy()
      {
        LastScroll = new android.graphics.Point(HelpView.getScrollX(), HelpView.getScrollY());
        super.onDestroy();
      } /*onDestroy*/

  } /*Help*/
