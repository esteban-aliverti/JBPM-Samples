package org.plugtree.training.jbpm.humantasks.client.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Application;
import org.plugtree.training.jbpm.humantasks.client.report.ReportService;
import org.plugtree.training.jbpm.humantasks.client.report.ReportService.DocumentProcessReport;
import org.plugtree.training.jbpm.humantasks.client.report.SingletonSimpleReportService;

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
public class ReportDialog extends javax.swing.JDialog {
	private ReportService service = SingletonSimpleReportService.getInstance();
	private JList processList;
	private JTextArea reportDetail;

	/**
	 * Auto-generated main method to display this JDialog
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				frame.setTitle("Report");
				ReportDialog inst = new ReportDialog(frame);
				inst.setVisible(true);
			}
		});
	}

	public ReportDialog(JFrame frame) {
		super(frame);
		initGUI();
	}

	private void initGUI() {
		try {
			AnchorLayout thisLayout = new AnchorLayout();
			getContentPane().setLayout(thisLayout);
			{
				reportDetail = new JTextArea();
				getContentPane().add(
						reportDetail,
						new AnchorConstraint(84, 852, 709, 304,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				reportDetail.setPreferredSize(new java.awt.Dimension(289, 158));
				reportDetail.setName("reportDetail");
			}
			{
				final DefaultListModel processListModel = new DefaultListModel();
				processList = new JList();
				getContentPane().add(
						processList,
						new AnchorConstraint(49, 224, 709, 23,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL,
								AnchorConstraint.ANCHOR_REL));
				processList.setModel(processListModel);
				processList.setPreferredSize(new java.awt.Dimension(106, 167));
				MouseListener mouseListener = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() >= 1) {
							int position = processList.getSelectedIndex();
							String process = (String) processListModel
									.getElementAt(position);
							long pid = Long.parseLong(process.split(":")[1].trim());
							refreshProcessDetails(pid);
						}
					}
				};
				processList.addMouseListener(mouseListener);
				for (DocumentProcessReport report : service.getReports()) {
					processListModel.addElement("Process: " + report.getProcessInstance());
				}
			}
			this.setSize(529, 242);
			Application.getInstance().getContext().getResourceMap(getClass())
					.injectComponents(getContentPane());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refreshProcessDetails(long processId) {
		DocumentProcessReport report = this.service.getProcessReport(processId);
		StringBuilder textReport = new StringBuilder();
		textReport.append("Process:").append(processId).append("\n");
		textReport.append("Produced Document: ").append(report.getDocument())
				.append("\n");
		textReport.append("Translated Document: ")
				.append(report.getTranslatedDocument()).append("\n");
		textReport.append("Review Comment: ").append(report.getReviewComment())
				.append("\n");
		this.reportDetail.setText(textReport.toString());
	}

}
