# Instant-Messaging-Server-Client
Instant messaging app using Java. Server/Client app

## Server
The server has the following responsibilities -
User Authentication - When a new client requests for a connection, the server should prompt the user to input the username and password and authenticate the user. The valid username and password combinations will be stored in a file called credentials.txt which will be in the same directory as the server program. An example credential.txt file is provided on the assignment page. Username and passwords are case-sensitive. We may use a different file for testing so DO NOT hardcode this information in your program. You may assume that each user name and password will be on a separate line and that there will be one white space between the two. If the credentials are correct, the client is considered to be logged in (i.e. online) and a welcome message is displayed. When all messaging is done, a user should be able to logout from the server.

On entering invalid credentials, the user is prompted to retry. After 3 consecutive failed attempts, the user is blocked for a duration of block_duration seconds (block_duration is a command line argument supplied to the server), and cannot login during this duration (even from another IP address). While a user is online, if someone uses the same username/password to log in (even from another IP address), then this new login attempt is denied. If the username is invalid then block access from that IP address for block_duration. 

Timeout - The server should keep track of all online users. If the server does not receive any commands from a user for a period of timeout seconds (timeout is a command line argument supplied to the server), then the server should automatically log this user out. Note that, to be considered active, a user must actively issue a command. The receipt of a message does not count.

Presence Broadcasts - The server should notify the presence/absence of other users logged into the server, i.e. send a broadcast notification to all online users when a user logs in and logs out.

List of online users - The server should provide a list of users that are currently online in response to such a query from a user.

Online history – The sever should provide a list of users that logged in for a user specified time in the past (e.g. users who logged in within the past 15 minutes).

Message Forwarding - The server should forward each instant message to the correct recipient assuming they are online.

Offline Messaging - When the recipient of a message is not logged in (i.e. is offline), the message will be saved by the server. When the recipient logs in next, the server will send all the unread messages stored for that user (timestamps are not required).

Message Broadcast – The server should allow a user to broadcast a message to all online users. Offline messaging is not required for broadcast messages.

Blacklisting - The server should allow a user to block / unblock any other user. For example, if user A has blocked user B, B can no longer send messages to A i.e. the server should intercept such messages and inform B that the message cannot be forwarded. Blocked users also do not get presence notifications i.e. B will not be informed each time A logs in or logs out.  
