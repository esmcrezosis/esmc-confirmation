package com.esmc.client.confirmation.main;

import com.esmc.client.confirmation.classes.MainPanel;
import com.esmc.client.confirmation.utils.LibraryManager;
import com.esmc.client.confirmation.utils.LicenseManager;
import com.neurotec.lang.NCore;


import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
//import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

/**
 *
 * @author ESMC
 */
public class EsmcConfirmation implements PropertyChangeListener {

	// ===========================================================
    // Private static final fields
    // ===========================================================
    private static final Set<String> LICENSES;

    // ===========================================================
    // Static constructor
    // ===========================================================
    static {
        LICENSES = new HashSet<>(1);
        LICENSES.add("Biometrics.FingerExtraction");
        LICENSES.add("Biometrics.FingerSegmentation");
        LICENSES.add("Biometrics.Tools.NFIQ"); // Optional.
        //LICENSES.add("Devices.Cameras"); // Optional.
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LibraryManager.initLibraryPath();
        EsmcConfirmation sample = new EsmcConfirmation();
       // LicenseManager.getInstance().addPropertyChangeListener(sample);
        /*try {
            LicenseManager.getInstance().addLicenses();
            LicenseManager.getInstance().obtain(LICENSES);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.toString());
            return;
        }*/


		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
                JFrame frame = new JFrame();
				frame.setTitle("IDENTIFICATION BIOMETRIQUE ESMC");
                //frame.setIconImage(Utils.createIconImage("images/Logo16x16.png"));
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						NCore.shutdown();
					}
				});
                frame.add(new MainPanel(), BorderLayout.CENTER);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
    }
   
 // ===========================================================
    // Event handling
    // ===========================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
       /* if (LicenseManager.PROGRESS_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("# of analyzed licenses: %d\n", progress);
            progressMonitor.setNote(message);
        }*/
    }
}
