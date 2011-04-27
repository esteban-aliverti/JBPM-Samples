package org.plugtree.training.jbpm.humantasks.client.core;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.plugtree.training.jbpm.humantasks.client.report.ReportService;
import org.plugtree.training.jbpm.humantasks.client.report.ReportService.DocumentProcessReport;
import org.plugtree.training.jbpm.humantasks.client.report.SingletonSimpleReportService;

public class GenerateReportWorkItem implements WorkItemHandler {

	private ReportService service = SingletonSimpleReportService.getInstance();

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String document = (String) workItem.getParameter("Document");
		String translatedDocument = (String) workItem
				.getParameter("TranslatedDocument");
		String reviewComment = (String) workItem
				.getParameter("ReviewComment");
		DocumentProcessReport report = new DocumentProcessReport();
		report.setProcessInstance(workItem.getProcessInstanceId());
		report.setDocument(document);
		report.setTranslatedDocument(translatedDocument);
		report.setReviewComment(reviewComment);
		service.addProcessReport(report);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	};
}
