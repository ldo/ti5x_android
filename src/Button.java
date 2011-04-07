package nz.gen.geek_central.ti5x;

public class Button extends android.view.View
  /* display and interaction with a calculator button */
  {
  /* colour scheme: */
    public static final int Black = 0xff222424;
    public static final int Brown = 0xff4e4238;
    public static final int Yellow = 0xffcc9858;
    public static final int White = 0xffbdaa7d;

  /* global modifier state */
    public static boolean AltState = false;
    public static boolean InvState = false;

    protected String Text, AltText;
    protected int TextColor, ButtonColor, AltTextColor, BGColor;
    protected Runnable BaseAction, InvAction, AltAction, AltInvAction;
    protected boolean IsModifier;

    public Button
      (
        android.content.Context TheContext,
        String Text,
        String AltText,
        int TextColor,
        int ButtonColor,
        Runnable BaseAction,
        Runnable InvAction,
        Runnable AltAction,
        Runnable AltInvAction
      )
      /* constructs a non-modifier Button. */
      {
        super(TheContext);
        this.Text = Text;
        this.AltText = AltText;
        this.TextColor = TextColor;
        this.ButtonColor = ButtonColor;
        this.AltTextColor = White;
        this.BGColor = Black;
        this.BaseAction = BaseAction;
        this.InvAction = InvAction != null ? InvAction : BaseAction;
        this.AltAction = AltAction != null ? AltAction : BaseAction;
        this.AltInvAction = AltInvAction != null ? AltInvAction : InvAction;
        this.IsModifier = false;
        setOnTouchListener
          (
            new android.view.View.OnTouchListener()
              {
                public boolean onTouch
                  (
                    android.view.View TheView,
                    android.view.MotionEvent TheEvent
                  )
                  {
                    boolean Handled = false;
                    final Button TheButton = (Button)TheView;
                    switch (TheEvent.getAction())
                      {
                    case android.view.MotionEvent.ACTION_DOWN:
                        TheView.setPressed(true);
                        TheView.invalidate();
                        Handled = true;
                    break;
                    case android.view.MotionEvent.ACTION_UP:
                        System.err.println("Clicked button " + ((Button)TheView).Text); /* debug */
                        TheButton.Invoke();
                        TheView.setPressed(false);
                        TheView.invalidate();
                        Handled = true;
                    break;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        TheView.setPressed(false);
                        TheView.invalidate();
                        Handled = true;
                    break;
                      } /*switch*/
                    return
                        Handled;
                  } /*onClick*/
              }
          );
      } /*Button*/

    @Override
    protected void onMeasure
      (
        int WidthMeasureSpec,
        int HeightMeasureSpec
      )
      {
        setMeasuredDimension(60, 40);
      } /*onMeasure*/

    protected static Button ModifierButton
      (
        android.content.Context TheContext,
        String Text,
        int TextColor,
        int ButtonColor,
        Runnable Action
      )
      /* common internal routine for constructing modifier buttons. */
      {
        final Button Result = new Button
          (
            /*TheContext =*/ TheContext,
            /*Text =*/ Text,
            /*AltText =*/ "",
            /*TextColor =*/ TextColor,
            /*ButtonColor =*/ ButtonColor,
            /*BaseAction =*/ Action,
            /*InvAction =*/ Action,
            /*AltAction =*/ Action,
            /*AltInvAction =*/ Action
          );
        Result.IsModifier = true;
        return
            Result;
      } /*ModifierButton*/

    public static Button InvButton
      (
        android.content.Context TheContext,
        String Text,
        int TextColor,
        int ButtonColor
      )
      /* constructs the “INV” modifier button. */
      {
        return
            ModifierButton
              (
                /*TheContext =*/ TheContext,
                /*Text =*/ Text,
                /*TextColor =*/ TextColor,
                /*ButtonColor =*/ ButtonColor,
                /*Action =*/
                    new Runnable()
                      {
                        public void run()
                          {
                            Button.InvState = !Button.InvState;
                          } /*run*/
                      }
              );
      } /*InvButton*/

    public static Button AltButton
      (
        android.content.Context TheContext,
        String Text,
        int TextColor,
        int ButtonColor
      )
      /* constructs the “2nd” modifier Button. */
      {
        return
            ModifierButton
              (
                /*TheContext =*/ TheContext,
                /*Text =*/ Text,
                /*TextColor =*/ TextColor,
                /*ButtonColor =*/ ButtonColor,
                /*Action =*/
                    new Runnable()
                      {
                        public void run()
                          {
                            Button.AltState = !Button.AltState;
                          } /*run*/
                      }
              );
      } /*AltButton*/

    void DrawCenteredText
      (
        android.graphics.Canvas Draw,
        String TheText,
        float x,
        float y,
        android.graphics.Paint UsePaint
      )
      /* draws text at position x, vertically centred around y. */
      {
        final android.graphics.Rect TextBounds = new android.graphics.Rect();
        UsePaint.getTextBounds(TheText, 0, TheText.length(), TextBounds);
        Draw.drawText
          (
            TheText,
            x, /* depend on UsePaint to align horizontally */
            y + (TextBounds.bottom - TextBounds.top) / 2.0f,
            UsePaint
          );
      } /*DrawCenteredText*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas Draw
      )
      {
        super.onDraw(Draw);
        final android.graphics.Rect Bounds =
            new android.graphics.Rect(0, 0, getWidth(), getHeight());
          {
            final android.graphics.Paint BG = new android.graphics.Paint();
            BG.setStyle(android.graphics.Paint.Style.FILL);
            BG.setColor(BGColor);
            Draw.drawRect(Bounds, BG);
          }
          {
            final android.graphics.Rect AltTextBounds =
                new android.graphics.Rect
                  (
                    Bounds.left,
                    Bounds.top,
                    Bounds.right,
                    Bounds.top + (Bounds.bottom - Bounds.top) / 2
                  );
            final android.graphics.Paint TextPaint = new android.graphics.Paint();
            TextPaint.setStyle(android.graphics.Paint.Style.FILL);
            TextPaint.setColor(AltTextColor);
            TextPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
            TextPaint.setTextSize(TextPaint.getTextSize() * 0.9f);
            DrawCenteredText
              (
                Draw,
                AltText,
                (AltTextBounds.left + AltTextBounds.right) / 2.0f,
                (AltTextBounds.bottom + AltTextBounds.top) / 2.0f + 2.0f,
                TextPaint
              );
          }
          {
            final android.graphics.RectF ClickableBounds =
                new android.graphics.RectF
                  (
                    Bounds.left + 4,
                    Bounds.top + (Bounds.bottom - Bounds.top) / 2,
                    Bounds.right - 4,
                    Bounds.bottom
                  );
            final android.graphics.Paint UsePaint = new android.graphics.Paint();
            UsePaint.setStyle(android.graphics.Paint.Style.FILL);
            UsePaint.setColor(isPressed() ? TextColor : ButtonColor);
            Draw.drawRoundRect(ClickableBounds, 2.0f, 2.0f, UsePaint);
            UsePaint.setColor(isPressed() ? ButtonColor : TextColor);
            UsePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
            UsePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            UsePaint.setTextSize(UsePaint.getTextSize() * 1.2f);
            DrawCenteredText
              (
                Draw,
                Text,
                (ClickableBounds.left + ClickableBounds.right) / 2.0f,
                (ClickableBounds.bottom + ClickableBounds.top) / 2.0f,
                UsePaint
              );
          }
      } /*onDraw*/

    public void Invoke()
      {
        (AltState ?
            InvState ?
                AltInvAction
            :
                AltAction
        :
            InvState ?
                InvAction
            :
                BaseAction
        ).run();
        if (!IsModifier)
          {
            Button.InvState = false;
            Button.AltState = false;
          } /*if*/
      } /*Invoke*/

  } /*Button*/

