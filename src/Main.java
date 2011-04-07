package nz.gen.geek_central.ti5x;

public class Main extends android.app.Activity
  {

  /* button functions, colours TBD */
    static class ButtonDef
      {
        String Text, AltText;

        public ButtonDef
          (
            String Text,
            String AltText
          )
          {
            this.Text = Text;
            this.AltText = AltText;
          } /*ButtonDef*/

        public ButtonDef
          (
            String Text
          )
          {
            this.Text = Text;
            this.AltText = "";
          } /*ButtonDef*/

      } /*ButtonDef*/

    static ButtonDef[][] ButtonDefs =
        {
            new ButtonDef[]
                {
                    new ButtonDef("A", "A'"),
                    new ButtonDef("B", "B'"),
                    new ButtonDef("C", "C'"),
                    new ButtonDef("D", "D'"),
                    new ButtonDef("E", "E'"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("2nd"),
                    new ButtonDef("INV"),
                    new ButtonDef("lnx", "log"),
                    new ButtonDef("CE", "CP"),
                    new ButtonDef("CLR"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("LRN", "Pgm"),
                    new ButtonDef("x⇄t", "P→R"),
                    new ButtonDef("x²", "sin"),
                    new ButtonDef("sqrt(x)", "cos"),
                    new ButtonDef("1/x", "tan"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SST", "Ins"),
                    new ButtonDef("STO", "CMs"),
                    new ButtonDef("RCL", "Exc"),
                    new ButtonDef("SUM", "Prd"),
                    new ButtonDef("y**x", "Ind"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("BST", "Del"),
                    new ButtonDef("EE", "Eng"),
                    new ButtonDef("(", "Fix"),
                    new ButtonDef(")", "Int"),
                    new ButtonDef("÷", "|x|"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("GTO", "Pause"),
                    new ButtonDef("7", "x=t"),
                    new ButtonDef("8", "Nop"),
                    new ButtonDef("9", "Op"),
                    new ButtonDef("×", "Deg"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("SBR", "Lbl"),
                    new ButtonDef("4", "x≥t"),
                    new ButtonDef("5", "∑x"),
                    new ButtonDef("6", "mean(x)"),
                    new ButtonDef("-", "Rad"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("RST", "St flg"),
                    new ButtonDef("1", "If flg"),
                    new ButtonDef("2", "D.MS"),
                    new ButtonDef("3", "π"),
                    new ButtonDef("+", "Grad"),
                },
            new ButtonDef[]
                {
                    new ButtonDef("R/S"),
                    new ButtonDef("0", "Dsz"),
                    new ButtonDef(".", "Adv"),
                    new ButtonDef("+/-", "Prt"),
                    new ButtonDef("=", "List"),
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
        for (ButtonDef[] ThisDefRow : ButtonDefs)
          {
            final android.widget.TableRow ThisRow = new android.widget.TableRow(this);
            for (ButtonDef ThisButtonDef : ThisDefRow)
              {
                final android.widget.Button ThisButton = new android.widget.Button(this);
                ThisButton.setText(ThisButtonDef.Text);
              /* AltText, colour TBD */
                ThisRow.addView(ThisButton);
              } /*for*/
            Buttons.addView(ThisRow);
          } /*for*/
      /* more TBD */
      } /*onCreate*/

  } /*Main*/
