package quantum.quantum;

import quantum.quantum.ui.MainWindow;

import javax.swing.*;

/**
 * Application entry point.
 *
 * Runs the BB84 + E91 Quantum Key Distribution Simulator GUI.
 * For headless testing, run BB84Test instead.
 */
public class Main {

    public static void main(String[] args) {
        // Run tests if --test flag provided
        if (args.length > 0 && args[0].equals("--test")) {
            BB84Test.main(new String[]{});
            return;
        }

        // Launch Swing UI
        SwingUtilities.invokeLater(() -> {
            try {
                // Nimbus look-and-feel for better dark theme support
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        // Dark overrides
                        UIManager.put("nimbusBase",            new java.awt.Color(18, 22, 36));
                        UIManager.put("nimbusBlueGrey",        new java.awt.Color(30, 38, 60));
                        UIManager.put("control",               new java.awt.Color(20, 24, 36));
                        UIManager.put("text",                  new java.awt.Color(200, 210, 230));
                        UIManager.put("nimbusFocus",           new java.awt.Color(86, 182, 254));
                        break;
                    }
                }
            } catch (Exception e) {
                // Fall back to system L&F silently
            }
            MainWindow win = new MainWindow();
            win.setVisible(true);
        });
    }
}