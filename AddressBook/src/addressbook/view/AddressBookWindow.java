package addressbook.view;

import addressbook.controller.AddressBookController;
import addressbook.model.AddressBookEntry;
import addressbook.model.InvalidEntryException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The Swing window (the "GUI").
 *
 * It only shows things and passes user actions to the AddressBookController.
 * Every action runs inside {@link #safely(Runnable)}, so even an unexpected
 * error is shown to the user in a dialog instead of silently breaking the program.
 */
public class AddressBookWindow implements AddressBookView {

    private static final String EXTENSION = "abook";

    private final JFrame frame = new JFrame("Address Book");
    private AddressBookController controller;

    // header
    private final JLabel headerFile = new JLabel("Untitled");
    private final JLabel unsavedLabel = new JLabel(" ");

    // form
    private final RoundedTextField nameField = new RoundedTextField("Full name");
    private final RoundedTextField phoneField = new RoundedTextField("e.g. +92 300 1234567");
    private final RoundedTextField emailField = new RoundedTextField("name@example.com");
    private final JLabel formError = new JLabel(" ");
    private final RoundedButton addButton = new RoundedButton("Add", RoundedButton.Style.PRIMARY);
    private final RoundedButton updateButton = new RoundedButton("Update", RoundedButton.Style.PRIMARY);
    private final RoundedButton removeButton = new RoundedButton("Remove", RoundedButton.Style.DANGER);
    private final RoundedButton clearButton = new RoundedButton("Clear", RoundedButton.Style.NEUTRAL);

    // list
    private final DefaultListModel<AddressBookEntry> model = new DefaultListModel<>();
    private final JList<AddressBookEntry> list;
    private final JLabel countLabel = new JLabel("Entries (0)");
    private int hoverIndex = -1;
    private float selectionFade = 1f;
    private final Timer selectionTimer;

    private final StatusLabel status = new StatusLabel();
    private JMenuItem saveItem;
    private JMenuItem saveAsItem;
    private JMenuItem closeItem;

    private boolean bookOpen = false;
    private File lastFolder;

    public AddressBookWindow() {
        list = new JList<AddressBookEntry>(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (model.isEmpty()) {
                    paintPlaceholder((Graphics2D) g.create(), this);
                }
            }
        };
        selectionTimer = new Timer(15, e -> {
            selectionFade += 0.18f;
            if (selectionFade >= 1f) {
                selectionFade = 1f;
                ((Timer) e.getSource()).stop();
            }
            list.repaint();
        });
        buildWindow();
    }

    public void setController(AddressBookController controller) {
        this.controller = controller;
    }

    public void show() {
        frame.setVisible(true);
    }

    // =====================================================================
    //  Building the screen
    // =====================================================================

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                safely(() -> controller.quit());
            }
        });
        frame.getContentPane().setBackground(Theme.BACKGROUND);
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(buildMenuBar());
        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildBody(), BorderLayout.CENTER);
        frame.add(buildStatusBar(), BorderLayout.SOUTH);
        frame.setMinimumSize(new Dimension(840, 560));
        frame.setSize(980, 640);
        frame.setLocationRelativeTo(null);
        setFormEnabled(false);
        updateTitle(null, false);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.HEADER_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(true);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(menuItem("New", KeyEvent.VK_N, false, () -> controller.newAddressBook()));
        file.add(menuItem("Open...", KeyEvent.VK_O, false, () -> controller.openAddressBook()));
        file.addSeparator();
        saveItem = menuItem("Save", KeyEvent.VK_S, false, () -> controller.save());
        saveAsItem = menuItem("Save As...", KeyEvent.VK_S, true, () -> controller.saveAs());
        file.add(saveItem);
        file.add(saveAsItem);
        file.addSeparator();
        closeItem = menuItem("Close", KeyEvent.VK_W, false, () -> controller.closeAddressBook());
        file.add(closeItem);
        file.add(menuItem("Quit", KeyEvent.VK_Q, false, () -> controller.quit()));
        bar.add(file);
        return bar;
    }

    private JMenuItem menuItem(String text, int key, boolean shift, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        if (shift) {
            mask |= KeyEvent.SHIFT_DOWN_MASK;
        }
        item.setAccelerator(KeyStroke.getKeyStroke(key, mask));
        item.addActionListener(e -> safely(action));
        return item;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JLabel title = new JLabel("Address Book");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.HEADER_TEXT);
        header.add(title, BorderLayout.WEST);

        headerFile.setFont(Theme.FONT_HEADING);
        headerFile.setForeground(Theme.HEADER_TEXT);
        headerFile.setHorizontalAlignment(SwingConstants.RIGHT);
        unsavedLabel.setFont(Theme.FONT_SMALL_BOLD);
        unsavedLabel.setForeground(Theme.WARNING_ON_DARK);
        unsavedLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        right.add(headerFile);
        right.add(unsavedLabel);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(6, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        body.add(buildFormCard(), BorderLayout.WEST);
        body.add(buildListCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildFormCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        int pad = RoundedPanel.SHADOW + 18;
        card.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        card.setPreferredSize(new Dimension(350, 400));

        JLabel heading = new JLabel("Entry details");
        heading.setFont(Theme.FONT_HEADING);
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(14));

        addLabeled(card, "Name", nameField);
        addLabeled(card, "Phone", phoneField);
        addLabeled(card, "Email", emailField);

        formError.setFont(Theme.FONT_SMALL_BOLD);
        formError.setForeground(Theme.ERROR);
        formError.setAlignmentX(Component.LEFT_ALIGNMENT);
        formError.setPreferredSize(new Dimension(280, 34));
        formError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        formError.setVerticalAlignment(SwingConstants.TOP);
        card.add(formError);
        card.add(Box.createVerticalStrut(6));

        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(removeButton);
        buttons.add(clearButton);
        card.add(buttons);
        card.add(Box.createVerticalGlue());

        addButton.addActionListener(e -> safely(this::onAdd));
        updateButton.addActionListener(e -> safely(this::onUpdate));
        removeButton.addActionListener(e -> safely(this::onRemove));
        clearButton.addActionListener(e -> clearForm());

        // Enter in a field = Add (or Update when an entry is selected)
        for (RoundedTextField f : new RoundedTextField[] {nameField, phoneField, emailField}) {
            f.addActionListener(e -> safely(() -> {
                if (list.getSelectedValue() == null) {
                    onAdd();
                } else {
                    onUpdate();
                }
            }));
            f.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { clearFormError(); }
                @Override public void removeUpdate(DocumentEvent e) { clearFormError(); }
                @Override public void changedUpdate(DocumentEvent e) { clearFormError(); }
            });
        }
        return card;
    }

    private void addLabeled(JPanel card, String label, RoundedTextField field) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL_BOLD);
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(l);
        card.add(Box.createVerticalStrut(4));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(field);
        card.add(Box.createVerticalStrut(12));
    }

    private JPanel buildListCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 8));
        int pad = RoundedPanel.SHADOW + 14;
        card.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        countLabel.setFont(Theme.FONT_HEADING);
        countLabel.setForeground(Theme.TEXT);
        card.add(countLabel, BorderLayout.NORTH);

        list.setOpaque(false);
        list.setFixedCellHeight(58);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new EntryRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        list.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                boolean inside = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());
                int newHover = inside ? index : -1;
                if (newHover != hoverIndex) {
                    hoverIndex = newHover;
                    list.setCursor(Cursor.getPredefinedCursor(inside ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    list.repaint();
                }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                list.repaint();
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 24, 8, 24));
        bar.add(status, BorderLayout.WEST);
        bar.setPreferredSize(new Dimension(100, 34));
        return bar;
    }

    /** Hint text drawn in the middle of an empty list. */
    private void paintPlaceholder(Graphics2D g2, JList<?> l) {
        Theme.smooth(g2);
        String line1 = bookOpen ? "No entries yet" : "No address book is open";
        String line2 = bookOpen ? "Fill in the form on the left and press Add."
                                : "Use File > New or File > Open to start.";
        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.FONT_HEADING);
        FontMetrics fm = g2.getFontMetrics();
        int y = l.getHeight() / 2 - 8;
        g2.drawString(line1, (l.getWidth() - fm.stringWidth(line1)) / 2, y);
        g2.setFont(Theme.FONT);
        fm = g2.getFontMetrics();
        g2.drawString(line2, (l.getWidth() - fm.stringWidth(line2)) / 2, y + 26);
        g2.dispose();
    }

    // =====================================================================
    //  User actions (button clicks) -> controller
    // =====================================================================

    private void onAdd() {
        controller.addEntry(nameField.getText(), phoneField.getText(), emailField.getText());
    }

    private void onUpdate() {
        AddressBookEntry selected = list.getSelectedValue();
        if (selected != null) {
            controller.editEntry(selected, nameField.getText(), phoneField.getText(), emailField.getText());
        }
    }

    private void onRemove() {
        AddressBookEntry selected = list.getSelectedValue();
        if (selected == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(frame,
                "Remove \"" + selected.getName() + "\" from the address book?",
                "Remove entry", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            controller.removeEntry(selected);
        }
    }

    private void onSelectionChanged() {
        AddressBookEntry selected = list.getSelectedValue();
        if (selected != null) {
            nameField.setText(selected.getName());
            phoneField.setText(selected.getPhone());
            emailField.setText(selected.getEmail());
            clearFormError();
        }
        selectionFade = 0f;                         // start the soft fade-in
        selectionTimer.restart();
        updateButtons();
    }

    /**
     * Runs a user action. If anything unexpected goes wrong the user sees a
     * dialog and the program keeps running.
     */
    private void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            showError("Unexpected error", "Something went wrong, but your data is still open.\n\n"
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    // =====================================================================
    //  AddressBookView (called by the controller)
    // =====================================================================

    @Override
    public String showSaveDialog(String suggestedName) {
        JFileChooser chooser = newChooser("Save Address Book As");
        if (suggestedName != null) {
            File suggested = new File(suggestedName);
            if (suggested.isAbsolute()) {
                chooser.setSelectedFile(suggested);
            } else {
                chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedName));
            }
        }
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            lastFolder = chosen.getParentFile();
            return chosen.getAbsolutePath();
        }
        return null;
    }

    @Override
    public String showOpenDialog() {
        JFileChooser chooser = newChooser("Open Address Book");
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            lastFolder = chosen.getParentFile();
            return chosen.getAbsolutePath();
        }
        return null;
    }

    private JFileChooser newChooser(String title) {
        JFileChooser chooser = new JFileChooser(lastFolder);
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Address books (*." + EXTENSION + ")", EXTENSION));
        return chooser;
    }

    @Override
    public SaveChoice askSaveChanges(String fileName) {
        String[] options = {"Save", "Don't Save", "Cancel"};
        int answer = JOptionPane.showOptionDialog(frame,
                "Do you want to save the changes to \"" + displayName(fileName) + "\"?\n"
                        + "Your changes will be lost if you do not save them.",
                "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        switch (answer) {
            case 0:  return SaveChoice.SAVE;
            case 1:  return SaveChoice.DISCARD;
            default: return SaveChoice.CANCEL;      // Cancel button or window closed
        }
    }

    @Override
    public boolean confirmOverwrite(String path) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "\"" + displayName(path) + "\" already exists.\nDo you want to replace it?",
                "Replace file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }

    @Override
    public void updateTitle(String fileName, boolean unsavedChanges) {
        if (fileName == null) {
            frame.setTitle("Address Book");
            headerFile.setText("No address book open");
            headerFile.setToolTipText(null);
            unsavedLabel.setText(" ");
            return;
        }
        String name = displayName(fileName);
        frame.setTitle(name + (unsavedChanges ? " *" : "") + " - Address Book");
        headerFile.setText(name);
        headerFile.setToolTipText(fileName);
        unsavedLabel.setText(unsavedChanges ? "● Unsaved changes" : " ");
    }

    @Override
    public void showEntries(List<AddressBookEntry> entries) {
        AddressBookEntry selected = list.getSelectedValue();
        model.clear();
        for (AddressBookEntry e : entries) {
            model.addElement(e);
        }
        bookOpen = true;
        if (selected != null && entries.contains(selected)) {
            list.setSelectedValue(selected, true);
        }
        countLabel.setText("Entries (" + entries.size() + ")");
        setFormEnabled(true);
        list.repaint();
    }

    @Override
    public void showNoAddressBook() {
        model.clear();
        bookOpen = false;
        countLabel.setText("Entries (0)");
        setFormEnabled(false);
        list.repaint();
    }

    @Override
    public void showStatus(String message) {
        status.show(message, false);
    }

    @Override
    public void showError(String title, String message) {
        status.show(title, true);
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showFormError(InvalidEntryException error) {
        formError.setText("<html>" + escapeHtml(error.getMessage()) + "</html>");
        RoundedTextField field;
        switch (error.getField()) {
            case PHONE: field = phoneField; break;
            case EMAIL: field = emailField; break;
            default:    field = nameField;
        }
        field.setError(true);
        field.requestFocusInWindow();
    }

    @Override
    public void clearForm() {
        list.clearSelection();
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        clearFormError();
        updateButtons();
    }

    @Override
    public void close() {
        frame.dispose();
        System.exit(0);
    }

    // =====================================================================
    //  Small helpers
    // =====================================================================

    private void clearFormError() {
        formError.setText(" ");
        nameField.setError(false);
        phoneField.setError(false);
        emailField.setError(false);
    }

    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        list.setEnabled(enabled);
        saveItem.setEnabled(enabled);
        saveAsItem.setEnabled(enabled);
        closeItem.setEnabled(enabled);
        updateButtons();
    }

    private void updateButtons() {
        boolean selected = bookOpen && list.getSelectedValue() != null;
        addButton.setEnabled(bookOpen);
        clearButton.setEnabled(bookOpen);
        updateButton.setEnabled(selected);
        removeButton.setEnabled(selected);
    }

    /** Only the file name (not the whole path) is shown to the user. */
    private static String displayName(String path) {
        try {
            Path name = Paths.get(path).getFileName();
            return name == null ? path : name.toString();
        } catch (InvalidPathException e) {
            return path;
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================================
    //  One row of the entry list
    // =====================================================================

    private class EntryRenderer extends JPanel implements javax.swing.ListCellRenderer<AddressBookEntry> {
        private AddressBookEntry entry;
        private boolean isSelected;
        private boolean isHover;

        EntryRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AddressBookEntry> l,
                AddressBookEntry value, int index, boolean selected, boolean focus) {
            this.entry = value;
            this.isSelected = selected;
            this.isHover = index == hoverIndex;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.smooth(g2);
            int w = getWidth();
            int h = getHeight();

            if (isSelected) {
                g2.setColor(Theme.blend(Theme.HOVER_ROW, Theme.SELECTED, selectionFade));
                g2.fillRoundRect(2, 3, w - 4, h - 6, 14, 14);
            } else if (isHover) {
                g2.setColor(Theme.HOVER_ROW);
                g2.fillRoundRect(2, 3, w - 4, h - 6, 14, 14);
            }
            g2.setFont(Theme.FONT_HEADING);
            g2.setColor(Theme.TEXT);
            g2.drawString(entry.getName(), 16, 24);

            String details = (entry.getPhone().isEmpty() ? "no phone" : entry.getPhone())
                    + "   |   " + (entry.getEmail().isEmpty() ? "no email" : entry.getEmail());
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.MUTED);
            g2.drawString(details, 16, 44);
            g2.dispose();
        }
    }
}
