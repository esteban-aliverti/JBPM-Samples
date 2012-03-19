/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.plugtree.training.jbpm;

import bitronix.tm.TransactionManagerServices;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.impl.EnvironmentFactory;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.jbpm.process.workitem.wsht.SyncWSHumanTaskHandler;
import org.jbpm.task.query.TaskSummary;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author esteban
 */
public class SimplePersistenceTestHornetQ extends BaseTestHornetQ{

    private KnowledgeRuntimeLogger fileLogger;
    private StatefulKnowledgeSession ksession;
    
    private JPAWorkingMemoryDbLogger historyLogger;

    @Before
    public void setup() throws IOException {

        //gets a session
        this.ksession = this.createKSession();

        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);

        //File logger: try to open its output using Audit View in eclipse
        File logFile = File.createTempFile("process-output", "");
        System.out.println("Log file= " + logFile.getAbsolutePath() + ".log");
        fileLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession, logFile.getAbsolutePath());
    }

    @After
    public void cleanup() {
        try{
            if (historyLogger != null){
                historyLogger.dispose();
            }
            if (this.fileLogger != null) {
                this.fileLogger.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void simpleProcessTest() throws Exception {
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("org.plugtree.training.jbpm.humantasks.client");

        //The process is in the first Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //gets salaboy's tasks
        List<TaskSummary> salaboyTasks = this.taskClient.getTasksAssignedAsPotentialOwner("salaboy", "en-UK");
        Assert.assertEquals(1, salaboyTasks.size());
        
        //translator doesn't have any task
        List<TaskSummary> translatorTasks = this.taskClient.getTasksAssignedAsPotentialOwner("translator", "en-UK");
        Assert.assertTrue(translatorTasks.isEmpty());
        
        //reviewer doesn't have any task
        List<TaskSummary> reviewerTasks = this.taskClient.getTasksAssignedAsPotentialOwner("reviewer", "en-UK");
        Assert.assertTrue(reviewerTasks.isEmpty());
        
        
        //----------------------------------
        
        //Salaboy completes its task
        this.taskClient.start(salaboyTasks.get(0).getId(), "salaboy");
        this.taskClient.complete(salaboyTasks.get(0).getId(), "salaboy", null);
        
        //Now translator and reviewer have 1 task each
        translatorTasks = this.taskClient.getTasksAssignedAsPotentialOwner("translator", "en-UK");
        Assert.assertEquals(1, translatorTasks.size());
        
        reviewerTasks = this.taskClient.getTasksAssignedAsPotentialOwner("reviewer", "en-UK");
        Assert.assertEquals(1, reviewerTasks.size());
        
        //The process is not completed yet
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        
        //----------------------------------
        
        
        //both, reviewer and translator complete their tasks
        this.taskClient.start(salaboyTasks.get(0).getId(), "reviewer");
        this.taskClient.complete(reviewerTasks.get(0).getId(), "reviewer", null);
        
        this.taskClient.start(salaboyTasks.get(0).getId(), "translator");
        this.taskClient.complete(translatorTasks.get(0).getId(), "translator", null);
        
        
        //The process reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }

    /**
     * Creates a ksession from a kbase containing process definition.
     * The session is created using JPAKnowledgeService
     * @return 
     */
    public StatefulKnowledgeSession createKSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("process/humanTask.bpmn"), ResourceType.BPMN2);
        System.out.println("Compiling resources");

        //Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    System.out.println("Error building kbase: " + error.getMessage());
                }
            }
            throw new RuntimeException("Error building kbase!");
        }

        //Create a knowledge base and add the generated package
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        
        
        //create a new statefull session using JPAKnowledgeService
        final StatefulKnowledgeSession newSession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, this.createEnvironment());
        
        //Register Human Task Handler
        SyncWSHumanTaskHandler htHandler = new SyncWSHumanTaskHandler(taskClient, ksession);
        htHandler.setLocal(true);
        newSession.getWorkItemManager().registerWorkItemHandler("Human Task", htHandler);

        //Register a dumb WI Handler for "Report" Task
        newSession.getWorkItemManager().registerWorkItemHandler("Report", new DoNothingWorkItemHandler(){
            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
                manager.completeWorkItem(workItem.getId(), null);
            }
        });
        
        //Attach a bam logger
        historyLogger = new JPAWorkingMemoryDbLogger(newSession);
        
        return newSession;
    }
    
    private Environment createEnvironment(){
        Environment env = EnvironmentFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf );
        env.set(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());
//        env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, new ObjectMarshallingStrategy[]{
//            new JPAPlaceholderResolverStrategy(env),
//            new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT)
//        });
        
        return env;
    }
    

}