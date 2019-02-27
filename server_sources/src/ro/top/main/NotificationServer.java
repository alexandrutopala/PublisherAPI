package ro.top.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.gui.ServerFrame;
import ro.top.service.NotificationController;

/**
 * The main class for push notification server api.
 * If you are integrating this api, then make sure you will always
 * end your app using System.exit(). Otherwise, the NotificationServer 
 * will never stop the RMI processes.
 * 
 * If you are using this api as a standalone with gui, then you don't 
 * have to concerned about anything
 * @author Alexandru Topala
 */
public final class NotificationServer {
    public final static int DEFAULT_PORT = NotificationController.DEFAULT_SERVER_PORT;
    
    private NotificationServer(){        
    }
    
    /**
     * Starts the Notification Server on this localhost, on the specified PORT
     * @param port where to start the server process
     * @param runWithGui tells whether or not the server should start its gui. <br>
     * It is highly recommended to set this to true if you are not integrating this jar
     * into another application
     */
    public static void start(int port, boolean runWithGui) {
        start(port, runWithGui, null, null);
    }
    
    /**
     * Obtains and filters the ip addresses of all network interfaces and returns 
     * the right local ip address (starting with "192.") 
     * @return the local ip address of this host
     * @throws SocketException 
     */
    public static String getLocalIp() throws SocketException {
        String ip = "";
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while(interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if(iface.isLoopback() || !iface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                ip = addr.getHostAddress();
                if(ip.startsWith("192")) {
                    return ip;
                }
            }
        }
        return ip;
    }
    
    /**
     * Starts the Notification Server on this localhost, on the specified PORT
     * @param port where to start the server process
     * @param runWithGui tells whether or not the server should start its gui. <br>
     * It is highly recommended to set this to true if you are not integrating this jar
     * into another application
     * @param redirectOut redirect the standard output stream
     * @param redirectErr redirect the standard error stream
     */
    public static void start(int port, boolean runWithGui, OutputStream redirectOut, OutputStream redirectErr) {        
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(new MainTask(port, runWithGui, redirectOut, redirectErr));
        service.shutdown();
    }
    
    /**
     * Quietly stops the server, without notifying any subscriber
     */
    public static void stop() {
        NotificationController.getInstance().stopServer(false, null);
    }
    
    /**
     * Stops the server and notifies all the active subscribers with th specified data
     * @param data data to be send to all subscribers
     */
    public static void stop(Object data) {
        NotificationController.getInstance().stopServer(true, data);
    }
    
    private static class MainTask implements Runnable {
        private final boolean runWithGui;
        private final int port;
        private final OutputStream redirectOut;
        private final OutputStream redirectErr;
        
        public MainTask(int port, boolean runWithGui, OutputStream redirectOut, OutputStream redirectErr) {
            this.port = port;
            this.runWithGui = runWithGui;
            this.redirectOut = redirectOut;
            this.redirectErr = redirectErr;
        }
        
        @Override
        public void run() {            
            if (runWithGui) {
                try {
                    System.setProperty("java.rmi.server.hostname", getLocalIp());
                } catch (SocketException ex) {
                    Logger.getLogger(NotificationServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                ServerFrame frame = new ServerFrame(port, redirectOut, redirectErr);
            }
            NotificationController.getInstance(port);
        }        
    }
    
    
    
}
