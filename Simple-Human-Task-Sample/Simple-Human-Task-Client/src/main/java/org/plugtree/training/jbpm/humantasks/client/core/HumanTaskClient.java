package org.plugtree.training.jbpm.humantasks.client.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.process.workitem.wsht.WSHumanTaskHandler;
import org.jbpm.task.AccessType;
import org.jbpm.task.Content;
import org.jbpm.task.Task;
import org.jbpm.task.TaskData;
import org.jbpm.task.User;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.mina.MinaTaskClientConnector;
import org.jbpm.task.service.mina.MinaTaskClientHandler;
import org.jbpm.task.service.responsehandlers.BlockingGetContentResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingGetTaskResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingTaskOperationResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;

public class HumanTaskClient {

	private static final long DEFAULT_WAIT_TIME = 5000;
	private TaskClient client;
	private KnowledgeRuntimeLogger logger;
	private StatefulKnowledgeSession ksession;

	public HumanTaskClient() {
		client = new TaskClient(new MinaTaskClientConnector("client 1",
                new MinaTaskClientHandler(SystemEventListenerFactory.getSystemEventListener())));
		client.connect("127.0.0.1", 9123);
	}

	public void start() throws RuntimeException {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

                kbuilder.add(new ClassPathResource("rules/humanTask.bpmn"), ResourceType.BPMN2);
                System.out.println("Compiling resources");
		if (kbuilder.hasErrors()) {
			if (kbuilder.getErrors().size() > 0) {
				for (KnowledgeBuilderError error: kbuilder.getErrors()) {
                                    System.out.println("Error building kbase: "+error.getMessage());
				}
			}
                        throw new RuntimeException("Error building kbase!");
		}

		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

		ksession = kbase.newStatefulKnowledgeSession();
		logger = KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new WSHumanTaskHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Report", new GenerateReportWorkItem());

	}

	public void startProcess() throws RuntimeException {
		try {
			ksession.startProcess( "org.plugtree.training.jbpm.humantasks.client", null );
		}
		catch (IllegalArgumentException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public List<TaskSummary> getAssignedTasks(User user) {
		BlockingTaskSummaryResponseHandler taskSummaryResponseHandler = new BlockingTaskSummaryResponseHandler();
		client.getTasksAssignedAsPotentialOwner(user.getId(), "en-UK", taskSummaryResponseHandler);
		taskSummaryResponseHandler.waitTillDone(DEFAULT_WAIT_TIME);
		List<TaskSummary> tasks = taskSummaryResponseHandler.getResults();
		return tasks;
	}

	public void completeTask(User user, TaskSummary task, Map data) {
		System.out.println("Completing task " + task.getId());
		BlockingTaskOperationResponseHandler operationResponseHandler = new BlockingTaskOperationResponseHandler();
		ContentData contentData = null;
		if (data != null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(data);
				out.close();
				contentData = new ContentData();
				contentData.setContent(bos.toByteArray());
				contentData.setAccessType(AccessType.Inline);
			}
			catch (IOException e) {
				System.err.print(e);
			}
		}
		client.complete(task.getId(), user.getId(), contentData, operationResponseHandler);
		operationResponseHandler.waitTillDone(DEFAULT_WAIT_TIME);
		System.out.println("Completed task " + task.getId());
	}
	
	public void startTask(User user, TaskSummary task) {
		System.out.println("Starting task " + task.getId());
		BlockingTaskOperationResponseHandler operationResponseHandler = new BlockingTaskOperationResponseHandler();
		client.start(task.getId(), user.getId(), operationResponseHandler);
		operationResponseHandler.waitTillDone(DEFAULT_WAIT_TIME);
		System.out.println("Started task " + task.getId());
	}

	public Object getTaskContentInput(TaskSummary taskSum) {
		BlockingGetTaskResponseHandler handlerT = new BlockingGetTaskResponseHandler();
		client.getTask(taskSum.getId(), handlerT);
		Task task2 = handlerT.getTask();
		TaskData taskData = task2.getTaskData();
		BlockingGetContentResponseHandler handlerC = new BlockingGetContentResponseHandler();
		client.getContent(taskData.getDocumentContentId(), handlerC);
		Content content = handlerC.getContent();
		ByteArrayInputStream bais = new ByteArrayInputStream(
				content.getContent());
		try {
			ObjectInputStream is = new ObjectInputStream(bais);
			Object obj = null;

			while ((obj = is.readObject()) != null) {
				System.out.println("OBJECT: " + obj);
				return obj;
			}
		} catch (Exception e) {
			System.err.print("There was an error reading task input...");
			e.printStackTrace();
			return null;
		}
		return null;
	}

	public void stop() {
		logger.close();
	}

}