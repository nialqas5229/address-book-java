package addressbook.view;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A text field with rounded corners, a grey hint text, a blue border while
 * it has focus and a red border when it holds invalid data.
 */
public class RoundedTextField extends JTextField {

    private static final int ARC = 16;

    private final String hint;
    private boolean error = false;

    public RoundedTextField(String hint) {
        this.hint = hint;
        setOpaque(false);
        setFont(Theme.FONT);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.SELECTED);
        setSelectedTextColor(Theme.TEXT);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setPreferredSize(new Dimension(200, 42));
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    public void setError(boolean error) {
        if (this.error != error) {
            this.error = error;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth();
        int h = getHeight();

        g2.setColor(isEnabled() ? Color.WHITE : Theme.DISABLED_FIELD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        g2.dispose();

        super.paintComponent(g);                    // the typed text

        Graphics2D g3 = (Graphics2D) g.create();
        Theme.smooth(g3);
        if (getText().isEmpty() && isEnabled()) {   // hint text
            g3.setFont(getFont());
            g3.setColor(new Color(0xA0AAB6));
            int y = (h - g3.getFontMetrics().getHeight()) / 2 + g3.getFontMetrics().getAscent();
            g3.drawString(hint, getInsets().left, y);
        }
        Color border = error ? Theme.ERROR : (isFocusOwner() ? Theme.PRIMARY : Theme.BORDER);
        g3.setColor(border);
        g3.setStroke(new BasicStroke(error || isFocusOwner() ? 2f : 1.2f));
        g3.drawRoundRect(1, 1, w - 3, h - 3, ARC, ARC);
        g3.dispose();
    }
}
