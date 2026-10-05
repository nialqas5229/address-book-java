package addressbook.view;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** A white "card" panel with rounded corners and a soft shadow. */
public class RoundedPanel extends JPanel {

    /** Space reserved around the card for the shadow. Add this to your padding. */
    public static final int SHADOW = 5;
    private static final int ARC = 22;

    public RoundedPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth() - 2 * SHADOW;
        int h = getHeight() - 2 * SHADOW;

        for (int i = SHADOW; i >= 1; i--) {         // soft shadow, outer layers are fainter
            g2.setColor(new Color(31, 41, 51, 9));
            g2.fillRoundRect(SHADOW - i, SHADOW - i + 2, w + 2 * i, h + 2 * i, ARC + i, ARC + i);
        }
        g2.setColor(Theme.CARD);
        g2.fillRoundRect(SHADOW, SHADOW, w, h, ARC, ARC);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(SHADOW, SHADOW, w - 1, h - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}
