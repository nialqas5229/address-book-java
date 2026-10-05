package addressbook.view;

import addressbook.model.AddressBookEntry;
import addressbook.model.InvalidEntryException;

import java.util.List;

/**
 * Everything the controller needs from the GUI.
 * The controller only knows this interface, never Swing classes, which keeps
 * the controller independent from the screen (and easy to test).
 */
public interface AddressBookView {

    /** The user's answer to "Do you want to save changes?". */
    enum SaveChoice { SAVE, DISCARD, CANCEL }

    /** Shows a "Save As" dialog. Returns the chosen path or null if cancelled. */
    String showSaveDialog(String suggestedName);

    /** Shows an "Open" dialog. Returns the chosen path or null if cancelled. */
    String showOpenDialog();

    /** Asks whether unsaved changes should be saved. */
    SaveChoice askSaveChanges(String fileName);

    /** Asks whether an existing file may be replaced. */
    boolean confirmOverwrite(String path);

    /** Updates the window title with the file name (null = no address book open). */
    void updateTitle(String fileName, boolean unsavedChanges);

    /** Shows the entries of the (now open) address book. */
    void showEntries(List<AddressBookEntry> entries);

    /** Shows the "nothing is open" state. */
    void showNoAddressBook();

    /** Shows a short, self-fading status message. */
    void showStatus(String message);

    /** Shows an error dialog (file problems, unexpected errors). */
    void showError(String title, String message);

    /** Shows a validation error next to the entry form and marks the wrong field. */
    void showFormError(InvalidEntryException error);

    /** Empties the entry form. */
    void clearForm();

    /** Closes the window and ends the program. */
    void close();
}
