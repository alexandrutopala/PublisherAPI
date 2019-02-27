package ro.top.subscriber;

/**
 * The interface that describes a subscriber
 * @author Alexandru Topala
 */
public interface Subscriber {    
    /**
     * Called when somebody notified the topic specified by this name
     * @param name - the name of the topic
     */
    public void newNotification(String name);
    
    /**
     * Called when somebody notified the topic specified by this name
     * @param data - the data sent from the server
     * @param name - the name of the topic     * 
     */
    public void newDataNotification(Object data, String name);
}
