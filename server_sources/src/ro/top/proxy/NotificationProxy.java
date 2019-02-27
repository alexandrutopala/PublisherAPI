
package ro.top.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import ro.top.exception.TopicDoesNotExistException;

/**
 * This proxy expose the basic operations for manipulating 
 * a topic 
 * @author Alexandru Topala
 */
public interface NotificationProxy extends Remote {
    
    /**
     * Registers the calling client, allocating an auto-generate clientId through which 
     * this server will reach the client in the future. 
     * @param clientSocket 
     * @return clientId - the auto-generated client id
     * @throws java.rmi.RemoteException
     */
    public String registerClient(String clientSocket) throws RemoteException;
    
    /**
     * Unregisters the client identified by the given client id
     * @param clientId 
     * @throws java.rmi.RemoteException 
     */
    public void unregisterClient(String clientId)  throws RemoteException;
    
    
    /**
     * Subscribe the client to the specified topic.
     * If the topic does not exist, a new one is create
     * @param name the name of the topic
     * @param clientId
     * @throws Exception
     * @throws java.rmi.RemoteException
     */
    public void subscribe(String name, String clientId) throws Exception, RemoteException;
    
    /**
     * Unsubscribe the client from the specified topic.
     * If the topic does not exists, nothing happens
     * @param name the name of the topic
     * @param clientId
     * @throws java.rmi.RemoteException
     */
    public void unsubscribe(String name, String clientId) throws RemoteException;
    
    /**
     * Deletes the specified topic, without notifying the subscribers 
     * that they were unsubscribed
     * @param nume the name of the topic
     * @throws java.rmi.RemoteException
     * @throws TopicDoesNotExistException - if the topic does not exist
     */
    public void deleteTopic(String nume) throws RemoteException;
    
    /**
     * Deletes the specified topic, and may be notifying all the subscribers 
     * that they were unsubscribed
     * @param nume the name of the topic
     * @param notifySubscribers
     * @throws java.rmi.RemoteException
     * @throws TopicDoesNotExistException - if the topic does not exist
     */
    public void deleteTopic(String nume, boolean notifySubscribers) throws RemoteException;
    
    /**
     * Deletes the specified topic, notifying all the subscribers with the specified data
     * that they were unsubscribed
     * @param nume the name of the topic
     * @param data data for the subscribers
     * @throws java.rmi.RemoteException
     * @throws TopicDoesNotExistException - if the topic does not exist
     */
    public void deleteTopic(String nume, Object data) throws RemoteException;
    
    /**
     * Notifies all the subscribers of this topic that something happened
     * @param name - the name of the topic
     * @throws java.rmi.RemoteException
     * @throws TopicDoesNotExistException
     */
    public void notifyTopic(String name) throws RemoteException;
    
    /**
     * Send this data to all subscribers of specified topic
     * @param data - the data to be sent to the listeners
     * @param name - the name of the topic
     * @throws java.rmi.RemoteException
     * @throws TopicDoesNotExistException
     */
    public void dataNotifyTopic(Object data, String name) throws RemoteException;
    
    /**
     * Tests if the specified topic exists
     * @param topicName - the name of the topic
     * @return true - if the topic exists, false otherwise
     * @throws java.rmi.RemoteException
     */
    public boolean exists(String topicName) throws RemoteException;
    
    /**
     * 
     * @param name - the name of the topic
     * @return the subscribers count of the specified topic
     * @throws java.rmi.RemoteException
     */
    public int getSubscribersCount(String name) throws RemoteException;
    
}
