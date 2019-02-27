
package ro.top.service;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.exception.UninitializedNotificationServerException;
import ro.top.exception.UnregistredUserException;
import ro.top.proxy.ClientNotificationProxy;

/**
 *
 * @author Alexandru Topala
 */
public final class NotificationController {
    private static final String NOTIFICATION_SERVER_NAME = "PushNotificationServerTop";
    private static final String CLIENT_NAME_PREFIX = "ClientNotificationServer";
    private static NotificationController singleton;
    private static long idGenerater = 0;
    public static final int DEFAULT_SERVER_PORT = 4444;
    public final int PORT;
   
    
    /**
     * key - the auto-generated client id
     * value - the proxy of key client
     */
    private final Map<String, Optional<ClientNotificationProxy>> clientsRegistry;
    
    /**
     * key - the auto-generated client id
     * value - the client socket
     */
    private final Map<String, String> clientsSocket;
    
    /**
     * key - the topic
     * value - a list formed the clients ids that subscribed to key topic
     */
    private final Map<String, List<String>> clientsSubscriptions;
    
    /**
     * A queue with clients that unexpectedly closed the connection with the server
     * The clients from this queue will be deleted from the server at every DELETE_INTERVAL seconds
     */
    private final static int DELETE_INTERVAL = 30;
    private final Set<String> lostClients;
    private final ScheduledExecutorService lostClientsRemover;
    
    /**
     * The executor that asynchronously notify the clients 
     */
    private ExecutorService notificationPusher;
    
    private NotificationController(int port) {
        try {
            this.PORT = port;
            init(port);
        } catch(MalformedURLException | RemoteException e) {
            e.printStackTrace();
            throw new UninitializedNotificationServerException();
        }
        clientsRegistry = new ConcurrentHashMap<>();
        clientsSocket = new ConcurrentHashMap<>();
        clientsSubscriptions = new ConcurrentHashMap<>();
        lostClients = new HashSet<>();
        
        notificationPusher = Executors.newCachedThreadPool();
        
        lostClientsRemover = Executors.newScheduledThreadPool(4);
        lostClientsRemover.scheduleWithFixedDelay(() -> deleteAllLostClients(), 
                DELETE_INTERVAL,
                DELETE_INTERVAL,
                TimeUnit.SECONDS
        );
    }
    
    /**
     * Initialize the notification server using the given PORT
     * @param port - where to start the server process on this localhost
     * @return singleton
     */
    public static NotificationController getInstance(int port) {
        if (singleton == null) {
            synchronized(NotificationController.class) {
                if (singleton == null) {
                    singleton = new NotificationController(port);
                }
            }
        }
        return singleton;
    }
    
    
    /**
     * Initialize the notification server using the default PORT = 4444
     * @return singleton
     */
    public static NotificationController getInstance() {
        return getInstance(DEFAULT_SERVER_PORT);
    }
        
    private void init(int port) throws RemoteException, MalformedURLException {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry("localhost", port);
        }
        registry.rebind(NOTIFICATION_SERVER_NAME, new NotificationService());
        Logger.getLogger(NotificationController.class.getName())
                .log(Level.INFO,"Server registry named " + NOTIFICATION_SERVER_NAME + " started successfully at {0}", port);
        //Naming.rebind("rmi://localhost" + PORT + "/" + NOTIFICATION_SERVER_NAME, new NotificationService());
    }    
    
    String registerClient(String clientSocket) {
        String clientName = CLIENT_NAME_PREFIX + idGenerater++;
        clientsRegistry.put(clientName, Optional.empty());
        clientsSocket.put(clientName, clientSocket);
        return clientName;
    }
    
    void unregisterClient(String clientName) {
        clientsRegistry.remove(clientName);
        clientsSocket.remove(clientName);
        clientsSubscriptions.forEach((k, v) -> v.removeIf(s -> s.equals(clientName)));
    }
    
    void subscribe(String topicName, String clientId) throws UnregistredUserException, NotBoundException, MalformedURLException, MalformedURLException, RemoteException {
        if (clientsRegistry.get(clientId) == null) {
            throw new UnregistredUserException();
        }
        Optional<ClientNotificationProxy> optionalProxy = clientsRegistry.get(clientId);
        if (!optionalProxy.isPresent()) {
            String clientSocket = clientsSocket.get(clientId);
            optionalProxy = Optional.of(getProxy(clientSocket, clientId));
            clientsRegistry.put(clientId, optionalProxy);
        }
        
        if (!clientsSubscriptions.containsKey(topicName)) {
            clientsSubscriptions.put(topicName, new CopyOnWriteArrayList<>());
        }
        
        if (!clientsSubscriptions.get(topicName).contains(clientId)) {
            clientsSubscriptions.get(topicName).add(clientId);
            // TODO: remove these
            System.out.println(clientId + " vrea si el sa stie"); // DEBUG
        }
    }
    
    void unsubscribe(String topicName, String clientId) {
        if (!clientsSubscriptions.containsKey(topicName)) {
            return;
        }
        
        clientsSubscriptions.get(topicName).remove(clientId);
        if (clientsSubscriptions.get(topicName).isEmpty()) {
            clientsSubscriptions.remove(topicName);
        }
    }
    
    void deleteTopic(String topicName, boolean notifySubscribers, Object data) {
        if (!clientsSubscriptions.containsKey(topicName)) {
            return;
        }
        
        if (!notifySubscribers) {
            clientsSubscriptions.remove(topicName);
            return;
        }
        
        List<Callable<String>> tasks = new ArrayList<>();
        
        clientsSubscriptions.get(topicName).forEach(cid -> {
            clientsRegistry.get(cid).ifPresent(proxy -> {
                tasks.add(() -> {
                    try {   
                       if (data == null)
                           proxy.receiveNotification(topicName); 
                       else 
                           proxy.receiveDataNotification(data, topicName);
                    } catch (RemoteException e) {
                        Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, 
                                           "Connection with client " + cid + " was unexpectedly closed. Client will be removed...", 
                                           e.getMessage());

                       synchronized (NotificationController.class) {
                           lostClients.add(cid);
                       }
                       return "fail";
                    }
                    return "success";
                });
            });
        });
        
        try {
            notificationPusher.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, "Clients were not notified", ex);
            
        }
    }
    
    boolean exists(String topicName) {
        return clientsSubscriptions.containsKey(topicName);
    }
    
    void notifyTopic(String topicName) {
        if (!exists(topicName)) {
            return;
        }
        
        List<Callable<String>> tasks = new ArrayList<>();
        
        clientsSubscriptions.get(topicName).forEach(
            cid -> clientsRegistry.get(cid).ifPresent(
                proxy ->  {
                    tasks.add(() -> {
                        try {
                            //TODO : remove those
                            proxy.receiveNotification(topicName);
                            System.out.println("Client " + cid + " was notified...successfully"); // DEBUG
                        } catch (Exception ex) {
                            System.out.println("Client " + cid + " was notified...not"); // DEBUG
                            Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, 
                                    "Connection with client " + cid + " was unexpectedly closed. Client will be removed...", 
                                    ex.getMessage());

                            synchronized (NotificationController.class) {
                                lostClients.add(cid);                                    
                            }
                            return "fail";
                        }
                        return "success";
                    });
                }
            )
        );
        
        try {
            notificationPusher.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, "Clients were not notified", ex);
        }
    }
    
    void dataNotifyTopic(Object data, String topicName) {
        if (!exists(topicName)) {
            return;
        }
        
        List<Callable<String>> tasks = new ArrayList<>();
        
        clientsSubscriptions.get(topicName).forEach(
            cid -> clientsRegistry.get(cid).ifPresent(
                proxy -> {
                    tasks.add(() -> {
                        try {
                            // TODO: remove those
                            proxy.receiveDataNotification(data, topicName);
                            System.out.println("Client " + cid + " was notified...successfully"); // DEBUG
                        } catch (RemoteException ex) { 
                            System.out.println("Client " + cid + " was notified...not"); // DEBUG
                            Logger.getGlobal().log(Level.SEVERE, 
                                    "Connection with client " + cid + " was unexpectedly closed. Client will be removed...", 
                                    ex.getMessage());
                            synchronized (NotificationController.class) {
                                lostClients.add(cid);
                            }
                            return "fail";
                        }
                        return "success";
                    });
                }
            )
        );
        
        try {
            notificationPusher.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, "Clients were not notified", ex);
        }
    }
    
    int getSubscribersCountForTopic(String topicName) {
        if (!exists(topicName)) {
            return 0;
        }
        return clientsSubscriptions.get(topicName).size();
    }
    
    public void stopServer(boolean notifySubscribers, Object data) {
        clientsSubscriptions.keySet().forEach(topicName -> deleteTopic(topicName, notifySubscribers, data));
        clientsSubscriptions.clear();
        clientsSocket.clear();
        clientsRegistry.clear();
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", PORT);
            registry.unbind(NOTIFICATION_SERVER_NAME);
            System.out.println(Arrays.asList(registry.list()));
            
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(NotificationController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        singleton = null;
        
        Logger.getGlobal().info("Top's Notification Server closed");
    }
    
    private ClientNotificationProxy getProxy(String clientSocket, String clientId) throws NotBoundException, MalformedURLException, RemoteException {
        String [] socket = clientSocket.split(":");
        String host = socket[0];
        int port = Integer.parseInt(socket[1]);
        Registry registry = LocateRegistry.getRegistry(host, port);
        return (ClientNotificationProxy) registry.lookup(clientId);
        //return (ClientNotificationProxy) Naming.lookup("rmi://" + clientSocket + "/" + clientId);
    }
    
    private void deleteAllLostClients() {
        synchronized (NotificationController.class) {
            if (lostClients.isEmpty()) {
                return;
            }
            lostClients.forEach(cid -> {
                Logger.getGlobal().log(Level.INFO, "Client {0} : {1} it is now deleted", new Object[]{cid, clientsSocket.get(cid)});
                unregisterClient(cid);
            });
            lostClients.clear();
        }
    }
}
