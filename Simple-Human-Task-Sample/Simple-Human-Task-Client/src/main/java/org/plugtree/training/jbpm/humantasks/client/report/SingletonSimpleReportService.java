package org.plugtree.training.jbpm.humantasks.client.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton Implementation of a simple report service which uses a hash map to
 * store the {@link DocumentProcessReport}.
 * 
 * @author calcacuervo
 * 
 */
public class SingletonSimpleReportService implements ReportService {

	private Map<Long, DocumentProcessReport> processes = new HashMap<Long, DocumentProcessReport>();

	private static SingletonSimpleReportService serviceInstance = null;

	public List<DocumentProcessReport> getReports() {
		return new ArrayList<DocumentProcessReport>(processes.values());
	}

	private SingletonSimpleReportService() {

	}

	public static SingletonSimpleReportService getInstance() {
		if (serviceInstance == null) {
			serviceInstance = new SingletonSimpleReportService();
		}
		return serviceInstance;
	}

	public void addProcessReport(DocumentProcessReport report) {
		this.processes.put(report.getProcessInstance(), report);
	}

	public DocumentProcessReport getProcessReport(long processInstanceId) {
		return this.processes.get(processInstanceId);
	}

}
