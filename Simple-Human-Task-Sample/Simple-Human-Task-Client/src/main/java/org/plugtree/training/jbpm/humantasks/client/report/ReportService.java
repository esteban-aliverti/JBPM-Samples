package org.plugtree.training.jbpm.humantasks.client.report;

import java.util.List;

/**
 * Service to store and get reports about the document process.
 * 
 * @author calcacuervo
 * 
 */
public interface ReportService {

	/**
	 * Adds a new report.
	 * 
	 * @param report
	 */
	public void addProcessReport(DocumentProcessReport report);

	/**
	 * Returns a report for a given id. It will return null if it is not found.
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public DocumentProcessReport getProcessReport(long processInstanceId);

	/**
	 * Returns a the list of reports.
	 * 
	 * @return
	 */
	public List<DocumentProcessReport> getReports();

	/**
	 * Represents a document report. It holds information about a single
	 * process.
	 * 
	 */
	public static class DocumentProcessReport {

		/**
		 * Process instance.
		 */
		private long processInstance;

		/**
		 * The original document.
		 */
		private String document;

		/**
		 * The translated document.
		 */
		private String translatedDocument;

		/**
		 * The comment.
		 */
		private String reviewComment;

		/**
		 * @return the processInstance
		 */
		public long getProcessInstance() {
			return processInstance;
		}

		/**
		 * @param processInstance
		 *            the processInstance to set
		 */
		public void setProcessInstance(long processInstance) {
			this.processInstance = processInstance;
		}

		/**
		 * @return the document
		 */
		public String getDocument() {
			return document;
		}

		/**
		 * @param document
		 *            the document to set
		 */
		public void setDocument(String document) {
			this.document = document;
		}

		/**
		 * @return the translatedDocument
		 */
		public String getTranslatedDocument() {
			return translatedDocument;
		}

		/**
		 * @param translatedDocument
		 *            the translatedDocument to set
		 */
		public void setTranslatedDocument(String translatedDocument) {
			this.translatedDocument = translatedDocument;
		}

		/**
		 * @return the reviewComment
		 */
		public String getReviewComment() {
			return reviewComment;
		}

		/**
		 * @param reviewComment
		 *            the reviewComment to set
		 */
		public void setReviewComment(String reviewComment) {
			this.reviewComment = reviewComment;
		}

	}
}
