package addressbook.storage;

/**
 * Thrown when reading or writing an address book file fails.
 * The message is written in plain language so it can be shown directly
 * to the user in an error dialog.
 */
public class FileOperationException extends Exception {

    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
