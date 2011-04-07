package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  {

  /* button functions TBD */
    static class ButtonDef
      {
        final String Text, AltText;
        final int TextColor, ButtonColor;

        public ButtonDef
          (
            String Text,
            String AltText,
            int TextColor,
            int ButtonColor
          )
          {
            this.Text = Text;
            this.AltText = AltText;
            this.TextColor = TextColor;
            this.ButtonColor = ButtonColor;
          } /*ButtonDef*/
      } /*ButtonDef*/

    static ButtonDef[][] ButtonDefs =
        {
            new ButtonDef[]
                {
                    new ButtonDef("A", "A´", Button.White, Button.Brown),
                    new ButtonDef("B", "B´", Button.White, Button.Brown),
                    new ButtonDef("C", "C´", Button.White, Button.Brown),
                    new ButtonDef("D", "D´", Button.White, Button.Brown),
                    new ButtonDef("E", "E´", Button.White, Button.Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("2nd", "", Button.Black, Button.Yellow),
                    new ButtonDef("INV", "", Button.White, Button.Brown),
                    new ButtonDef("lnx", "log", Button.White, Button.Brown),
                    new ButtonDef("CE", "CP", Button.White, Button.Brown),
                    new ButtonDef("CLR", "", Button.Black, Button.Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("LRN", "Pgm", Button.White, Button.Brown),
                    new ButtonDef("x⇌t", "P→R", Button.White, Button.Brown),
                    new ButtonDef("x²", "sin", Button.White, Button.Brown),
                    new ButtonDef("√x", "cos", Button.White, Button.Brown),
                    new ButtonDef("1/x", "tan", Button.White, Button.Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SST", "Ins", Button.White, Button.Brown),
                    new ButtonDef("STO", "CMs", Button.White, Button.Brown),
                    new ButtonDef("RCL", "Exc", Button.White, Button.Brown),
                    new ButtonDef("SUM", "Prd", Button.White, Button.Brown),
                    new ButtonDef("y**x", "Ind", Button.White, Button.Brown),
                },
            new ButtonDef[]
                {
                    new ButtonDef("BST", "Del", Button.White, Button.Brown),
                    new ButtonDef("EE", "Eng", Button.White, Button.Brown),
                    new ButtonDef("(", "Fix", Button.White, Button.Brown),
                    new ButtonDef(")", "Int", Button.White, Button.Brown),
                    new ButtonDef("÷", "|x|", Button.Black, Button.Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("GTO", "Pause", Button.White, Button.Brown),
                    new ButtonDef("7", "x=t", Button.Black, Button.White),
                    new ButtonDef("8", "Nop", Button.Black, Button.White),
                    new ButtonDef("9", "Op", Button.Black, Button.White),
                    new ButtonDef("×", "Deg", Button.Black, Button.Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SBR", "Lbl", Button.White, Button.Brown),
                    new ButtonDef("4", "x≥t", Button.Black, Button.White),
                    new ButtonDef("5", "∑x", Button.Black, Button.White),
                    new ButtonDef("6", "mean(x)", Button.Black, Button.White),
                    new ButtonDef("-", "Rad", Button.Black, Button.Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("RST", "St flg", Button.White, Button.Brown),
                    new ButtonDef("1", "If flg", Button.Black, Button.White),
                    new ButtonDef("2", "D.MS", Button.Black, Button.White),
                    new ButtonDef("3", "π", Button.Black, Button.White),
                    new ButtonDef("+", "Grad", Button.Black, Button.Yellow),
                },
            new ButtonDef[]
                {
                    new ButtonDef("R/S", "", Button.White, Button.Brown),
                    new ButtonDef("0", "Dsz", Button.Black, Button.White),
                    new ButtonDef(".", "Adv", Button.Black, Button.White),
                    new ButtonDef("+/-", "Prt", Button.Black, Button.White),
                    new ButtonDef("=", "List", Button.Black, Button.Yellow),
                },
        };

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final android.widget.TableLayout Buttons =
            (android.widget.TableLayout)findViewById(R.id.buttons);
        Buttons.setBackgroundColor(Button.Black);
        for (ButtonDef[] ThisDefRow : ButtonDefs)
          {
            final android.widget.TableRow ThisRow = new android.widget.TableRow(this);
            for (ButtonDef ThisButtonDef : ThisDefRow)
              {
                final Button ThisButton = new Button
                  (
                    this,
                    ThisButtonDef.Text,
                    ThisButtonDef.AltText,
                    ThisButtonDef.TextColor,
                    ThisButtonDef.ButtonColor,
                    new Runnable()
                      {
                        public void run()
                          {
                          /* do nothing for now */
                          } /*run*/
                      },
                    null,
                    null,
                    null
                  );
              /* AltText, colour TBD */
                ThisRow.addView(ThisButton);
              } /*for*/
            Buttons.addView(ThisRow);
          } /*for*/
      /* more TBD */
      } /*onCreate*/

  } /*Main*/
