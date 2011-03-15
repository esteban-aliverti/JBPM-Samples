package org.plugtree.training.jbpm.humantasks.client.core;

import java.util.List;

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
import org.jbpm.task.User;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.mina.MinaTaskClientConnector;
import org.jbpm.task.service.mina.MinaTaskClientHandler;
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

	public void completeTask(User user, TaskSummary task) {
		System.out.println("Starting task " + task.getId());
		BlockingTaskOperationResponseHandler operationResponseHandler = new BlockingTaskOperationResponseHandler();
		client.start(task.getId(), user.getId(), operationResponseHandler);
		operationResponseHandler.waitTillDone(DEFAULT_WAIT_TIME);
		System.out.println("Started task " + task.getId());
		System.out.println("Completing task " + task.getId());
		operationResponseHandler = new BlockingTaskOperationResponseHandler();
		client.complete(task.getId(), user.getId(), null, operationResponseHandler);
		operationResponseHandler.waitTillDone(DEFAULT_WAIT_TIME);
		System.out.println("Completed task " + task.getId());
	}

	public void stop() {
		logger.close();
	}

}
