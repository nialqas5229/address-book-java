package addressbook.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The address book that is currently open (the model).
 *
 * It stores the entries and its own state: the associated file name and
 * whether there are unsaved changes. It does NOT read/write files, show
 * windows or ask the user anything - that is done by FileSystem, the GUI
 * and the AddressBookController.
 */
public class AddressBook {

    /** Name used for an address book that has not been saved yet. */
    public static final String UNTITLED = "Untitled";

    private final List<AddressBookEntry> entries = new ArrayList<>();
    private String fileName = UNTITLED;
    private boolean hasFile = false;
    private boolean modified = false;

    // ---------------------------------------------------------- entries

    public void addEntry(AddressBookEntry entry) {
        Objects.requireNonNull(entry, "entry");
        entries.add(entry);
        modified = true;
    }

    /**
     * Tells the address book that an entry it already holds was edited.
     * (The entry validates and changes itself; here we record the change.)
     */
    public void updateEntry(AddressBookEntry entry) {
        if (!containsEntry(entry)) {
            throw new IllegalArgumentException("This entry is not part of the address book.");
        }
        modified = true;
    }

    /** Removes the entry. Returns true if it was found. */
    public boolean removeEntry(AddressBookEntry entry) {
        for (Iterator<AddressBookEntry> it = entries.iterator(); it.hasNext(); ) {
            if (it.next() == entry) {
                it.remove();
                modified = true;
                return true;
            }
        }
        return false;
    }

    /** Read-only view of the entries. */
    public List<AddressBookEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Used after loading from a file: replaces all entries, no unsaved changes. */
    public void setEntries(List<AddressBookEntry> newEntries) {
        Objects.requireNonNull(newEntries, "newEntries");
        entries.clear();
        entries.addAll(newEntries);
        modified = false;
    }

    private boolean containsEntry(AddressBookEntry entry) {
        for (AddressBookEntry e : entries) {
            if (e == entry) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------- state / file

    public boolean hasUnsavedChanges() {
        return modified;
    }

    /** The associated file (full path) or "Untitled" if there is none yet. */
    public String getFileName() {
        return fileName;
    }

    /** True if the address book has been opened from / saved to a real file. */
    public boolean hasFile() {
        return hasFile;
    }

    /** Called after a successful save: remember the file and clear the unsaved flag. */
    public void markSaved(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("A file name is required.");
        }
        this.fileName = fileName;
        this.hasFile = true;
        this.modified = false;
    }
}
