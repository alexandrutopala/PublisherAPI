package ro.top.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The proxy through the server can send push-notifications to a client
 * @author Alexandru Topala
 */
public interface ClientNotificationProxy extends Remote {
    /**
     * Receive normal notification
     * @param name - the name of the topic
     * @throws java.rmi.RemoteException
     */
    public void receiveNotification(String name) throws RemoteException;
    
    /**
     * Receive a notification with data attached
     * @param data - notification data
     * @param name - the name of the topic
     * @throws java.rmi.RemoteException
     */
    public void receiveDataNotification(Object data, String name) throws RemoteException;
    
}
