package com.esmc.client.confirmation.classes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;

import com.esmc.client.confirmation.dto.Confirmation;
import com.esmc.client.confirmation.dto.RestClient;
import com.esmc.client.confirmation.dto.Result;
import com.esmc.client.confirmation.dto.SerializationTools;
import com.esmc.client.confirmation.utils.QrCapture;
import com.esmc.client.confirmation.utils.Utils;
import com.neurotec.biometrics.NBiometricCaptureOption;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.devices.NFingerScanner;
import com.neurotec.images.NImages;
import com.neurotec.io.NBuffer;
import com.neurotec.io.NFile;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.concurrent.CompletionHandler;

public final class EnrollFromScanner extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================
	private final String nomFichierTemplate = "esmctemp.data";
	private NSubject subjectCurrent;
	private NSubject subjectFromFile;
	private final List<NSubject> subjects;

	private final NDeviceManager deviceManager;
	private boolean scanning;
	private boolean priseEmpreinte;
	private final CaptureCompletionHandler captureCompletionHandler = new CaptureCompletionHandler();
	private final VerificationHandler verificationHandler = new VerificationHandler();
	private final EnrollHandler enrollHandler = new EnrollHandler();
	private final IdentificationHandler identificationHandler = new IdentificationHandler();

	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();
	// private ImageThumbnailFileChooser fileChooser;
	private JFileChooser fileChooserTemplateFile;
	private File oldTemplateFile;

	private NFingerView view;
	private JFileChooser fcImage;
	private JFileChooser fcTemplate;
	private File oldImageFile;

	private JButton btnScan;
	// private JButton btnCancel;
	private JButton btnVerify;
	private JButton btnRefresh;
	private JButton btnSaveImage;
	private JButton btnSaveTemplate;
	private JButton btnQrCodem;
	private JButton btnOpen;
	// private JButton btnQrCodef;
	private JButton btnAnnuler;

	private JCheckBox cbAutomatic;
	private JCheckBox cbShowBinarized;
	private JCheckBox cbProcuration;
	private JLabel lblCodemembre;
	private JLabel lblInfo;
	private JLabel lblCount;
	private JPanel panelButtons;
	private JPanel panelInfo;
	private JPanel panelMain;
	private JPanel panelSave;
	private JPanel panelScanners;
	private JPanel panelSouth;
	private JList<NFingerScanner> scannerList;
	private JScrollPane scrollPane;
	private JScrollPane scrollPaneList;

	// private String codeMembreFournisseur="";
	private String codeMembre = "";
	private Confirmation confirmation;
	public int choixReseau;
	String[] buttons = { "Intranet", "Internet" };
	private int procuration = 0;
	private int statutReponse = 0;
	// ===========================================================
	// Public constructor
	// ===========================================================

	public EnrollFromScanner() {
		super();
		subjects = new ArrayList<>();
		requiredLicenses = new ArrayList<String>();
		// requiredLicenses.add("Devices.FingerScanners");
		requiredLicenses.add("Biometrics.FingerExtraction");

		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");

		FingersTools.getInstance().getClient().setUseDeviceManager(true);
		deviceManager = FingersTools.getInstance().getClient().getDeviceManager();
		deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));
		deviceManager.initialize();
		// System.out.println("current device= "+deviceManager.getDevices().get(0));

		confirmation = new Confirmation();

		choixReseau = JOptionPane.showOptionDialog(null, "Quel réseau utilisez-vous ?", "Confirmation",
				JOptionPane.WARNING_MESSAGE, 0, null, buttons, buttons[1]);

	}
	// ===========================================================
	// Private methods
	// ===========================================================

	private void startCapturing() throws IOException {
			lblInfo.setText("");
			if (FingersTools.getInstance().getClient().getFingerScanner() == null) {
				JOptionPane.showMessageDialog(this, "Please select scanner from the list.", "No scanner selected",
						JOptionPane.PLAIN_MESSAGE);
				return;
			}

			// Create a finger.
			NFinger finger = new NFinger();
			// Set Manual capturing mode if automatic isn't selected.
			if (!cbAutomatic.isSelected()) {
				finger.setCaptureOptions(EnumSet.of(NBiometricCaptureOption.MANUAL));
			}

			// Add finger to subject and finger view.
			subjectCurrent = new NSubject();
			subjectCurrent.getFingers().add(finger);
			view.setFinger(finger);
			view.setShownImage(ShownImage.ORIGINAL);

			// Begin capturing.
			NBiometricTask task = FingersTools.getInstance().getClient().createTask(
					EnumSet.of(NBiometricOperation.CAPTURE, NBiometricOperation.CREATE_TEMPLATE), subjectCurrent);
			FingersTools.getInstance().getClient().performTask(task, null, captureCompletionHandler);

			scanning = true;
			priseEmpreinte = true;
			updateControls();
	}

	private void saveTemplate() throws IOException {
		if (subjectCurrent != null) {
			if (oldTemplateFile != null) {
				fcTemplate.setSelectedFile(oldTemplateFile);
			}
			if (fcTemplate.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				oldTemplateFile = fcTemplate.getSelectedFile();
				NFile.writeAllBytes(fcTemplate.getSelectedFile().getAbsolutePath(), subjectCurrent.getTemplateBuffer());

			}
		}
	}

	private void saveImage() throws IOException {
		if (subjectCurrent != null) {
			if (oldImageFile != null) {
				fcImage.setSelectedFile(oldImageFile);
			}
			if (fcImage.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				oldImageFile = fcImage.getSelectedFile();
				if (cbShowBinarized.isSelected()) {
					subjectCurrent.getFingers().get(0).getBinarizedImage()
							.save(fcImage.getSelectedFile().getAbsolutePath());
				} else {
					subjectCurrent.getFingers().get(0).getImage().save(fcImage.getSelectedFile().getAbsolutePath());
				}
			}
		}
	}

	private void updateShownImage() {
		if (cbShowBinarized.isSelected()) {
			view.setShownImage(ShownImage.RESULT);
		} else {
			view.setShownImage(ShownImage.ORIGINAL);
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	void updateStatus(String status) {
		lblInfo.setText(status);
	}

	NSubject getSubject() {
		return subjectCurrent;
	}

	NFingerScanner getSelectedScanner() {
		return scannerList.getSelectedValue();
	}

	void setSubject(NSubject subject) {
		this.subjectCurrent = subject;
	}

	List<NSubject> getSubjects() {
		return subjects;
	}

	/*
	 * void appendIdentifyResult(String name, int score) { ((DefaultTableModel)
	 * tableResults.getModel()).addRow(new Object[] {name, score}); }
	 * 
	 * void prependIdentifyResult(String name, int score) { ((DefaultTableModel)
	 * tableResults.getModel()).insertRow(0, new Object[] {name, score}); }
	 */

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		setLayout(new BorderLayout());

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		panelMain = new JPanel();
		panelMain.setLayout(new BorderLayout());
		add(panelMain, BorderLayout.CENTER);
		{
			panelScanners = new JPanel();
			panelScanners.setBorder(BorderFactory.createTitledBorder("Scanners list"));
			panelScanners.setLayout(new BorderLayout());
			panelMain.add(panelScanners, BorderLayout.NORTH);
			{
				scrollPaneList = new JScrollPane();
				scrollPaneList.setPreferredSize(new Dimension(0, 90));
				panelScanners.add(scrollPaneList, BorderLayout.CENTER);
				{
					scannerList = new JList<NFingerScanner>();
					scannerList.setModel(new DefaultListModel<NFingerScanner>());
					scannerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					scannerList.setBorder(LineBorder.createBlackLineBorder());
					scannerList.addListSelectionListener(new ScannerSelectionListener());
					scrollPaneList.setViewportView(scannerList);

				}
			}
			{
				panelButtons = new JPanel();
				panelButtons.setLayout(new FlowLayout(FlowLayout.LEADING));
				panelScanners.add(panelButtons, BorderLayout.SOUTH);
				{
					btnRefresh = new JButton();
					btnRefresh.setText("Rafraichir");
					btnRefresh.addActionListener(this);
					panelButtons.add(btnRefresh);
				}
				/*
				 * { btnQrCodef = new JButton(); btnQrCodef.setText("QR CODE FOURNISSEUR");
				 * btnQrCodef.addActionListener(this); panelButtons.add(btnQrCodef); }
				 */
				{
					btnQrCodem = new JButton();
					btnQrCodem.setText("QR CODE");
					btnQrCodem.addActionListener(this);
					panelButtons.add(btnQrCodem);
				}
				{
					cbProcuration = new JCheckBox();
					cbProcuration.setSelected(false);
					cbProcuration.setText("Procuration?");
					cbProcuration.addActionListener(this);
					panelButtons.add(cbProcuration);
				}
				{
					btnScan = new JButton();
					btnScan.setText("SCANNER");
					btnScan.setEnabled(true);
					btnScan.addActionListener(this);
					panelButtons.add(btnScan);
				}

				{
					btnAnnuler = new JButton();
					btnAnnuler.setText("ANNULER");
					// btnAnnuler.setEnabled(false);
					btnAnnuler.addActionListener(this);
					panelButtons.add(btnAnnuler);
				}
				{
					btnOpen = new JButton();
					btnOpen.setText("CONFIRMATION");
					btnOpen.addActionListener(this);
					panelButtons.add(btnOpen);
				}
				{
					lblCodemembre = new JLabel();
					panelButtons.add(lblCodemembre);
				}
				/*
				 * { btnCancel = new JButton(); btnCancel.setText("Confirmation espace");
				 * btnCancel.setEnabled(true); btnCancel.addActionListener(this);
				 * panelButtons.add(btnCancel); }
				 */
				{
					btnVerify = new JButton();
					btnVerify.setText("Verify");
					btnVerify.addActionListener(this);
					// panelButtons.add(btnVerify);
				}
				{
					cbAutomatic = new JCheckBox();
					cbAutomatic.setSelected(true);
					cbAutomatic.setText("Scan automatically");
					// panelButtons.add(cbAutomatic);
				}

			}
		}
		{
			scrollPane = new JScrollPane();
			panelMain.add(scrollPane, BorderLayout.CENTER);
			{
				view = new NFingerView();
				view.setShownImage(ShownImage.RESULT);
				view.setAutofit(true);
				view.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent ev) {
						super.mouseClicked(ev);
						if (ev.getButton() == MouseEvent.BUTTON3) {
							cbShowBinarized.doClick();
						}
					}

				});
				scrollPane.setViewportView(view);
			}
		}
		{
			panelSouth = new JPanel();
			panelSouth.setLayout(new BorderLayout());
			panelMain.add(panelSouth, BorderLayout.SOUTH);
			{
				panelInfo = new JPanel();
				panelInfo.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
				panelInfo.setLayout(new GridLayout(1, 1));
				panelSouth.add(panelInfo, BorderLayout.NORTH);
				{
					lblInfo = new JLabel();
					lblInfo.setText(" ");
					panelInfo.add(lblInfo);
				}
			}
			{
				panelSave = new JPanel();
				panelSave.setLayout(new FlowLayout(FlowLayout.LEADING));
				panelSouth.add(panelSave, BorderLayout.WEST);
				{
					btnSaveImage = new JButton();
					btnSaveImage.setText("Save image");
					btnSaveImage.setEnabled(false);
					btnSaveImage.addActionListener(this);
					// panelSave.add(btnSaveImage);
				}
				{
					btnSaveTemplate = new JButton();
					btnSaveTemplate.setText("Save template");
					btnSaveTemplate.setEnabled(false);
					btnSaveTemplate.addActionListener(this);
					// panelSave.add(btnSaveTemplate);
				}

				{
					cbShowBinarized = new JCheckBox();
					cbShowBinarized.setSelected(true);
					cbShowBinarized.setText("Show binarized image");
					cbShowBinarized.addActionListener(this);
					panelSave.add(cbShowBinarized);
				}

			}
			{
				NViewZoomSlider zoomSlider = new NViewZoomSlider();
				zoomSlider.setView(view);
				panelSouth.add(zoomSlider, BorderLayout.EAST);
			}
			{
				lblCount = new JLabel();
				lblCount.setText("0");
				panelSouth.add(lblCount, BorderLayout.CENTER);
			}
		}

		fcImage = new JFileChooser();
		fcImage.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
		fcTemplate = new JFileChooser();
		fileChooserTemplateFile = new JFileChooser();
	}

	@Override
	protected void setDefaultValues() {
		// No default values.
	}

	@Override
	protected void updateControls() {
		System.out.println("Updating controls");
		btnQrCodem.setEnabled(!scanning);
		btnAnnuler.setEnabled(!scanning);
		btnScan.setEnabled(!scanning);
		// btnForce.setEnabled(scanning && !cbAutomatic.isSelected());
		btnVerify.setEnabled(!scanning);
		btnRefresh.setEnabled(!scanning);
		// btnSaveTemplate.setEnabled(!scanning && (subject != null) &&
		// (subject.getStatus() == NBiometricStatus.OK));
		// btnSaveImage.setEnabled(!scanning && (subject != null) &&
		// (subject.getStatus() == NBiometricStatus.OK));
		cbShowBinarized.setEnabled(!scanning);
		cbAutomatic.setEnabled(!scanning);
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setUseDeviceManager(true);
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);

	}

	// ===========================================================
	// Public methods
	// ===========================================================

	public void updateScannerList() {
		DefaultListModel<NFingerScanner> model = (DefaultListModel<NFingerScanner>) scannerList.getModel();
		model.clear();
		deviceManager.getDevices().stream().forEach((device) -> {
			model.addElement((NFingerScanner) device);
		});
		NFingerScanner scanner = (NFingerScanner) FingersTools.getInstance().getClient().getFingerScanner();
		if ((scanner == null) && (model.getSize() > 0)) {
			scannerList.setSelectedIndex(0);
		} else if (scanner != null) {
			scannerList.setSelectedValue(scanner, true);
		}
	}

	public void cancelCapturing() {
		FingersTools.getInstance().getClient().cancel();
		view.setFinger(null);

		lblCodemembre.setText("");
		codeMembre = "";
		procuration = 0;
		cbProcuration.setSelected(false);
		statutReponse = 0;

	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnRefresh) {
				updateScannerList();
			} else if (ev.getSource() == btnVerify) {
				// identify();
				// FingersTools.getInstance().getClient().force();
				// verify();
			} else if (ev.getSource() == btnSaveImage) {
				saveImage();
			} else if (ev.getSource() == btnSaveTemplate) {
				saveTemplate();
			} else if (ev.getSource() == cbShowBinarized) {
				updateShownImage();
			} else if (ev.getSource() == btnScan) {
				startCapturing();
			} else if (ev.getSource() == btnOpen) {
				sendToServer(confirmation);
				if (statutReponse == 3) {
					cancelCapturing();
				}
			} else if (ev.getSource() == btnQrCodem) {
				captureQRMembre();
			} else if (ev.getSource() == btnAnnuler) {
				cancelCapturing();
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class CaptureCompletionHandler implements CompletionHandler<NBiometricTask, Object> {
		@Override
		public void completed(final NBiometricTask result, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					System.out.println("Restoring Image ");
					scanning = false;
					updateShownImage();
					System.out.println("After Image ");
					if (result.getStatus() == NBiometricStatus.OK) {
						updateStatus("Quality: " + getSubject().getFingers().get(0).getObjects().get(0).getQuality());
					} else {
						updateStatus(result.getStatus().toString());
					}
					updateControls();
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					scanning = false;
					updateShownImage();
					showError(th);
					updateControls();
				}

			});
		}

	}

	private class ScannerSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			FingersTools.getInstance().getClient().setFingerScanner(getSelectedScanner());
		}
	}

	private void verify() {
		updateFingersTools();
		FingersTools.getInstance().getClient().verify(subjectCurrent, subjectFromFile, null, verificationHandler);
		subjects.clear();

	}

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, String> {

		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (status != NBiometricStatus.OK) {
						JOptionPane.showMessageDialog(EnrollFromScanner.this, "Template was not created: " + status,
								"Error", JOptionPane.WARNING_MESSAGE);
					}
					updateControls();
				}
			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showError(th);
				}
			});
		}

	}

	private class VerificationHandler implements CompletionHandler<NBiometricStatus, String> {

		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (status == NBiometricStatus.OK) {
						int score = subjectCurrent.getMatchingResults().get(0).getScore();
						JOptionPane.showMessageDialog(EnrollFromScanner.this, "Confirmation bien effectuée", "Match",
								JOptionPane.PLAIN_MESSAGE);

						/*
						 * NIndexPair[] matedMinutiae =
						 * getLeft().getMatchingResults().get(0).getMatchingDetails().getFingers().get(0
						 * ).getMatedMinutiae();
						 * 
						 * viewLeft.setMatedMinutiaIndex(0); viewLeft.setMatedMinutiae(matedMinutiae);
						 * 
						 * viewRight.setMatedMinutiaIndex(1); viewRight.setMatedMinutiae(matedMinutiae);
						 * 
						 * viewLeft.prepareTree(); viewRight.setTree(viewLeft.getTree());
						 */
					} else {
						JOptionPane.showMessageDialog(EnrollFromScanner.this, "Templates didn't match.", "No match",
								JOptionPane.WARNING_MESSAGE);
					}
				}

			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					showError(th);
				}

			});
		}

	}
	/*
	 * private void openTemplates() throws IOException {
	 * 
	 * if (fileChooserTemplateFile.showOpenDialog(this) ==
	 * JFileChooser.APPROVE_OPTION) { //((DefaultTableModel)
	 * tableResults.getModel()).setRowCount(0); subjects.clear();
	 * 
	 * // Create subjects from selected templates. for (File file :
	 * fileChooserTemplateFile.getSelectedFiles()) {
	 * System.out.println("file: "+file.getAbsolutePath());
	 * 
	 * NSubject s = NSubject.fromFile(file.getAbsolutePath());
	 * s.setId(file.getName()); subjects.add(s); }
	 * lblCount.setText(String.valueOf(subjects.size())); } //updateControls(); }
	 * 
	 * private void openProbe() throws IOException { //if
	 * (fileChooserTemplateFile.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
	 * { String recuperationPath = getUSBPathName()+nomFichierTemplate;
	 * subjectFromFile = null; try { subjectFromFile =
	 * NSubject.fromFile(recuperationPath);
	 * subjectFromFile.setId(nomFichierTemplate); NSubject.FingerCollection fingers
	 * = subjectFromFile.getFingers();
	 * 
	 * subjects.add(subjectFromFile);
	 * 
	 * if (fingers.isEmpty()) { subjectFromFile = null; throw new
	 * IllegalArgumentException("LE FICHIER NE CONTIENT AUCUN ENREGISTREMENTS"); }
	 * //finger = fingers.get(0);
	 * templateCreationHandler.completed(NBiometricStatus.OK, null);
	 * 
	 * //currentsubject } catch (UnsupportedOperationException e) { // Ignore.
	 * UnsupportedOperationException means file is not a valid template.
	 * JOptionPane.showMessageDialog(EnrollFromScanner.this,
	 * "ERREUR DE RECUPERATION DU FICHIER.", "ERROR", JOptionPane.WARNING_MESSAGE);
	 * 
	 * } /* // If file is not a template, try to load it as an image. if
	 * (subjectFromFile == null) { finger = new NFinger();
	 * finger.setFileName(fileChooserTemplateFile.getSelectedFile().getAbsolutePath(
	 * )); subjectFromFile = new NSubject();
	 * subjectFromFile.setId(fileChooserTemplateFile.getSelectedFile().getName());
	 * subjectFromFile.getFingers().add(finger); updateFingersTools();
	 * FingersTools.getInstance().getClient().createTemplate(subjectFromFile, null,
	 * templateCreationHandler); /* // If file is not a template, try to load it as
	 * an image. if (subjectFromFile == null) { finger = new NFinger();
	 * finger.setFileName(fileChooserTemplateFile.getSelectedFile().getAbsolutePath(
	 * )); subjectFromFile = new NSubject();
	 * subjectFromFile.setId(fileChooserTemplateFile.getSelectedFile().getName());
	 * subjectFromFile.getFingers().add(finger); updateFingersTools();
	 * FingersTools.getInstance().getClient().createTemplate(subjectFromFile, null,
	 * templateCreationHandler); }
	 * 
	 * }
	 * 
	 * private void identify() { System.out.println("identify"); //
	 * subjects.add(subjectCurrent); //
	 * System.out.println("subjectCurrent id= "+subjectCurrent.getId().toString());
	 * System.out.println("subject from file id= "+subjectFromFile.getId());
	 * System.out.println("subjects size= "+subjects.size());
	 * 
	 * if ((subjectFromFile != null) && !subjects.isEmpty()) {
	 * //((DefaultTableModel) tableResults.getModel()).setRowCount(0);
	 * updateFingersTools();
	 * 
	 * // Clean earlier data before proceeding, enroll new data
	 * FingersTools.getInstance().getClient().clear();
	 * 
	 * // Create enrollment task. NBiometricTask enrollmentTask = new
	 * NBiometricTask(EnumSet.of(NBiometricOperation.ENROLL));
	 * 
	 * // Add subjects to be enrolled. subjects.stream().forEach((s) -> {
	 * enrollmentTask.getSubjects().add(s); }); // Enroll subjects.
	 * FingersTools.getInstance().getClient().performTask(enrollmentTask, null,
	 * enrollHandler); } }
	 */

	private class EnrollHandler implements CompletionHandler<NBiometricTask, Object> {

		@Override
		public void completed(final NBiometricTask task, final Object attachment) {
			if (task.getStatus() == NBiometricStatus.OK) {

				// Identify current subject in enrolled ones.
				FingersTools.getInstance().getClient().identify(getSubject(), null, identificationHandler);
			} else {
				JOptionPane.showMessageDialog(EnrollFromScanner.this, "Enrollment failed: " + task.getStatus(), "Error",
						JOptionPane.WARNING_MESSAGE);
			}
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					showError(th);
				}

			});
		}

	}

	private class IdentificationHandler implements CompletionHandler<NBiometricStatus, Object> {
		@Override
		public void completed(final NBiometricStatus status, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if ((status == NBiometricStatus.OK) || (status == NBiometricStatus.MATCH_NOT_FOUND)) {

						// Match subjects.
						for (NSubject s : getSubjects()) {
							boolean match = false;
							for (NMatchingResult result : getSubject().getMatchingResults()) {
								if (s.getId().equals(result.getId())) {
									match = true;
									// prependIdentifyResult(result.getId(), result.getScore());
									break;
								}
							}
							if (!match) {
								// appendIdentifyResult(s.getId(), 0);
								System.out.println("match= " + match);
							}
						}
					} else {
						JOptionPane.showMessageDialog(EnrollFromScanner.this, "Identification failed: " + status,
								"Error", JOptionPane.WARNING_MESSAGE);
					}
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					showError(th);
				}

			});
		}

	}

	// retrouver le lecteur USB qui contiendra les fichiers
	private static String getUSBPathName() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] paths;
		paths = File.listRoots();

		for (File path : paths) {
			if (fsv.getSystemDisplayName(path).contains("USB")
					|| fsv.getSystemDisplayName(path).contains("Disque amovible")) {

				File file = new File(path.getAbsolutePath() + "esmctemp.data");
				if (file.isFile() && file.exists()) {
					return path.getAbsolutePath();

				}
			}

		}
		return null;
	}

	private static String getAbsoluteUSBPath() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] paths;
		paths = File.listRoots();
		int index = paths.length;

		if (index > 0) {

			String[] ListPaths = new String[index];

			for (int i = 0; i < index; i++) {
				ListPaths[i] = fsv.getSystemDisplayName(paths[i]);
			}
			String n = (String) JOptionPane.showInputDialog(null, "Sélectionner le lecteur de la carte biométrique",
					"choix du disque", JOptionPane.QUESTION_MESSAGE, null, ListPaths, ListPaths[0]);

			int indextrouve = 0;

			if (Objects.nonNull(n)) {

				for (int i = 0; i < index; i++) {
					if (n.equals(fsv.getSystemDisplayName(paths[i]))) {
						indextrouve = i;
					}
				}

				if (indextrouve >= 0) {
					return paths[indextrouve].getAbsolutePath();

				}
			}

		}

		return null;
	}

	private String openTemplateFromFile(String recuperationPath) {
		NTemplate nTemplateFromFile = null;
		subjectFromFile = null;
		String stringTemplate = "";
		try {
			subjectFromFile = NSubject.fromFile(recuperationPath);
			subjectFromFile.setId(nomFichierTemplate);
			NSubject.FingerCollection fingers = subjectFromFile.getFingers();

			if (fingers.isEmpty()) {
				subjectFromFile = null;
				throw new IllegalArgumentException("LE FICHIER NE CONTIENT AUCUN ENREGISTREMENT");
			}
			// finger = fingers.get(0);
			// templateCreationHandler.completed(NBiometricStatus.OK, null);

			nTemplateFromFile = new NTemplate(subjectFromFile.getTemplateBuffer());

			NBuffer buffer = nTemplateFromFile.save();
			byte[] templateByte = buffer.toByteArray();

			stringTemplate = byteArrayToHexString(templateByte);

		} catch (UnsupportedOperationException | IOException e) {
			// Ignore. UnsupportedOperationException means file is not a valid template.
			JOptionPane.showMessageDialog(EnrollFromScanner.this, "ERREUR DE RECUPERATION DU FICHIER.", "ERROR",
					JOptionPane.WARNING_MESSAGE);

		}
		return stringTemplate;
	}

	private int sendToServer(Confirmation confirmation) throws IOException {

		String urlPath = "";
		String message = "Verifier la connexion";
		if (priseEmpreinte) {
			// tester la presence du fichier
			String nomFichier = getAbsoluteUSBPath() + nomFichierTemplate;

			File fichier = new File(nomFichier);
			if (Objects.isNull(getUSBPathName())) {
				JOptionPane.showMessageDialog(this, "Veuillez brancher votre carte");
			} else if (!fichier.exists()) {
				JOptionPane.showMessageDialog(this, "Veuillez brancher votre carte");
			} else {
				// recuperer le template de la carte

				String stringCardTemplate = openTemplateFromFile(nomFichier);

				NTemplate template = new NTemplate(subjectCurrent.getTemplateBuffer());

				NBuffer buffer = template.save();
				byte[] templateByte = buffer.toByteArray();

				String stringTemplate = byteArrayToHexString(templateByte);
				//
				if (cbProcuration.isSelected()) {
					procuration = 1;
				}
				// envoi sur serveur pour ecrire dans la table confirmation
				confirmation = new Confirmation();
				if (codeMembre == null) {
					JOptionPane.showMessageDialog(this, "Le code membre est nul. Prendre le QR CODE");
				} else {
					confirmation.setCodeMembre(codeMembre);
					confirmation.setStringTemplate(stringTemplate);
					confirmation.setStringCardTemplate(stringCardTemplate);
					confirmation.setProcuration(procuration);

					/*
					 * if(choixReseau ==0){//intranet urlPath =
					 * "http://tom.gacsource.net/jmcnpApi/fingermatching/confirmerauth";
					 */
					if (choixReseau == 0) {// intranet
						urlPath = "http://tom.gacsource.net/jmcnpApi/fingermatching/confirmerauth";

					} else if (choixReseau == 1) {// internet
						urlPath = "https://tom.esmcgie.com/jmcnpApi/fingermatching/confirmerauth";
					}
					String param = SerializationTools.jsonSerialise(confirmation);
					String result = RestClient.executePost(urlPath, param);

					Result reponse = (Result) SerializationTools.jsonDeserialise(result, Result.class);
					if (reponse != null && reponse instanceof Result) {
						message = ((Result) reponse).getMessage();
						statutReponse = reponse.getResultat();
					} 
					JOptionPane.showMessageDialog(this, message);
					cancelCapturing();
					template.close();
					subjectCurrent.close();
					return statutReponse;
				}
			}
		}
		// JOptionPane.showMessageDialog(this, "Veuillez cliquer sur SCANNER pour
		// prendre l'empreinte");
		return statutReponse;
	}

	/*
	 * public void captureQRFournisseur() { final Thread thread = new Thread(new
	 * Runnable() {
	 * 
	 * @Override public void run() { try { QrCapture qr = new QrCapture();
	 * codeMembreFournisseur= qr.getResult().substring(0, 20);
	 * System.out.println("codeMembreFournisseur= "+codeMembreFournisseur);
	 * 
	 * // codeMembre = codeMembreTextField.getText(); } catch (InterruptedException
	 * ex) { ex.printStackTrace(); } } ; }); thread.setDaemon(true); thread.start();
	 * }
	 */

	public void captureQRMembre() {
		lblCodemembre.setText("");

		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					QrCapture qr = new QrCapture();
					codeMembre = qr.getResult().substring(0, 20);
					lblCodemembre.setText(codeMembre);
					qr.close();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

			};
		});
		thread.setDaemon(true);
		thread.start();
	}

	// Converting a bytes array to string of hex character
	public String byteArrayToHexString(byte[] b) {
		int len = b.length;
		String data = new String();
		for (int i = 0; i < len; i++) {
			data += Integer.toHexString((b[i] >> 4) & 0xf);
			data += Integer.toHexString(b[i] & 0xf);
		}
		return data.toUpperCase();
	}

	public byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

}
