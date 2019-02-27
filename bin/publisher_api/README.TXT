====== Instructions for using the push notification api ======

-> for the server side
	You can either run the NotificationServer.jar as a stand-alone application with a gui with all the information about the server or you can intergrate this jar in your app, and use NotificationServer class to start or stop the server.
(Read class description for more details)

-> for client side 
	Integrate in your app the NotificationClient.jar library and use ClientNotificationController and ClientNotificationAsyncController for posting notification login and Subscriber interface for defining a subscriber.

====== IMPORTANT NOTE ======
-> for better performance, on both server and client side, before initializing the API, write this line of code: 
	+ for client : "System.setProperty("java.rmi.server.hostname", ClientNotificationController.getLocalIp());"
	+ for server : "System.setProperty("java.rmi.server.hostname", NotificationServer.getLocalIp());"