package addressbook.view;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A button with rounded corners. The colour changes smoothly (animation)
 * when the mouse enters or leaves, and gets darker while it is pressed.
 */
public class RoundedButton extends JButton {

    public enum Style { PRIMARY, DANGER, NEUTRAL }

    private static final int ARC = 18;

    private final Color baseColor;
    private final Color hoverColor;
    private final Color textColor;

    private float progress = 0f;     // 0 = normal colour, 1 = hover colour
    private float target = 0f;
    private boolean pressed = false;
    private final Timer animation;

    public RoundedButton(String text, Style style) {
        super(text);
        switch (style) {
            case DANGER:
                baseColor = Theme.DANGER;
                hoverColor = Theme.DANGER_HOVER;
                textColor = Color.WHITE;
                break;
            case NEUTRAL:
                baseColor = Theme.NEUTRAL;
                hoverColor = Theme.NEUTRAL_HOVER;
                textColor = Theme.TEXT;
                break;
            default:
                baseColor = Theme.PRIMARY;
                hoverColor = Theme.PRIMARY_HOVER;
                textColor = Color.WHITE;
        }
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.FONT_BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(120, 40));

        animation = new Timer(15, e -> step());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    target = 1f;
                    animation.start();
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                target = 0f;
                pressed = false;
                animation.start();
            }
            @Override public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    private void step() {
        float diff = target - progress;
        if (Math.abs(diff) < 0.06f) {
            progress = target;
            animation.stop();
        } else {
            progress += Math.signum(diff) * 0.16f;
        }
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        if (!enabled) {
            progress = 0f;
            target = 0f;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth();
        int h = getHeight();

        Color fill;
        Color text;
        if (!isEnabled()) {
            fill = Theme.DISABLED_BG;
            text = Theme.DISABLED_TEXT;
        } else {
            fill = Theme.blend(baseColor, hoverColor, progress);
            if (pressed) {
                fill = Theme.blend(fill, Color.BLACK, 0.12f);
            }
            text = textColor;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);

        if (isFocusOwner() && isEnabled()) {
            g2.setColor(new Color(59, 125, 216, 130));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, ARC - 2, ARC - 2);
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(text);
        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}
