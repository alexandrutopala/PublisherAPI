package ro.top.service;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.exception.UninitializedNotificationServerException;
import ro.top.proxy.NotificationProxy;
import ro.top.subscriber.Subscriber;

/**
 * The main class for client.
 * To prevent unexpected behaviors, make sure you make the very first use 
 * of this class by calling the getInstance(String serverSocket, int clientPort) method, 
 * that fully configures the client side
 * 
 * @since 1.8
 * @author Alexandru Topala
 */
public final class ClientNotificationController implements AutoCloseable {
    private static ClientNotificationController singleton;
    
    private static final String NOTIFICATION_SERVER_NAME = "PushNotificationServerTop";
    public static final int DEFAULT_PORT = 9999;
    public static final int DEFAULT_SERVER_PORT = 4444;
    public static String serverSocket = "localhost:" + DEFAULT_SERVER_PORT;
    
    private Map<String, Set<Subscriber>> topics;
    private static NotificationProxy proxy;
    
    private static String CLIENT_ID;
    private final int CLIENT_PORT;
    
    private ExecutorService notificationPusher;
    
    private ClientNotificationController(String serverSocket, int clientPort) {   
        CLIENT_PORT = clientPort;
        try {            
            init(serverSocket, clientPort);
        } catch (MalformedURLException | NotBoundException | RemoteException | UnknownHostException | SocketException e) {
            e.printStackTrace();
        } 
        
        if (proxy == null) {
            throw new UninitializedNotificationServerException();
        }
        topics = new HashMap<>();
        
        notificationPusher = Executors.newCachedThreadPool();
    }
    
    /**
     * Initialize the client and returns the singleton instance.
     * It is highly recommended to use this method for the very first use of this class  
     * 
     * @param serverSocket socket of the Notification Server under the forma "host:port"   
     * @param clientPort at which port should this client create its registry   
     * @return singleton     
    */
    public static ClientNotificationController getInstance(String serverSocket, int clientPort) {
        if (singleton == null) {
            synchronized(ClientNotificationController.class) {
                if (singleton == null) {
                    singleton = new ClientNotificationController(serverSocket, clientPort);
                }
            }
        }
        return singleton;
    }
    
    /**
     * Initialize the client with default configurations and returns the singleton instance.
     * It is highly recommended to use getInstance(String serverSocket, int clientPort) method for the very 
     * first use of this class
     * 
     * The default configurations are : <br>
     * - serverSocket : localhost:4444 <br>
     * - clientSocket : local_ip_address:9999
     *   
     * @return singleton     
    */
    public static ClientNotificationController getInstance() {        
        return getInstance(serverSocket, DEFAULT_PORT);
    }
    
    /**
     * Initialize the client with the given server socket and default client socket and returns the singleton instance.
     * It is highly recommended to use getInstance(String serverSocket, int clientPort) method for the very 
     * first use of this class. <br>
     * Server Socket format : "host:port" <br>
     * 
     * The default configurations are : 
     * - clientSocket : this_local_ip_address:9999
     *   
     * @param serverSocket
     * @return singleton     
    */
    public static ClientNotificationController getInstance(String serverSocket) {
        return getInstance(serverSocket, DEFAULT_PORT);
    }
    
    /**
     * 
     * @return the local ip address
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
     * Initialize the ClientNotification and establishes the
     * connection with NotificationServer, situated at the specified address
     * @param serverSocket - the socket of the NotificationServer application
     * @param clientSocket
     * @throws java.rmi.NotBoundException
     * @throws java.net.MalformedURLException
     * @throws java.rmi.RemoteException
     */
    private void init(String serverSocket, int clientPort) throws NotBoundException, MalformedURLException, RemoteException, UnknownHostException, SocketException {
        String clientSocket = getLocalIp() + ":" + clientPort;
        ClientNotificationController.serverSocket = serverSocket;
        
        String[] socket = serverSocket.split(":");
        String serverHost = socket[0];
        int serverPort = Integer.parseInt(socket[1]);
        
        
        proxy = (NotificationProxy) LocateRegistry.getRegistry(serverHost, serverPort).lookup(NOTIFICATION_SERVER_NAME);
        //proxy = (NotificationProxy) Naming.lookup("rmi://" + serverHost + "/" + NOTIFICATION_SERVER_NAME);
        CLIENT_ID = proxy.registerClient(clientSocket);
        
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(clientPort);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry("localhost", clientPort);
        }
        registry.rebind(CLIENT_ID, new ClientNotificationService());
        Logger.getGlobal().log(Level.INFO, "Created client registry {0} at {1}", new Object[]{CLIENT_ID, clientSocket});
        //Naming.rebind("rmi://" + clientSocket + "/" + CLIENT_ID, new ClientNotificationService());
    }  
        
    void receiveNotification(String topicName) {
        if (topics.containsKey(topicName)) {
            List<Callable<String>> tasks = new ArrayList<>();
           
            topics.get(topicName)
                    .forEach(s ->  { 
                        tasks.add(() -> { 
                            s.newNotification(topicName);
                            return "";
                        });                       
                    });
            
            try {
                notificationPusher.invokeAll(tasks);
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    void receiveDataNotification(Object data, String topicName) {
        if (topics.containsKey(topicName)) {
            List<Callable<String>> tasks = new ArrayList<>();
           
            topics.get(topicName)
                    .forEach(s ->  { 
                        tasks.add(() -> { 
                            s.newDataNotification(data, topicName);
                            return "";
                        });                       
                    });
            
            try {
                notificationPusher.invokeAll(tasks);
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * 
     * @return the id associated with this client 
     */
    public String getClientId() {
        return CLIENT_ID;
    }
    
    /**
     * Notifies all the subscribers for the specified topic
     * @param topicName 
     */
    public void postNotification(String topicName) {
        try {
            proxy.notifyTopic(topicName);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Notifies all the subscribers for the specified topic with the given data
     * @param data
     * @param topicName 
     */
    public void postDataNotification(Object data, String topicName) {
        try {
            proxy.dataNotifyTopic(data, topicName);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Subscribe the specified subscriber from the specified topic
     * @param topicName
     * @param s - the subscriber
     * @return true if the subscriber was added successfully, false otherwise
     * @throws java.lang.Exception
     */
    public boolean addSubscriber(String topicName, Subscriber s) throws Exception {
        synchronized (ClientNotificationController.class) {
            if (!topics.containsKey(topicName)) {
                proxy.subscribe(topicName, CLIENT_ID);            
                topics.put(topicName, new CopyOnWriteArraySet<>());
            }
        }
        //TODO: remove these
        System.out.println("Vrea si " + s + " sa stie"); // DEBUG
        return topics.get(topicName).add(s);
    }
    
    /**
     * Unsubscribe the specified subscriber from the specified topic
     * @param topicName
     * @param s the subscriber
     * @return true if the subscriber s does not listen anymore, false otherwise 
     */
    public boolean removeSubscriber(String topicName, Subscriber s)  {
        if (!topics.containsKey(topicName)) {
            return true;
        }
        boolean rez = topics.get(topicName).remove(s);
        if (topics.get(topicName).isEmpty()) {
            try {
                proxy.unsubscribe(topicName, CLIENT_ID);
            } catch (RemoteException ex) {
                Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
            }
            topics.remove(topicName);
        }
        return rez;
    }    
    
    /**
     * Tests if the specified topic exists
     * @param topicName
     * @return true if the topic exists, false otherwise
     */
    public boolean exists(String topicName) {
        try {
            return proxy.exists(topicName);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
            return false;            
        }
    }
    
    /**
     * Returns the current subscribers count for the specified topic
     * @param topicName
     * @return subscribers count
     */
    public int getSubscribersCountForTopic(String topicName) {
        try {
            return proxy.getSubscribersCount(topicName);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    /**
     * Quietly deletes the specified topic (without notifying any subscriber)
     * @param topicName 
     */
    public void deleteTopic(String topicName) {
        try {
            proxy.deleteTopic(topicName);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Deletes the specified topic, either notifying or not the current subscribers to that topic
     * @param topicName
     * @param notifySubscribers 
     */
    public void deleteTopic(String topicName, boolean notifySubscribers) {
        try {
            proxy.deleteTopic(topicName, notifySubscribers);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Deletes the specified topic, notifying all the subscribers with the given data
     * @param topicName
     * @param data 
     */
    public void deleteTopic(String topicName, Object data) {
        try {
            proxy.deleteTopic(topicName, data);
        } catch (RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void close() {
        //topics.forEach((k, v) -> v.forEach(s -> removeSubscriber(k, s)));        
        topics.clear();
        try {
            proxy.unregisterClient(CLIENT_ID);
            LocateRegistry.getRegistry(CLIENT_PORT).unbind(CLIENT_ID);
        } catch (NotBoundException | RemoteException ex) {
            Logger.getLogger(ClientNotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
        singleton = null;
        proxy = null;
        Logger.getGlobal().log(Level.INFO, "Client server {0} hopefully closed", new Object[]{CLIENT_ID});
    }
}
