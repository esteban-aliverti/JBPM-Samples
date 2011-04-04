/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.plugtree.training.jbpm;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.process.workitem.wsht.WSHumanTaskHandler;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.responsehandlers.BlockingTaskOperationResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author esteban
 */
public class UserGroupsProcessTest extends BaseTest implements Serializable {

    private KnowledgeRuntimeLogger fileLogger;
    private StatefulKnowledgeSession ksession;
    private long waitTime = 1000;
    
    @Before
    public void setup() throws IOException{
        this.ksession = this.createKSession();
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
        
        //File logger: try to open its output using Audit View in eclipse
        File logFile = File.createTempFile("process-output", "");
        System.out.println("Log file= "+logFile.getAbsolutePath()+".log");
        fileLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession,logFile.getAbsolutePath());
        
        //Configure WIHandler for Human Tasks
        this.ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new WSHumanTaskHandler());
        
    }

    @After
    public void cleanup(){
        if (this.fileLogger != null){
            this.fileLogger.close();
        }
    } 
    
    @Test
    public void taskGroupTest() throws InterruptedException{
        //These is the list of groups where krsiv belongs. jBPM5 doesn't define
        //any relation between users and groups. It is the responsability of 
        //API's users to make these relations when calling HT Client methods. 
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("GroupA");
        
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("org.plugtree.training.jbpm.sampleprocess");
        
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        Thread.sleep(500);
        
        //krisv doesn't have a task for itself. (see the definition of
        //the porcess)
        BlockingTaskSummaryResponseHandler responseHandler = new BlockingTaskSummaryResponseHandler();
        client.getTasksAssignedAsPotentialOwner("krisv", "en-UK", responseHandler);
        List<TaskSummary> results = responseHandler.getResults();
        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
        
        //But if krisv is in GroupA, then he has 1 task (because the group has
        //1 task). Here we are making the relation between user ris and group 
        //GroupA
        responseHandler = new BlockingTaskSummaryResponseHandler();
        client.getTasksAssignedAsPotentialOwner("krisv",groupIds, "en-UK", responseHandler);
        results = responseHandler.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        TaskSummary krisvsTask = results.get(0);
        
        //krisv claims the task (always indicating the group)
        BlockingTaskOperationResponseHandler operationResponseHandler = new BlockingTaskOperationResponseHandler();
        client.claim(krisvsTask.getId(), "Tony Stark", groupIds, operationResponseHandler );
        operationResponseHandler.waitTillDone(waitTime);
        
        //krisv completes the task. There is no need to specify the groups
        //anymore because the ask is already claimed.
        operationResponseHandler = new BlockingTaskOperationResponseHandler();
        client.start(krisvsTask.getId(), "Tony Stark", operationResponseHandler);
        operationResponseHandler.waitTillDone(waitTime);
        operationResponseHandler = new BlockingTaskOperationResponseHandler();
        client.complete(krisvsTask.getId(), "Tony Stark", null, operationResponseHandler);
        operationResponseHandler.waitTillDone(waitTime);
        
        Thread.sleep(5000);
        
        //The process should be completed
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }
    
    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKSession(){
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("process/userGroupsSampleProcess.bpmn"), ResourceType.BPMN2);
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

        //return a new statefull session
        return kbase.newStatefulKnowledgeSession();
    }
    
}
