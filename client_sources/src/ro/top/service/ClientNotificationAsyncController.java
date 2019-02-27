/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.top.service;

import java.lang.reflect.Field;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.exception.UnreachableServerException;
import ro.top.subscriber.Subscriber;

/**
 *
 * Basically, the asynchronous version of ClientNotificationController class. 
 * Both of them can be used simultaneously for initialization and posting notification
 * without causing any problem
 * @author Alexandru Topala
 */
public class ClientNotificationAsyncController implements AutoCloseable {
    private static String DEFAULT_SERVER_SOCKET;
    private static int DEFAULT_CLIENT_PORT;
    private static ClientNotificationAsyncController singleton;
    private ExecutorService service;
    private Future<ClientNotificationController> futureController;
    private ClientNotificationController controller;
    
    static {
        DEFAULT_CLIENT_PORT = ClientNotificationController.DEFAULT_PORT;
        DEFAULT_SERVER_SOCKET = ClientNotificationController.serverSocket;
    }
    
    private ClientNotificationAsyncController(final String serverSocket, final int port) {
        service = Executors.newCachedThreadPool();
        futureController = service.submit(() -> ClientNotificationController.getInstance(serverSocket, port));
    }
    
    public static ClientNotificationAsyncController getInstance(String serverSocket, int port) {
        if (singleton == null) {
            synchronized (ClientNotificationAsyncController.class) {
                if (singleton == null) {
                    singleton = new ClientNotificationAsyncController(serverSocket, port);
                }
            }
        }
        return singleton;
    }
    
    public static String getLocalIp() throws SocketException {
        return ClientNotificationController.getLocalIp();
    }
    
    public static ClientNotificationAsyncController getInstance(String serverSocket) {
        return getInstance(serverSocket, DEFAULT_CLIENT_PORT);
    }
    
    public static ClientNotificationAsyncController getInstance() {
        return getInstance(DEFAULT_SERVER_SOCKET, DEFAULT_CLIENT_PORT);
    }
    
    private void ensureControllerInstanceExists() {
        if (controller != null) {
            return;
        }
        
        try {
            controller = futureController.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Logger.getLogger(ClientNotificationAsyncController.class.getName()).log(Level.SEVERE, null, ex);
            throw new UnreachableServerException();
        }
    }
    
    public void postNotification(String topicName) {
        ensureControllerInstanceExists();
        service.submit(() -> controller.postNotification(topicName));
    }
    
    public void postDataNotification(Object data, String topicName) {
        ensureControllerInstanceExists();
        service.submit(() -> controller.postDataNotification(data, topicName));
    }
    
    public Future<Boolean> addSubscriber(String topicName, Subscriber s) throws Exception {
        ensureControllerInstanceExists();
        return service.submit(() -> controller.addSubscriber(topicName, s));
    }
    
    public Future<Boolean> removeSubscriber(String topicName, Subscriber s)  {
        ensureControllerInstanceExists();
        return service.submit(() -> controller.removeSubscriber(topicName, s));
    }
    
    public Future<Boolean> exists(String topicName) { 
        ensureControllerInstanceExists();
        return service.submit(() -> controller.exists(topicName));
    }
    
    public Future<Integer> getSubscribersCountForTopic(String topicName) {
        ensureControllerInstanceExists();
        return service.submit(() -> controller.getSubscribersCountForTopic(topicName));
    }
    
    public void deleteTopic(String topicName) {
        ensureControllerInstanceExists();
        service.submit(() -> controller.deleteTopic(topicName));
    }
    
    public void deleteTopic(String topicName, boolean notifySubscribers) {
        ensureControllerInstanceExists();
        service.submit(() -> controller.deleteTopic(topicName, notifySubscribers));
    }
    
    public void deleteTopic(String topicName, Object data) {
        ensureControllerInstanceExists();
        service.submit(() -> controller.deleteTopic(topicName, data));
    }
    
    @Override
    public void close() {
        if (controller != null) {
            controller.close();
        }
        service.shutdown();
    }
}
