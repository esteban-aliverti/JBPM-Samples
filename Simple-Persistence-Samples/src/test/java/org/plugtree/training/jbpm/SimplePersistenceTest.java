/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.plugtree.training.jbpm;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
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
import org.jbpm.task.AccessType;
import org.jbpm.task.Content;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.plugtree.training.jbpm.model.Document;
import org.plugtree.training.jbpm.model.DomainMarshallingStrategy;

/**
 *
 * @author esteban
 */
public class SimplePersistenceTest extends BaseTest{

    private KnowledgeRuntimeLogger fileLogger;
    private StatefulKnowledgeSession ksession;
    
    private JPAWorkingMemoryDbLogger historyLogger;
    private PoolingDataSource dataSource;
    EntityManagerFactory jbpmEmf;
    EntityManagerFactory domainEmf;

    @Before
    public void setup() throws IOException {
        //create datasource
        dataSource = this.createDataSrouce();
        
        //creates emfs
        this.jbpmEmf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
        this.domainEmf = Persistence.createEntityManagerFactory("domainPU");
        
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
            if (this.dataSource != null){
                this.dataSource.close();
            }
            if (this.jbpmEmf != null){
                this.jbpmEmf.close();
            }
            if (this.domainEmf != null){
                this.domainEmf.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void simpleProcessTest() throws Exception {
        TransactionManagerServices.getTransactionManager().begin();
        
        //creates an Entity Manager
        EntityManager em = this.domainEmf.createEntityManager();
        
        //create an empty document
        Document document = new Document();
        em.persist(document);
        em.close();
        
        //Start the process using its id
        Map<String, Object> inputParameters = new HashMap<String, Object>();
        inputParameters.put("document", document);
        ProcessInstance process = ksession.startProcess("org.plugtree.training.jbpm.humantasks.client", inputParameters);

        
        //The process is in the first Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //gets salaboy's tasks
        List<TaskSummary> salaboyTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("salaboy", "en-UK");
        Assert.assertEquals(1, salaboyTasks.size());
        
        //translator doesn't have any task
        List<TaskSummary> translatorTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("translator", "en-UK");
        Assert.assertTrue(translatorTasks.isEmpty());
        
        //reviewer doesn't have any task
        List<TaskSummary> reviewerTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("reviewer", "en-UK");
        Assert.assertTrue(reviewerTasks.isEmpty());
        
        
        //----------------------------------
        
        
        //Salaboy completes its task
        Document taskDocument = this.getTaskContent(salaboyTasks.get(0));
        taskDocument.setContent("This is the content of the document");
        Map result = new HashMap();
        result.put("Result", taskDocument);
        
        this.localTaskService.start(salaboyTasks.get(0).getId(), "salaboy");
        this.localTaskService.complete(salaboyTasks.get(0).getId(), "salaboy", this.prepareContentData(result));
        
        //Now reviewer has 1 task
        reviewerTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("reviewer", "en-UK");
        Assert.assertEquals(1, reviewerTasks.size());
        
        //No tasks for translator yet
        translatorTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("translator", "en-UK");
        Assert.assertTrue(translatorTasks.isEmpty());
        
        
        //The process is not completed yet
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //The document must have a content in the DB
        
        em = this.domainEmf.createEntityManager();
        Document dbDocument = em.find(Document.class, document.getId() );
        Assert.assertEquals("This is the content of the document", dbDocument.getContent());
        Assert.assertNull(dbDocument.getReviewedContent());
        Assert.assertNull(dbDocument.getTranslatedContent());
        
        
        //----------------------------------
        
        //reviewer completes its tasks
        this.localTaskService.start(reviewerTasks.get(0).getId(), "reviewer");
        this.localTaskService.complete(reviewerTasks.get(0).getId(), "reviewer", null);
        
        //Translator now has 1 task
        translatorTasks = this.localTaskService.getTasksAssignedAsPotentialOwner("translator", "en-UK");
        Assert.assertEquals(1, translatorTasks.size());
        
        //The process is not completed yet
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        
        //--------------------------------------
        
        
        //translator completes its tasks
        this.localTaskService.start(translatorTasks.get(0).getId(), "translator");
        this.localTaskService.complete(translatorTasks.get(0).getId(), "translator", null);
        
        
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
        SyncWSHumanTaskHandler htHandler = new SyncWSHumanTaskHandler(localTaskService, ksession);
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
        //historyLogger = new JPAWorkingMemoryDbLogger(newSession);
        
        return newSession;
    }
    
    private Document getTaskContent(TaskSummary summary) throws IOException, ClassNotFoundException{
        Content content = this.localTaskService.getContent(summary.getId());
        
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(content.getContent()));
        
        return (Document)ois.readObject();
    }
    
    /**
     * Convert a Map<String, Object> into a ContentData object.
     * @param data
     * @return 
     */
    private ContentData prepareContentData(Map data){
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
        
        return contentData;
    }
    
    private Environment createEnvironment(){
        
        Environment env = EnvironmentFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, jbpmEmf );
        env.set(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());
        env.set("DOMAIN_EMF", domainEmf);
        env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, new ObjectMarshallingStrategy[]{
            //new JPAPlaceholderResolverStrategy(env),
            new DomainMarshallingStrategy(env),
            new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT)
        });
        
        return env;
    }

    private PoolingDataSource createDataSrouce() {
        PoolingDataSource ds1 = new PoolingDataSource();
        ds1.setUniqueName("jdbc/testDS1");
        ds1.setClassName("org.h2.jdbcx.JdbcDataSource");
        ds1.setMaxPoolSize(3);
        ds1.setAllowLocalTransactions(true);
        ds1.getDriverProperties().put("user", "sa");
        ds1.getDriverProperties().put("password", "sasa");
        ds1.getDriverProperties().put("URL", "jdbc:h2:mem:mydb");
        ds1.init();
        
        return ds1;
    }
    

}