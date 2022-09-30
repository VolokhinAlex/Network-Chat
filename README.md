Network Chat in Java
===
A project that was made during the Java training.
****
Network chat consists of 2 parts Client and Server

# Client 
* Ability to view the last 100 messages.
* Sending private messages and regular messages to users.
* The ability to change the nickname.
**

# Server 
* Saving server logs and client logs to files.
* Saving users and messages in the database.
* Displaying events on the server via the graphical interface.
* Starting and stopping the server from the server GUI
* There is user authorization on the server.
**

# Logging
* Logging occurs both in the server part and in the client part.
The logs directory is created on the server
Which stores Client logs and Server logs.

# Authorization
The client's connection to the chat is carried out due to the authorization data that the user enters when connecting.
When connecting, all data is checked, and there cannot be more than 1 client with the same nickname on the server.

# Database management system
The Sqlite DBMS is used to save and manage information in the program.
* Users are saved in the database.
* User messages are stored in the database.

# Image
![Client-1](https://user-images.githubusercontent.com/89124134/193273089-011f3d30-8ede-4e22-8859-d878da2b9f66.png) 
![Client-2](https://user-images.githubusercontent.com/89124134/193273098-3f002eb5-1cc4-48c2-8715-0d82ec0413b6.png)
![Server_IMG](https://user-images.githubusercontent.com/89124134/193273130-6594f181-c692-4078-b066-99007220cc39.png)

 
