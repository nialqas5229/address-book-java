package addressbook.model;

import java.util.regex.Pattern;

/**
 * One contact in the address book (name, phone, e-mail).
 * The data is validated every time an entry is created or changed, so an
 * invalid entry can never exist inside the system.
 */
public class AddressBookEntry {

    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_EMAIL_LENGTH = 100;

    private static final Pattern PHONE_CHARACTERS = Pattern.compile("[+]?[0-9 ()\\-]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}");

    private String name;
    private String phone;
    private String email;

    public AddressBookEntry(String name, String phone, String email) throws InvalidEntryException {
        String[] clean = validate(name, phone, email);
        this.name = clean[0];
        this.phone = clean[1];
        this.email = clean[2];
    }

    /**
     * Changes the entry. If the new data is invalid, an exception is thrown
     * BEFORE anything is changed, so the old values stay safe.
     */
    public void update(String name, String phone, String email) throws InvalidEntryException {
        String[] clean = validate(name, phone, email);
        this.name = clean[0];
        this.phone = clean[1];
        this.email = clean[2];
    }

    /** Checks the data and returns the cleaned (trimmed) values {name, phone, email}. */
    public static String[] validate(String name, String phone, String email)
            throws InvalidEntryException {
        String n = clean(name);
        String p = clean(phone);
        String e = clean(email);

        if (n.isEmpty()) {
            throw new InvalidEntryException(InvalidEntryException.Field.NAME, "Name is required.");
        }
        if (n.length() > MAX_NAME_LENGTH) {
            throw new InvalidEntryException(InvalidEntryException.Field.NAME,
                    "Name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!p.isEmpty()) {
            if (!PHONE_CHARACTERS.matcher(p).matches()) {
                throw new InvalidEntryException(InvalidEntryException.Field.PHONE,
                        "Phone may contain only digits, spaces, + ( ) and -.");
            }
            int digits = 0;
            for (char c : p.toCharArray()) {
                if (Character.isDigit(c)) {
                    digits++;
                }
            }
            if (digits < 7 || digits > 15) {
                throw new InvalidEntryException(InvalidEntryException.Field.PHONE,
                        "Phone number must have between 7 and 15 digits.");
            }
        }
        if (!e.isEmpty()) {
            if (e.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(e).matches()) {
                throw new InvalidEntryException(InvalidEntryException.Field.EMAIL,
                        "Email is not valid (example: name@example.com).");
            }
        }
        return new String[] {n, p, e};
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name;
    }
}
