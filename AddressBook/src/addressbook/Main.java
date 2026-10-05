package addressbook;

import addressbook.controller.AddressBookController;
import addressbook.storage.FileSystem;
import addressbook.view.AddressBookWindow;
import addressbook.view.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class Main {

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred:\n" + error,
                    "Unexpected error", JOptionPane.ERROR_MESSAGE));
        });

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception e) {
                // the default look and feel is fine as well
            }
            Theme.install();

            AddressBookWindow window = new AddressBookWindow();
            AddressBookController controller = new AddressBookController(window, new FileSystem());
            window.setController(controller);
            controller.start();
            window.show();
        });
    }
}
