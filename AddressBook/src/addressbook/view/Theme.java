package addressbook.view;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** All colours and fonts in one place ("Calm Blue" theme). */
public final class Theme {

    private Theme() { }

    // Colours
    public static final Color BACKGROUND = new Color(0xF4F7FB);
    public static final Color HEADER = new Color(0x2F5D9E);
    public static final Color HEADER_DARK = new Color(0x264F8F);
    public static final Color HEADER_TEXT = Color.WHITE;
    public static final Color PRIMARY = new Color(0x3B7DD8);
    public static final Color PRIMARY_HOVER = new Color(0x2F66B3);
    public static final Color DANGER = new Color(0xD64545);
    public static final Color DANGER_HOVER = new Color(0xB53A3A);
    public static final Color NEUTRAL = new Color(0xE3E9F2);
    public static final Color NEUTRAL_HOVER = new Color(0xD3DCE9);
    public static final Color CARD = Color.WHITE;
    public static final Color SELECTED = new Color(0xDCE9F9);
    public static final Color HOVER_ROW = new Color(0xEEF4FC);
    public static final Color TEXT = new Color(0x1F2933);
    public static final Color MUTED = new Color(0x6B7785);
    public static final Color BORDER = new Color(0xD5DDE8);
    public static final Color ERROR = new Color(0xD64545);
    public static final Color SUCCESS = new Color(0x2E7D5B);
    public static final Color WARNING_ON_DARK = new Color(0xFFD27A);
    public static final Color DISABLED_BG = new Color(0xC9D3E0);
    public static final Color DISABLED_TEXT = new Color(0x8A96A5);
    public static final Color DISABLED_FIELD = new Color(0xEEF2F7);

    // Fonts
    public static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    /** Mix of two colours: t = 0 gives a, t = 1 gives b. */
    public static Color blend(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * k);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k);
        return new Color(r, g, bl);
    }

    public static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Applies the theme to menus, dialogs and other standard Swing parts. */
    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", FONT);
        UIManager.put("OptionPane.buttonFont", FONT_BOLD);

        UIManager.put("Menu.background", HEADER_DARK);
        UIManager.put("Menu.foreground", HEADER_TEXT);
        UIManager.put("Menu.selectionBackground", PRIMARY);
        UIManager.put("Menu.selectionForeground", HEADER_TEXT);
        UIManager.put("Menu.font", FONT);
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuItem.selectionBackground", SELECTED);
        UIManager.put("MenuItem.selectionForeground", TEXT);
        UIManager.put("MenuItem.acceleratorForeground", MUTED);
        UIManager.put("MenuItem.acceleratorSelectionForeground", MUTED);
        UIManager.put("MenuItem.disabledForeground", new Color(0xA0AAB6));
        UIManager.put("MenuItem.font", FONT);
        UIManager.put("MenuItem.acceleratorFont", FONT_SMALL);
        UIManager.put("PopupMenu.background", Color.WHITE);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
    }
}
