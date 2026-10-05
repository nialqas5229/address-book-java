# Address Book (Java Swing)

A simple Address Book desktop app built for the Software Construction and
Development course (UET Taxila). It stores contacts in a file and uses a
clean separation between GUI, controller and file storage.

## Features
- New, Open, Save, Save As, Close and Quit
- Add, edit and remove contacts (name, phone, email)
- Tracks unsaved changes and asks before closing
- Shows the file name in the window title ("Untitled" for new books)
- Clear error messages for invalid input and file problems
- Rounded modern interface with hover animations

## Requirements
- Java 11 or newer (JDK)

## How to run
1. Clone the project:
   git clone https://github.com/YOUR-USERNAME/address-book-java.git
   cd address-book-java
2. Build and run:
   - Windows: build.bat, then run.bat
   - Linux / macOS: ./build.sh, then ./run.sh

Or by hand:
   javac -d out $(find src -name "*.java")
   java -cp out addressbook.Main

## Run the tests
   ./run_tests.sh

## Project structure
- model: AddressBook, AddressBookEntry
- storage: FileSystem
- controller: AddressBookController
- view: Swing window and custom rounded components

## Screenshots
![Entries](docs/screenshot_entries.png)
