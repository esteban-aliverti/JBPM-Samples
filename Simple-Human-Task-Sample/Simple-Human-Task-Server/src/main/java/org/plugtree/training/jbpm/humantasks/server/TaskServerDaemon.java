package org.plugtree.training.jbpm.humantasks.server;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.SystemEventListenerFactory;
import org.jbpm.task.User;
import org.jbpm.task.service.TaskServer;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.TaskServiceSession;
import org.jbpm.task.service.mina.MinaTaskServer;
import org.plugtree.training.jbpm.humantasks.server.domain.MockUserInfo;


public class TaskServerDaemon {
    
    private boolean running;
    private TaskServer taskServer;
    
    public TaskServerDaemon() {
        this.running = false;
    }
    
    public void startServer() {
        if(isRunning())
            throw new IllegalStateException("Server is already started");
        this.running = true;
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("org.jbpm.task");
        TaskService taskService = new TaskService(entityManagerFactory, SystemEventListenerFactory.getSystemEventListener());
        TaskServiceSession taskSession = taskService.createSession() ;
        MockUserInfo userInfo = new MockUserInfo();
        taskService.setUserinfo( userInfo);
        
        for (String userName : getDefaultUsers()) {
            taskSession.addUser(new User(userName));
        }
        
	System.out.println("Starting Human Task Server");
        taskServer = new MinaTaskServer(taskService);
        Thread thread = new Thread(taskServer);
        thread.start();
    }

    public void stopServer() throws Exception {
        if(!isRunning())
            return;
        taskServer.stop();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private String[] getDefaultUsers() {
        return new String[]{"salaboy", "translator", "reviewer", "Administrator"};
    }
    
}
