package com.esmc.client.confirmation.classes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.esmc.client.confirmation.utils.ImageThumbnailFileChooser;
import com.esmc.client.confirmation.utils.Utils;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.images.NImage;
import com.neurotec.images.NImages;
import com.neurotec.io.NFile;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.concurrent.CompletionHandler;

public final class GeneralizeFinger extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private ImageThumbnailFileChooser fcOpen;
	private ImageThumbnailFileChooser fcSave;
	NViewZoomSlider imageZoomSlider;
	private JButton btnOpenImage;
	private JButton btnSaveTemplate;
	private JCheckBox cbShowBinarized;
	private JFileChooser fcSaveTemplate;
	private JLabel lblStatus;
	private JLabel lblImagesLoaded;
	private JLabel lblImageCount;
	private JPanel actionButtonsPanel;
	private JPanel panelNorth;
	private JPanel panelCenter;
	private JPanel panelSouth;
	private JScrollPane scrollPaneImage;
	private NFingerView fingerView;
	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();
	private NSubject subject = null;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public GeneralizeFinger() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void openImages() throws IOException {
		subject = null;
		fingerView.setFinger(null);
		btnSaveTemplate.setEnabled(false);
		lblImageCount.setText("0");
		lblStatus.setVisible(false);

		if (fcOpen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File[] files = fcOpen.getSelectedFiles();
			if (files.length < 3 || files.length > 10) {
				String msg = String.format("%s images selected. Please select at least 3 and no more than 10 images", files.length > 10 ? "Too many" : "Too few");
				showError(msg);
			} else {
				subject = new NSubject();
				for (File file : files) {
					NFinger finger = new NFinger();
					finger.setImage(NImage.fromFile(file.getAbsolutePath()));
					finger.setSessionId(1); // all fingers with same session will be generalized
					subject.getFingers().add(finger);
				}
				lblImageCount.setText(String.valueOf(files.length));
				lblStatus.setText("Status: performing extraction and generalizarion");
				lblStatus.setVisible(true);
				updateControls();
				updateFingersTools();
				FingersTools.getInstance().getClient().createTemplate(subject, null, templateCreationHandler);
			}
		}
	}

	private void saveTemplate() throws IOException {
		if (subject != null) {
			if (fcSaveTemplate.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				try {
					NFile.writeAllBytes(fcSaveTemplate.getSelectedFile().getAbsolutePath(), subject.getTemplateBuffer());
				} catch (Exception e) {
					showError(e);
				}
			}
		}
	}

	private void showBinarizedImageChanged() {
		if (cbShowBinarized.isSelected()) {
			fingerView.setShownImage(ShownImage.RESULT);
		} else {
			fingerView.setShownImage(ShownImage.ORIGINAL);
		}
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		setLayout(new BorderLayout());
		{
			panelNorth = new JPanel();
			panelNorth.setLayout(new BoxLayout(panelNorth, BoxLayout.Y_AXIS));
			add(panelNorth, BorderLayout.NORTH);

			panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
			panelNorth.add(panelLicensing);
			{
				actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				actionButtonsPanel.setBorder(new TitledBorder(null, "Load finger images (Min 3, Max 10)", TitledBorder.LEADING, TitledBorder.TOP, null, null));
				panelNorth.add(actionButtonsPanel);
				{
					btnOpenImage = new JButton("Open image");
					btnOpenImage.addActionListener(this);
					actionButtonsPanel.add(btnOpenImage);
				}
				{
					lblImagesLoaded = new JLabel("Images loaded:");
					actionButtonsPanel.add(lblImagesLoaded);
				}
				{
					lblImageCount = new JLabel("0");
					actionButtonsPanel.add(lblImageCount);
				}
			}
			{
				panelCenter = new JPanel();
				add(panelCenter, BorderLayout.CENTER);
				scrollPaneImage = new JScrollPane();
				panelCenter.setLayout(new GridLayout(1, 1));
				scrollPaneImage.setMinimumSize(new Dimension(100, 100));
				scrollPaneImage.setPreferredSize(new Dimension(200, 200));
				panelCenter.add(scrollPaneImage);
				fingerView = new NFingerView();
				scrollPaneImage.setViewportView(fingerView);
			}
			{
				panelSouth = new JPanel();
				add(panelSouth, BorderLayout.SOUTH);
				panelSouth.setLayout(new BorderLayout());
				{
					JPanel saveTemplatePanel = new JPanel();
					saveTemplatePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
					panelSouth.add(saveTemplatePanel, BorderLayout.WEST);
					{
						btnSaveTemplate = new JButton("Save template");
						btnSaveTemplate.setEnabled(false);
						btnSaveTemplate.addActionListener(this);
						saveTemplatePanel.add(btnSaveTemplate);
					}
					{
						cbShowBinarized = new JCheckBox("Show binarized image");
						cbShowBinarized.addActionListener(this);
						cbShowBinarized.setSelected(true);
						saveTemplatePanel.add(cbShowBinarized);
					}
					{
						lblStatus = new JLabel("Status: None");
						saveTemplatePanel.add(lblStatus);
					}
				}
				{
					imageZoomSlider = new NViewZoomSlider();
					imageZoomSlider.setView(fingerView);
					panelSouth.add(imageZoomSlider, BorderLayout.EAST);
				}
			}
		}

		fcOpen = new ImageThumbnailFileChooser();
		//fcOpen.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcOpen.setFileFilter(new Utils.ImageFileFilter(NImages.getOpenFileFilter()));
		fcOpen.setMultiSelectionEnabled(true);
		fcSaveTemplate = new JFileChooser();
		fcSave = new ImageThumbnailFileChooser();
		//fcSave.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcSave.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
	}

	@Override
	protected void setDefaultValues() {
	}

	@Override
	protected void updateControls() {
		btnSaveTemplate.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		showBinarizedImageChanged();
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnOpenImage) {
				openImages();
			} else if (ev.getSource() == btnSaveTemplate) {
				saveTemplate();
			} else if (ev.getSource() == cbShowBinarized) {
				showBinarizedImageChanged();
			}
		} catch (Exception e) {
			showError(e);
			updateControls();
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, Object> {

		@Override
		public void completed(final NBiometricStatus status, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (status == NBiometricStatus.OK) {
						lblStatus.setText("Status: OK");
						btnSaveTemplate.setEnabled(true);
						fingerView.setFinger(subject.getFingers().get(subject.getFingers().size() - 1));
					} else {
						lblStatus.setText(String.format("Status: %s", status));
					}
					btnOpenImage.setEnabled(true);
				}
			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					lblStatus.setText("Status: Error occured");
					showError(th);
					btnOpenImage.setEnabled(true);
				}
			});
		}
	}
}
