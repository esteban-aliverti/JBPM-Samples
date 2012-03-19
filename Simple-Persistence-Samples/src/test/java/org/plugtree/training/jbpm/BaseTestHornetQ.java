package org.plugtree.training.jbpm;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;
import org.drools.compiler.ProcessBuilderFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.process.ProcessRuntimeFactory;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.jbpm.process.builder.ProcessBuilderFactoryServiceImpl;
import org.jbpm.process.instance.ProcessRuntimeFactoryServiceImpl;
import org.jbpm.task.Group;
import org.jbpm.task.User;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.TaskServer;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.jms.JMSTaskClientConnector;
import org.jbpm.task.service.jms.JMSTaskClientHandler;
import org.jbpm.task.service.jms.JMSTaskServer;
import org.jnp.server.Main;
import org.jnp.server.NamingBeanImpl;
import org.junit.After;
import org.junit.Before;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.jbpm.task.service.AsyncTaskServiceWrapper;
import org.jbpm.task.service.TaskServiceSession;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExpressionCompiler;
import org.plugtree.training.jbpm.mock.MockUserInfo;

/**
 * Base test to setup a JMS enviroment, to use {@link JMSTaskServer} and
 * {@link JMSTaskClientConnector}.
 *
 * @author calcacuervo
 *
 */
public abstract class BaseTestHornetQ {

    public final static String PROCESSES_PKG_KEY = "processes";
    protected TaskServer taskServer;
    protected TaskService taskService;
    protected TaskServiceSession taskSession;
    private Context context;
    private KnowledgeRuntimeLogger fileLogger;
    protected AsyncTaskServiceWrapper taskClient;

    static {
        ProcessBuilderFactory.setProcessBuilderFactoryService(new ProcessBuilderFactoryServiceImpl());
        ProcessRuntimeFactory.setProcessRuntimeFactoryService(new ProcessRuntimeFactoryServiceImpl());
    }
    private PoolingDataSource ds1;
    protected EntityManagerFactory emf;
    private EntityManagerFactory emfTask;
    
    protected Map<String, User> users;
    protected Map<String, Group> groups;

    @Before
    public void setUp() throws Exception {
        this.startHornet();
        // Compiles and persists all the .bpmn resources
        ds1 = new PoolingDataSource();
        ds1.setUniqueName("jdbc/testDS1");
        ds1.setClassName("org.h2.jdbcx.JdbcDataSource");
        ds1.setMaxPoolSize(3);
        ds1.setAllowLocalTransactions(true);
        ds1.getDriverProperties().put("user", "sa");
        ds1.getDriverProperties().put("password", "sasa");
        ds1.getDriverProperties().put("URL", "jdbc:h2:mem:mydb");
        ds1.init();

        System.setProperty("java.naming.factory.initial",
                "bitronix.tm.jndi.BitronixInitialContextFactory");
        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");

        emfTask = Persistence.createEntityManagerFactory("org.jbpm.task");
        taskService = new TaskService(emfTask,
                SystemEventListenerFactory.getSystemEventListener());
        taskSession = taskService.createSession();
        MockUserInfo userInfo = new MockUserInfo();

        taskService.setUserinfo(userInfo);

        this.fillUsersAndGroups(taskSession);

        Properties serverProperties = new Properties();
        serverProperties.setProperty("JMSTaskServer.connectionFactory",
                "XAConnectionFactory");
        serverProperties.setProperty("JMSTaskServer.transacted", "true");
        serverProperties.setProperty("JMSTaskServer.acknowledgeMode",
                "AUTO_ACKNOWLEDGE");
        serverProperties.setProperty("JMSTaskServer.queueName", "tasksQueue");
        serverProperties.setProperty("JMSTaskServer.responseQueueName",
                "tasksResponseQueue");
        System.setProperty("java.naming.factory.initial",
                "org.jnp.interfaces.NamingContextFactory");
        Context ctx = null;
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException("Could not start initial context", e);
        }

        taskServer = new JMSTaskServer(taskService, serverProperties, ctx);

        Thread thread = new Thread(taskServer);
        thread.start();
        System.out.println("Waiting for the HornetQTask Server to come up");
        while (!taskServer.isRunning()) {
            System.out.print(".");
            Thread.sleep(50);
        }

        taskClient = new AsyncTaskServiceWrapper(this.getTaskClientInstance());
        taskClient.connect("127.0.0.1", 5446);

    }

    @After
    public void tearDown() throws Exception {

        if (this.fileLogger != null) {
            this.fileLogger.close();
        }

        if (emf != null) {
            emf.close();
        }
        // if (emfTask != null) {
        // emfTask.close();
        // }
        if (ds1 != null) {
            ds1.close();
        }

        taskClient.disconnect();
        taskServer.stop();
    }

    private void startHornet() {
        try {
            Configuration configuration = new ConfigurationImpl();
            configuration.setPersistenceEnabled(false);
            configuration.setSecurityEnabled(false);
            TransportConfiguration connectorConfig = new TransportConfiguration(
                    NettyConnectorFactory.class.getName());
            configuration.getAcceptorConfigurations().add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
            HornetQServer hornetqServer = HornetQServers.newHornetQServer(configuration);

            System.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            NamingBeanImpl naming = new NamingBeanImpl();
            naming.start();
            Main jndiServer = new Main();
            jndiServer.setNamingInfo(naming);
            jndiServer.setPort(1099);
            jndiServer.setBindAddress("localhost");
            jndiServer.setRmiPort(1098);
            jndiServer.setRmiBindAddress("localhost");
            jndiServer.start();

            JMSConfiguration jmsConfig = new JMSConfigurationImpl();

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            env.put("java.naming.provider.url", "jnp://localhost:1099");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
            Context context = new InitialContext(env);
            jmsConfig.setContext(context);
            configuration.getConnectorConfigurations().put("connector",
                    connectorConfig);


            // Step 3. Configure the JMS ConnectionFactory
            ArrayList<String> connectorNames = new ArrayList<String>();
            connectorNames.add("connector");

            ConnectionFactoryConfiguration cfConfig = new ConnectionFactoryConfigurationImpl(
                    "XAConnectionFactory", false, connectorNames,
                    "XAConnectionFactory");
            jmsConfig.getConnectionFactoryConfigurations().add(cfConfig);
            JMSQueueConfiguration queueConfig = new JMSQueueConfigurationImpl(
                    "tasksQueue", null, false, "/queue/tasksQueue");
            JMSQueueConfiguration queueConfig2 = new JMSQueueConfigurationImpl(
                    "tasksResponseQueue", null, false,
                    "/queue/tasksResponseQueue");
            jmsConfig.getQueueConfigurations().add(queueConfig);
            jmsConfig.getQueueConfigurations().add(queueConfig2);
            JMSServerManager jmsServer = new JMSServerManagerImpl(hornetqServer, jmsConfig);
            jmsServer.start();

            ConnectionFactory cf = (ConnectionFactory) context.lookup("XAConnectionFactory");

        } catch (Exception e) {
            throw new RuntimeException(
                    "There was an error when starting hornetq server", e);
        }
    }

    private void fillUsersAndGroups(TaskServiceSession session) throws IOException {
        Reader reader = null;
        Map vars = new HashMap();
        try {
            reader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("LoadUsers.mvel"));
            users = (Map<String, User>) eval(reader, vars);
        } finally {
            if (reader != null) {
                reader.close();
            }
            reader = null;
        }

        try {
            reader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("LoadGroups.mvel"));
            groups = (Map<String, Group>) eval(reader, vars);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        for (User user : users.values()) {
            taskSession.addUser(user);
        }
        
        for (Group group : groups.values()) {
            taskSession.addGroup(group);
        }
    }

    private TaskClient getTaskClientInstance() {
        Properties clientProperties = new Properties();

        // Here we set the JMS connection properties.
        clientProperties.setProperty("JMSTaskClient.connectionFactory",
                "XAConnectionFactory");
        clientProperties.setProperty("JMSTaskClient.transactedQueue", "true");
        clientProperties.setProperty("JMSTaskClient.acknowledgeMode",
                "AUTO_ACKNOWLEDGE");
        clientProperties.setProperty("JMSTaskClient.queueName", "tasksQueue");
        clientProperties.setProperty("JMSTaskClient.responseQueueName",
                "tasksResponseQueue");
        System.setProperty("java.naming.factory.initial",
                "org.jnp.interfaces.NamingContextFactory");
        Context ctx = null;
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        TaskClient client = new TaskClient(new JMSTaskClientConnector(
                "testConnector", new JMSTaskClientHandler(
                SystemEventListenerFactory.getSystemEventListener()),
                clientProperties, ctx));
        return client;

    }
    
    public Object eval(Reader reader,
                       Map vars) {
        try {
            return eval(toString(reader),
                    vars);
        } catch (IOException e) {
            throw new RuntimeException("Exception Thrown",
                    e);
        }
    }
    
    public String toString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        int charValue;

        while ((charValue = reader.read()) != -1) {
            sb.append((char) charValue);
        }
        return sb.toString();
    }

    public Object eval(String str, Map vars) {
        ExpressionCompiler compiler = new ExpressionCompiler(str.trim());

        ParserContext context = new ParserContext();
        context.addPackageImport("org.jbpm.task");
        context.addPackageImport("org.jbpm.task.service");
        context.addPackageImport("org.jbpm.task.query");
        context.addPackageImport("java.util");

        vars.put("now", new Date());
        return MVEL.executeExpression(compiler.compile(context), vars);
    }
}