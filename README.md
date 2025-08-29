# SEP2 – Chat app

Semester Project 2 *(SEP2)* was about building a client/server application using JavaFX and either Sockets or RMI. We must also implement a relational database.

In my group, we decided to build a chat application with functionality to write direct messages between two users, but also allow users to create group chats and invite other users to their group, so that multiple users can chat together.

In a group chat, each participant is assigned a role. By default, a participant is assigned the `MEMBER` role when joining a group. A participant with the `MEMBER` role can be promoted to `ADMIN` by either the `CREATOR` or another `ADMIN`. However, only the `CREATOR` can demote an `ADMIN` to a normal `MEMBER`. The user who created the group is assigned the `CREATOR` role, which is a permanent role.

## Requirements

### Functional

**Create account:** As a user, I want to create an account, so that I can log in and send messages.

**Login:** As a user, I want to log in with my username and password, so that I can access my messages.

**Logout:** As a user, I want to log out of my account, so that I can secure my session.

**Send message:** As a user, I want to send a message to another user, so that I can communicate with them.

**Receive messages:** As a user, I want to receive messages from other users in real-time, so that I can stay updated on conversations.

**Contact list:** As a user, I want to see a list of users I’ve had conversations with, so that I can quickly continue them.

**Start new conversation:** As a user, I want to initiate a conversation with a new user by entering their profile name or username, so that I can initiate conversations with people not in my contact list.

**Select active conversation:** As a user, I want to click on a contact to view our conversation, so that I can switch between different chats.

**Create group chat:** As a user, I want to create a group chat, so I have a dedicated space to communicate with multiple users.

**Invite users:** As a admin, I want to invite other users to a group chat, so they can join the conversation and collaborate with the group.

**Remove members:** As an admin, I want to remove members from a group chat, so that I can manage group size and keep discussions focused.

**Group chat roles:** As a user, I want group chat roles such as admin and members, so that admins can manage the group chat effectively.

**Message history:** As a user, I want to retrieve recent messages in a conversation, so that I can read what we’ve talked about.

**Message timestamp:** As a user, I want to see a timestamp on each message, so that I know when it was sent.

**Chronological ordering:** As a user, I want messages displayed from oldest to newest, so that I can follow the natural flow of conversation.

**Scroll through history:** As a user, I want to scroll up to see older messages in a conversation, so that I can review our complete chat history.

### Non-functional

Passwords must be hashed using a secure algorithm before being stored in the database.

Only authenticated users may send and receive messages.

Cross-platform support for Windows and macOS.

Application must follow MVVM pattern with clear separation between View, ViewModel, and Model layers.

Application must display all interface text and error messages in English.
