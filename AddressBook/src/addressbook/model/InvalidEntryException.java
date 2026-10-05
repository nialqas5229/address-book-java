package addressbook.model;

/**
 * Thrown when the data typed for an address book entry is not valid
 * (for example: empty name or a badly formed e-mail address).
 * It remembers WHICH field is wrong so the GUI can highlight that field.
 */
public class InvalidEntryException extends Exception {

    /** The form field that caused the problem. */
    public enum Field { NAME, PHONE, EMAIL }

    private final Field field;

    public InvalidEntryException(Field field, String message) {
        super(message);
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
