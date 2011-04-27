package org.plugtree.training.jbpm.humantasks.client.ui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import org.jbpm.task.User;
import org.jbpm.task.query.TaskSummary;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.plugtree.training.jbpm.humantasks.client.core.HumanTaskClient;

import com.cloudgarden.layout.AnchorConstraint;
import com.cloudgarden.layout.AnchorLayout;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class WriteDocumentDialog extends javax.swing.JDialog {
	private JTextArea writeDocumentPanel;
	private JButton startWriteButton;
	private JLabel taskLabel;
	private JButton submitWriteButton;
	private TaskSummary taskSummary;
	private User user;
	private HumanTaskClient client;
	private long startTime;
	private JLabel referenceLabel;
	private JTextArea referenceTextArea;

	public WriteDocumentDialog(JFrame frame, TaskSummary task, User user,
			HumanTaskClient theClient, String reference) {
		super(frame);
		this.user = user;
		this.taskSummary = task;
		this.client = theClient;
		initGUI();
	}

	private void initGUI() {
		try {
			AnchorLayout thisLayout = new AnchorLayout();
			getContentPane().setLayout(thisLayout);
			{
				referenceLabel = new JLabel();
				getContentPane().add(
						referenceLabel,
						new AnchorConstraint(12, 703, 68, 503,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				referenceLabel
						.setPreferredSize(new java.awt.Dimension(136, 15));
				referenceLabel.setName("reference");
				referenceLabel.setText("Reference Data:");
			}
			{
				referenceTextArea = new JTextArea();
				getContentPane().add(
						referenceTextArea,
						new AnchorConstraint(101, 962, 727, 503,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				referenceTextArea.setPreferredSize(new java.awt.Dimension(312,
						169));
				referenceTextArea.setName("referenceTextArea");
				referenceTextArea.setEditable(false);
			}
			this.setReferenceArea();
			javax.swing.ActionMap actionMap = org.jdesktop.application.Application
					.getInstance(
							org.plugtree.training.jbpm.humantasks.client.ui.Application.class)
					.getContext().getActionMap(WriteDocumentDialog.class, this);
			{
				taskLabel = new JLabel();
				getContentPane().add(
						taskLabel,
						new AnchorConstraint(27, 680, 101, 11,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				taskLabel.setPreferredSize(new java.awt.Dimension(455, 20));
				taskLabel.setText("Your task is "
						+ taskSummary.getDescription());
			}
			{
				submitWriteButton = new JButton();
				getContentPane().add(
						submitWriteButton,
						new AnchorConstraint(772, 378, 929, 66,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				submitWriteButton.setPreferredSize(new java.awt.Dimension(120,
						41));
				submitWriteButton.setName("SubmitWriteButton");
				submitWriteButton.setAction(actionMap.get("completeTask"));
				submitWriteButton.setText("Submit Task");
				submitWriteButton.setEnabled(false);
			}
			{
				startWriteButton = new JButton();
				getContentPane().add(
						startWriteButton,
						new AnchorConstraint(772, 920, 929, 605,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				startWriteButton.setPreferredSize(new java.awt.Dimension(121,
						41));
				startWriteButton.setName("startWriteButton");
				startWriteButton.setAction(actionMap.get("startTask"));
				startWriteButton.setText("Start task");
			}
			{
				writeDocumentPanel = new JTextArea();
				getContentPane().add(
						writeDocumentPanel,
						new AnchorConstraint(101, 447, 727, 37,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				writeDocumentPanel.setPreferredSize(new java.awt.Dimension(279,
						169));
				writeDocumentPanel.setName("writeDocumentPanel");
			}
			this.setSize(690, 300);
			Application.getInstance().getContext().getResourceMap(getClass())
					.injectComponents(getContentPane());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setReferenceArea() {
		Object contentInput = client.getTaskContentInput(taskSummary);
		if (!(contentInput instanceof String)) {
			System.out.println("No Data input..");
			referenceTextArea.setText("No reference data...");
			referenceTextArea.setEnabled(false);
			return;
		}
		String stringContentInput = (String) contentInput;
		if (contentInput == null || stringContentInput.isEmpty()) {
			System.out.println("No Data input..");
			referenceTextArea.setText("No reference data...");
			referenceTextArea.setEnabled(false);
			return;
		}
		referenceTextArea.setText((String)contentInput);
		referenceTextArea.setEnabled(true);
	}

	@Action
	public void startTask() {
		this.client.startTask(user, this.taskSummary);
		this.startTime = System.currentTimeMillis();
		this.submitWriteButton.setEnabled(true);
		this.startWriteButton.setEnabled(false);
	}

	@Action
	public void completeTask() {
		Map result = new HashMap();
		result.put("Result", this.writeDocumentPanel.getText());
		this.client.completeTask(user, this.taskSummary, result);
		JOptionPane.showMessageDialog(null,
				"Task Completed in "
						+ (System.currentTimeMillis() - this.startTime)
						+ " seconds");
		this.setVisible(false);
	}

}
