package org.plugtree.training.jbpm.humantasks.server;

public class StartHumanTaskServer {

    public static void main(String[] args) {
        final TaskServerDaemon taskServerDaemon = new TaskServerDaemon();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() { 
                System.out.println("\n");
                try {
					taskServerDaemon.stopServer();
					System.out.println("server stoped...");
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
         });

        taskServerDaemon.startServer();
        System.out.println("server started... (ctrl-c to stop it)");
    }
}
