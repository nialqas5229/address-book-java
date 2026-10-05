package addressbook.storage;

import addressbook.model.AddressBookEntry;
import addressbook.model.InvalidEntryException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all disk access for the address book (persistent storage).
 *
 * File format (plain UTF-8 text):
 * <pre>
 * ADDRESSBOOK v1
 * name TAB phone TAB email
 * name TAB phone TAB email
 * </pre>
 * Tabs, new lines and backslashes inside a value are escaped (\t \n \r \\).
 *
 * Every problem is reported as a FileOperationException with a clear message.
 */
public class FileSystem {

    private static final String HEADER = "ADDRESSBOOK v1";
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10 MB safety limit

    // ------------------------------------------------------------ exists

    public boolean fileExists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (InvalidPathException | SecurityException e) {
            return false;
        }
    }

    // -------------------------------------------------------------- read

    /**
     * Reads an address book file and returns its entries.
     *
     * @throws FileOperationException if the file is missing, unreadable or damaged
     */
    public List<AddressBookEntry> readFile(String path) throws FileOperationException {
        Path file = toPath(path);
        try {
            if (!Files.exists(file)) {
                throw new FileOperationException("The file was not found:\n" + file);
            }
            if (Files.isDirectory(file)) {
                throw new FileOperationException("This is a folder, not an address book file:\n" + file);
            }
            if (!Files.isReadable(file)) {
                throw new FileOperationException("You do not have permission to read this file:\n" + file);
            }
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw new FileOperationException("The file is too large to be an address book:\n" + file);
            }

            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return parse(lines);

        } catch (java.nio.charset.CharacterCodingException e) {
            throw new FileOperationException(
                    "This file is not a valid address book (it is not readable text):\n" + file, e);
        } catch (IOException e) {
            throw new FileOperationException("The file could not be read:\n" + describe(e), e);
        } catch (SecurityException e) {
            throw new FileOperationException("Access to the file was blocked:\n" + file, e);
        }
    }

    private List<AddressBookEntry> parse(List<String> lines) throws FileOperationException {
        if (lines.isEmpty() || !stripBom(lines.get(0)).trim().equals(HEADER)) {
            throw new FileOperationException(
                    "This file is not an address book file (the header \"" + HEADER + "\" is missing).");
        }
        List<AddressBookEntry> entries = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                continue;                           // truly empty lines are allowed
            }
            int lineNumber = i + 1;
            String[] parts = line.split("\t", -1);
            if (parts.length != 3) {
                throw new FileOperationException("The file is damaged: line " + lineNumber
                        + " should have 3 fields (name, phone, email) but has " + parts.length + ".");
            }
            try {
                entries.add(new AddressBookEntry(unescape(parts[0], lineNumber),
                        unescape(parts[1], lineNumber), unescape(parts[2], lineNumber)));
            } catch (InvalidEntryException e) {
                throw new FileOperationException(
                        "The file is damaged: line " + lineNumber + " - " + e.getMessage(), e);
            }
        }
        return entries;
    }

    // ------------------------------------------------------------- write

    /**
     * Writes the entries to the file.
     * The data is first written to a temporary file and then moved over the
     * target, so a failure in the middle can never destroy an existing file.
     *
     * @return true when the file was written successfully
     * @throws FileOperationException if the file could not be written
     */
    public boolean writeFile(String path, List<AddressBookEntry> entries)
            throws FileOperationException {
        Path target = toPath(path);
        Path folder = target.getParent();
        Path temp = null;
        try {
            if (Files.isDirectory(target)) {
                throw new FileOperationException("A folder with this name already exists:\n" + target);
            }
            if (folder == null || !Files.isDirectory(folder)) {
                throw new FileOperationException("The folder does not exist:\n" + folder);
            }
            if (!Files.isWritable(folder)) {
                throw new FileOperationException("You do not have permission to save in this folder:\n" + folder);
            }
            if (Files.exists(target) && !Files.isWritable(target)) {
                throw new FileOperationException("The file is read-only and cannot be replaced:\n" + target);
            }

            StringBuilder text = new StringBuilder(HEADER).append('\n');
            for (AddressBookEntry e : entries) {
                text.append(escape(e.getName())).append('\t')
                    .append(escape(e.getPhone())).append('\t')
                    .append(escape(e.getEmail())).append('\n');
            }

            temp = Files.createTempFile(folder, ".addressbook-", ".tmp");
            Files.write(temp, text.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;

        } catch (IOException e) {
            throw new FileOperationException("The file could not be saved:\n" + describe(e), e);
        } catch (SecurityException e) {
            throw new FileOperationException("Saving was blocked by the system:\n" + target, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // nothing more we can do for a leftover temp file
                }
            }
        }
    }

    // ----------------------------------------------------------- helpers

    private Path toPath(String path) throws FileOperationException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileOperationException("No file name was given.");
        }
        try {
            return Paths.get(path).toAbsolutePath();
        } catch (InvalidPathException e) {
            throw new FileOperationException("The file name is not valid:\n" + path, e);
        }
    }

    /** Turns low-level I/O exceptions into a short, understandable sentence. */
    private String describe(IOException e) {
        if (e instanceof AccessDeniedException) {
            return "Permission denied: " + e.getMessage();
        }
        if (e instanceof NoSuchFileException) {
            return "File or folder not found: " + e.getMessage();
        }
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof FileSystemException && message.toLowerCase().contains("no space")) {
            return "The disk is full.";
        }
        return message;
    }

    private static String stripBom(String s) {
        return !s.isEmpty() && s.charAt(0) == '﻿' ? s.substring(1) : s;
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String value, int lineNumber) throws FileOperationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= value.length()) {
                throw new FileOperationException("The file is damaged: line " + lineNumber
                        + " ends with an incomplete escape code.");
            }
            char next = value.charAt(++i);
            switch (next) {
                case '\\': sb.append('\\'); break;
                case 't': sb.append('\t'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                default:
                    throw new FileOperationException("The file is damaged: line " + lineNumber
                            + " contains an unknown escape code (\\" + next + ").");
            }
        }
        return sb.toString();
    }
}
