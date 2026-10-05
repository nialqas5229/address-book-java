package addressbook.controller;

import addressbook.model.AddressBook;
import addressbook.model.AddressBookEntry;
import addressbook.model.InvalidEntryException;
import addressbook.storage.FileOperationException;
import addressbook.storage.FileSystem;
import addressbook.view.AddressBookView;
import addressbook.view.AddressBookView.SaveChoice;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Coordinates the GUI, the AddressBook and the FileSystem.
 *
 * - The GUI sends user actions here.
 * - The controller decides what must happen and in which order.
 * - AddressBook holds the data, FileSystem touches the disk.
 *
 * ERROR HANDLING: every checked exception (InvalidEntryException,
 * FileOperationException) is caught here and turned into a message for the
 * user through the view. When something fails, the address book is left
 * exactly as it was (no data is lost and nothing is marked "saved").
 */
public class AddressBookController {

    private static final String DEFAULT_EXTENSION = ".abook";

    private AddressBook addressBook;               // null = no address book is open
    private final AddressBookView gui;
    private final FileSystem fileSystem;

    public AddressBookController(AddressBookView gui, FileSystem fileSystem) {
        this.gui = gui;
        this.fileSystem = fileSystem;
    }

    /** Starts the program with a new, untitled address book. */
    public void start() {
        newAddressBook();
    }

    // ------------------------------------------------------------ New

    public void newAddressBook() {
        if (!confirmSaveIfNeeded()) {
            return;                                 // user pressed Cancel
        }
        addressBook = new AddressBook();
        refreshAll();
        gui.clearForm();
        gui.showStatus("New address book created.");
    }

    // ----------------------------------------------------------- Open

    /**
     * Opens an address book from disk. The current address book is closed
     * (after the "save changes?" question) but only replaced once the new file
     * was read successfully - a bad file never costs the user their open work.
     */
    public void openAddressBook() {
        if (!confirmSaveIfNeeded()) {
            return;
        }
        String path = gui.showOpenDialog();
        if (path == null) {
            return;                                 // dialog cancelled
        }
        try {
            List<AddressBookEntry> entries = fileSystem.readFile(path);
            AddressBook opened = new AddressBook();
            opened.setEntries(entries);
            opened.markSaved(path);
            addressBook = opened;
            refreshAll();
            gui.clearForm();
            gui.showStatus("Opened " + shortName(path) + " (" + entries.size() + " entries).");
        } catch (FileOperationException e) {
            gui.showError("Could not open the address book", e.getMessage());
        }
    }

    // ----------------------------------------------------------- Save

    /** Save. If there is no associated file yet it becomes "Save As". */
    public void save() {
        doSave();
    }

    /** Save As: asks for a file name, then stores the address book there. */
    public void saveAs() {
        doSaveAs();
    }

    private boolean doSave() {
        if (addressBook == null) {
            return false;
        }
        if (!addressBook.hasFile()) {
            return doSaveAs();                      // Save -> Save As
        }
        return writeTo(addressBook.getFileName());
    }

    private boolean doSaveAs() {
        if (addressBook == null) {
            return false;
        }
        String suggested = addressBook.hasFile()
                ? addressBook.getFileName()
                : AddressBook.UNTITLED + DEFAULT_EXTENSION;
        String path = gui.showSaveDialog(suggested);
        if (path == null) {
            return false;                           // dialog cancelled - nothing changes
        }
        path = ensureExtension(path);
        if (fileSystem.fileExists(path) && !gui.confirmOverwrite(path)) {
            return false;                           // user does not want to replace the file
        }
        return writeTo(path);
    }

    /** Writes the entries to the file; marks the address book saved only on success. */
    private boolean writeTo(String path) {
        try {
            fileSystem.writeFile(path, addressBook.getEntries());
            addressBook.markSaved(path);
            updateWindowTitle();
            gui.showStatus("Saved to " + shortName(path));
            return true;
        } catch (FileOperationException e) {
            gui.showError("Could not save the address book", e.getMessage());
            return false;                           // still marked as unsaved
        }
    }

    // ---------------------------------------------------------- Close

    /**
     * Closes the current address book. If it has unsaved changes the user is
     * asked first. Returns false if the user cancelled (book stays open).
     */
    public boolean closeAddressBook() {
        if (!confirmSaveIfNeeded()) {
            return false;
        }
        addressBook = null;
        gui.showNoAddressBook();
        updateWindowTitle();
        gui.clearForm();
        return true;
    }

    // ----------------------------------------------------------- Quit

    public void quit() {
        if (closeAddressBook()) {
            gui.close();
        }
    }

    // --------------------------------------------------------- Entries
    // (the GUI never touches AddressBook directly - it goes through here)

    /** Adds an entry. Returns true on success; otherwise the form shows the problem. */
    public boolean addEntry(String name, String phone, String email) {
        if (addressBook == null) {
            return false;
        }
        try {
            AddressBookEntry entry = new AddressBookEntry(name, phone, email);
            addressBook.addEntry(entry);
            refreshAll();
            gui.clearForm();
            gui.showStatus("Entry added: " + entry.getName());
            return true;
        } catch (InvalidEntryException e) {
            gui.showFormError(e);
            return false;
        }
    }

    /** Edits an entry. The old values are kept if the new ones are invalid. */
    public boolean editEntry(AddressBookEntry entry, String name, String phone, String email) {
        if (addressBook == null || entry == null) {
            return false;
        }
        try {
            entry.update(name, phone, email);
            addressBook.updateEntry(entry);
            refreshAll();
            gui.showStatus("Entry updated: " + entry.getName());
            return true;
        } catch (InvalidEntryException e) {
            gui.showFormError(e);
            return false;
        }
    }

    public boolean removeEntry(AddressBookEntry entry) {
        if (addressBook == null || entry == null) {
            return false;
        }
        if (addressBook.removeEntry(entry)) {
            refreshAll();
            gui.clearForm();
            gui.showStatus("Entry removed: " + entry.getName());
            return true;
        }
        return false;
    }

    // --------------------------------------------------------- Helpers

    /**
     * If the address book has unsaved changes, asks the user what to do.
     * Returns true if it is OK to continue (saved or discarded or nothing to
     * save) and false if the action must be cancelled.
     */
    private boolean confirmSaveIfNeeded() {
        if (addressBook == null || !addressBook.hasUnsavedChanges()) {
            return true;
        }
        SaveChoice choice = gui.askSaveChanges(addressBook.getFileName());
        switch (choice) {
            case SAVE:
                return doSave();                    // false if saving failed/cancelled
            case DISCARD:
                return true;
            default:
                return false;
        }
    }

    private void refreshAll() {
        if (addressBook != null) {
            gui.showEntries(addressBook.getEntries());
        }
        updateWindowTitle();
    }

    private void updateWindowTitle() {
        if (addressBook == null) {
            gui.updateTitle(null, false);
        } else {
            gui.updateTitle(addressBook.getFileName(), addressBook.hasUnsavedChanges());
        }
    }

    private static String ensureExtension(String path) {
        try {
            Path name = Paths.get(path).getFileName();
            if (name != null && !name.toString().contains(".")) {
                return path + DEFAULT_EXTENSION;
            }
        } catch (InvalidPathException ignored) {
            // the FileSystem will report the invalid name
        }
        return path;
    }

    private static String shortName(String path) {
        try {
            Path name = Paths.get(path).getFileName();
            return name == null ? path : name.toString();
        } catch (InvalidPathException e) {
            return path;
        }
    }
}
