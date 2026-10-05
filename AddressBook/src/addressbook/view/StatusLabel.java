package addressbook.view;

import javax.swing.JLabel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** A status message that stays visible for a moment and then fades out. */
public class StatusLabel extends JLabel {

    private float alpha = 0f;
    private final Timer holdTimer;
    private final Timer fadeTimer;

    public StatusLabel() {
        super(" ");
        setFont(Theme.FONT_BOLD);
        fadeTimer = new Timer(30, null);
        fadeTimer.addActionListener(e -> {
            alpha -= 0.06f;
            if (alpha <= 0f) {
                alpha = 0f;
                fadeTimer.stop();
                setText(" ");
            }
            repaint();
        });
        holdTimer = new Timer(3000, e -> fadeTimer.start());
        holdTimer.setRepeats(false);
    }

    public void show(String message, boolean isError) {
        fadeTimer.stop();
        setText(message);
        setForeground(isError ? Theme.ERROR : Theme.SUCCESS);
        alpha = 1f;
        holdTimer.restart();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        super.paintComponent(g2);
        g2.dispose();
    }
}
