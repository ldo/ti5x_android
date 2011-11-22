package nz.gen.geek_central.ti5x;
/*
    ti5x calculator emulator -- global data

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

public class Global
  {
    public static Display Disp;
    public static LabelCard Label;
    public static ButtonGrid Buttons;
    public static State Calc;
    public static Importer Import;
    public static Exporter Export;
    public static Printer Print;

    public static final int NrSigFigures = 16;
      /* for formatting reals */
    public static final java.util.Locale StdLocale = java.util.Locale.US;
      /* for all those places I don't want formatting to be locale-specific */

    public static android.view.View ProgressWidgets;
    public static android.widget.TextView ProgressMessage;
    public static android.os.Handler UIRun;

    public interface Task
      {
        public void BGRun();
          /* to be run in a background thread */

        public void PostRun
          (
            int TaskStatus,
            Throwable TaskFailure
          );
          /* to be run on UI thread after BGRun has finished */
      } /*Task*/

    private static class BGTask extends Thread
      {
        private final Task RunWhat;
        private int TaskStatus;
        private Throwable TaskFailure;

        public BGTask
          (
            Task RunWhat
          )
          {
            super();
            this.RunWhat = RunWhat;
            TaskStatus = 0; /* to begin with */
          } /*BGTask*/

        public void run()
          {
            RunWhat.BGRun();
            if (CurrentBGTask == this)
              {
                CurrentBGTask = null;
              }
            else
              {
                throw new RuntimeException("Trying to stop wrong background task");
              } /*if*/
            UIRun.post
              (
                new Runnable()
                  {
                    public void run()
                      {
                        ProgressWidgets.setVisibility(android.view.View.INVISIBLE);
                        RunWhat.PostRun(TaskStatus, TaskFailure);
                      } /*run*/
                  } /*Runnable*/
              );
          } /*run*/

      } /*BGTask*/

    private static BGTask CurrentBGTask;

    public static void StartBGTask
      (
        Task RunWhat,
        String ProgressMessage
      )
      {
        if (CurrentBGTask == null)
          {
            Global.ProgressMessage.setText(ProgressMessage);
            CurrentBGTask = new BGTask(RunWhat);
          /* short delay before making progress widget visible so that user
            doesn't see anything if task finishes quickly enough */
            UIRun.postDelayed
              (
                new Runnable()
                  {
                    public void run()
                      {
                        if (CurrentBGTask != null)
                          {
                            ProgressWidgets.setVisibility(android.view.View.VISIBLE);
                          } /*if*/
                      } /*run*/
                  } /*Runnable*/,
                1000
              );
            CurrentBGTask.start();
          }
        else
          {
            throw new RuntimeException("Trying to start second background task");
          } /*if*/
      } /*StartingBGTask*/

    public static void SetTaskStatus
      (
        Task Me,
        int NewStatus,
        Throwable Failure
      )
      {
        if (CurrentBGTask != null && Me == CurrentBGTask.RunWhat)
          {
            CurrentBGTask.TaskStatus = NewStatus;
            CurrentBGTask.TaskFailure = Failure;
          }
        else
          {
            throw new RuntimeException("SetTaskStatus called from wrong task");
          } /*if*/
      } /*SetTaskStatus*/

    public static boolean BGTaskInProgress()
      {
        return
            CurrentBGTask != null;
      } /*BGTaskInProgress*/

  } /*Global*/
