package via.sep2.client.viewmodel.chat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.EventListener;
import via.sep2.client.event.events.MessageDeletedEvent;
import via.sep2.client.event.events.MessageEditedEvent;
import via.sep2.client.event.events.MessageReceivedEvent;
import via.sep2.client.event.events.UserJoinedGroupEvent;
import via.sep2.client.event.events.UserLeftGroupEvent;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.AuthService;
import via.sep2.client.service.ChatService;
import via.sep2.client.view.chat.ChatItemData;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public class MainChatViewModel {

    private static final Logger logger = Logger.getLogger(
            MainChatViewModel.class.getName());

    private final AuthService authService;
    private final ChatService chatService;
    private final ConnectionManager connectionManager;

    private final StringProperty searchText = new SimpleStringProperty("");
    private final StringProperty messageInput = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty showUserSearchResults = new SimpleBooleanProperty(false);
    private final BooleanProperty showChatOptions = new SimpleBooleanProperty(
            false);
    private final StringProperty currentUserRole = new SimpleStringProperty("");

    private final ObservableList<ChatItemData> allChats = FXCollections.observableArrayList();
    private final FilteredList<ChatItemData> filteredChats = new FilteredList<>(
            allChats);
    private final ObservableList<UserDTO> searchedUsers = FXCollections.observableArrayList();
    private final ObservableList<MessageDTO> currentMessages = FXCollections.observableArrayList();

    private ChatItemData selectedChat;
    private ChatFilter currentFilter = ChatFilter.ALL;

    private final EventListener<MessageReceivedEvent> messageReceivedListener;
    private final EventListener<MessageEditedEvent> messageEditedListener;
    private final EventListener<MessageDeletedEvent> messageDeletedListener;

    private final EventListener<UserJoinedGroupEvent> userJoinedGroupListener;
    private final EventListener<UserLeftGroupEvent> userLeftGroupListener;

    public enum ChatFilter {
        ALL,
        DIRECT,
        GROUP,
    }

    public MainChatViewModel() {
        this.authService = ServiceFactory.getInstance().getService(
                AuthService.class);
        this.chatService = ServiceFactory.getInstance().getService(
                ChatService.class);
        this.connectionManager = ConnectionManager.getInstance();

        this.messageReceivedListener = this::handleMessageReceived;
        this.messageEditedListener = this::handleMessageEdited;
        this.messageDeletedListener = this::handleMessageDeleted;

        this.userJoinedGroupListener = this::handleUserJoinedGroup;
        this.userLeftGroupListener = this::handleUserLeftGroup;

        setupEventListeners();
        setupSearchFilter();
    }

    private void setupEventListeners() {
        connectionManager
                .getEventBus()
                .subscribe(MessageReceivedEvent.class, messageReceivedListener);
        connectionManager
                .getEventBus()
                .subscribe(MessageEditedEvent.class, messageEditedListener);
        connectionManager
                .getEventBus()
                .subscribe(MessageDeletedEvent.class, messageDeletedListener);

        connectionManager.getEventBus().subscribe(UserLeftGroupEvent.class, userLeftGroupListener);
        connectionManager.getEventBus().subscribe(UserJoinedGroupEvent.class, userJoinedGroupListener);
    }

    private void setupSearchFilter() {
        updateFilterPredicate();

        searchText.addListener((obs, oldVal, newVal) -> {
            updateFilterPredicate();
            handleSearchTextChange(newVal);
        });
    }

    private void updateFilterPredicate() {
        filteredChats.setPredicate(chatItem -> {
            String search = searchText.get();
            if (search == null)
                search = "";
            search = search.toLowerCase().trim();

            boolean matchesSearch = search.isEmpty() ||
                    chatItem.getName().toLowerCase().contains(search) ||
                    chatItem.getPreview().toLowerCase().contains(search);

            boolean matchesType = switch (currentFilter) {
                case DIRECT -> chatItem.getType() == ChatItemData.ChatType.DIRECT;
                case GROUP -> chatItem.getType() == ChatItemData.ChatType.GROUP;
                case ALL -> true;
            };

            return matchesSearch && matchesType;
        });
    }

    private void handleSearchTextChange(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            showUserSearchResults.set(false);
            searchedUsers.clear();
            return;
        }

        String trimmedSearch = searchText.trim();

        boolean hasMatchingChats = allChats
                .stream()
                .anyMatch(chat -> chat
                        .getName()
                        .toLowerCase()
                        .contains(trimmedSearch.toLowerCase()));

        if (!hasMatchingChats && trimmedSearch.length() >= 2) {
            searchForUsers(trimmedSearch);
        } else {
            showUserSearchResults.set(false);
            searchedUsers.clear();
        }
    }

    public void loadChats() {
        if (isLoading.get())
            return;

        isLoading.set(true);
        allChats.clear();

        CompletableFuture<Void> loadDirectChats = chatService
                .getDirectChatsAsync()
                .thenAccept(directChats -> Platform.runLater(() -> {
                    for (DirectChatDTO directChat : directChats) {
                        ChatItemData chatItem = createChatItemFromDirectChat(
                                directChat);
                        allChats.add(chatItem);
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setErrorMessage(
                            "Failed to load direct chats: " + throwable.getMessage()));
                    return null;
                });

        CompletableFuture<Void> loadGroupChats = chatService
                .getMyGroupChatsAsync()
                .thenAccept(groupChats -> Platform.runLater(() -> {
                    for (ChatRoomDTO groupChat : groupChats) {
                        ChatItemData chatItem = createChatItemFromGroupChat(
                                groupChat);
                        allChats.add(chatItem);
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setErrorMessage(
                            "Failed to load group chats: " + throwable.getMessage()));
                    return null;
                });

        CompletableFuture.allOf(loadDirectChats, loadGroupChats).whenComplete(
                (result, throwable) -> Platform.runLater(() -> {
                    isLoading.set(false);
                    if (throwable != null) {
                        logger.warning(
                                "Error loading chats: " + throwable.getMessage());
                    }
                }));
    }

    public CompletableFuture<ChatRoomDTO> createGroupChat(
            String name,
            String description,
            boolean isPrivate,
            int maxMembers) {
        return chatService
                .createGroupChatAsync(name, description, isPrivate, maxMembers)
                .thenApply(groupChat -> {
                    Platform.runLater(() -> {
                        ChatItemData newChatItem = createChatItemFromGroupChat(
                                groupChat);
                        allChats.add(0, newChatItem);
                    });
                    return groupChat;
                });
    }

    public CompletableFuture<Void> leaveGroup(int groupId) {
        return chatService
                .leaveGroupChatAsync(groupId)
                .thenRun(() -> Platform.runLater(() -> {
                    // Remove the group from the chat list
                    allChats.removeIf(
                            chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                                    chat.getId() == groupId);

                    // Clear selection if this was the selected chat
                    if (selectedChat != null && selectedChat.getId() == groupId) {
                        selectedChat = null;
                        currentMessages.clear();
                        showChatOptions.set(false);
                    }
                }));
    }

    public CompletableFuture<java.util.List<ChatMemberDTO>> getGroupMembers(
            int groupId) {
        return chatService.getGroupChatMembersAsync(groupId);
    }

    public CompletableFuture<Void> promoteToAdmin(
            String username,
            int groupId) {
        return chatService.promoteToAdminAsync(username, groupId);
    }

    public CompletableFuture<Void> demoteFromAdmin(
            String username,
            int groupId) {
        return chatService.demoteFromAdminAsync(username, groupId);
    }

    public void searchForUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }

        searchForUsersAsync(searchTerm.trim())
                .thenAccept(users -> Platform.runLater(() -> {
                    searchedUsers.clear();
                    searchedUsers.addAll(users);
                    showUserSearchResults.set(!users.isEmpty());
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.warning(
                                "User search failed: " + throwable.getMessage());
                        showUserSearchResults.set(false);
                    });
                    return null;
                });
    }

    public void startDirectChatWithUser(UserDTO user) {
        if (user == null)
            return;

        String currentUsername = authService.getCurrentUser().getUsername();
        boolean existingChatFound = allChats
                .stream()
                .anyMatch(
                        chat -> chat.getType() == ChatItemData.ChatType.DIRECT &&
                                chat.getName().equals(user.getUsername()));

        if (existingChatFound) {
            ChatItemData existingChat = allChats
                    .stream()
                    .filter(
                            chat -> chat.getType() == ChatItemData.ChatType.DIRECT &&
                                    chat.getName().equals(user.getUsername()))
                    .findFirst()
                    .orElse(null);

            if (existingChat != null) {
                selectChat(existingChat);
                clearSearch();
                return;
            }
        }

        chatService
                .createDirectChatAsync(user.getUsername())
                .thenAccept(directChat -> Platform.runLater(() -> {
                    ChatItemData newChatItem = createChatItemFromDirectChat(
                            directChat);
                    allChats.add(0, newChatItem);
                    selectChat(newChatItem);
                    clearSearch();
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setErrorMessage(
                            "Failed to create chat with " +
                                    user.getUsername() +
                                    ": " +
                                    throwable.getMessage()));
                    return null;
                });
    }

    public void selectChat(ChatItemData chatItem) {
        this.selectedChat = chatItem;
        loadMessagesForChat(chatItem);

        if (chatItem.getType() == ChatItemData.ChatType.GROUP) {
            setupGroupChatOptions(chatItem.getId());
        } else {
            showChatOptions.set(false);
            currentUserRole.set("");
        }
    }

    private void setupGroupChatOptions(int groupId) {
        getGroupMembers(groupId)
                .thenAccept(members -> Platform.runLater(() -> {
                    String currentUsername = authService
                            .getCurrentUser()
                            .getUsername();
                    Optional<ChatMemberDTO> currentMember = members
                            .stream()
                            .filter(member -> member.getUsername().equals(currentUsername))
                            .findFirst();

                    if (currentMember.isPresent()) {
                        currentUserRole.set(
                                currentMember.get().getRole().name());
                        showChatOptions.set(true);
                    } else {
                        showChatOptions.set(false);
                        currentUserRole.set("");
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.warning(
                                "Failed to load group members: " +
                                        throwable.getMessage());
                        showChatOptions.set(false);
                    });
                    return null;
                });
    }

    public boolean canManageGroup() {
        String role = currentUserRole.get();
        return role != null && (role.equals("CREATOR") || role.equals("ADMIN"));
    }

    public boolean canLeaveGroup() {
        String role = currentUserRole.get();
        return role != null && !role.equals("CREATOR");
    }

    public boolean canDeleteGroup() {
        String role = currentUserRole.get();
        return role != null && role.equals("CREATOR");
    }

    private void handleUserJoinedGroup(UserJoinedGroupEvent event) {
        String currentUsername = authService.getCurrentUser().getUsername();

        logger.info("UserJoinedGroupEvent received: roomId=" + event.getRoomId() +
                ", user=" + event.getUser().getUsername() +
                ", currentUser=" + currentUsername +
                ", invitedBy=" + event.getInvitedBy());

        if (event.getUser().getUsername().equals(currentUsername)) {
            Platform.runLater(() -> {
                // check if the group is already in the list
                // prevent duplications
                boolean groupExists = allChats.stream()
                        .anyMatch(chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                                chat.getId() == event.getRoomId());

                if (!groupExists) {
                    chatService.getMyGroupChatsAsync()
                            .thenAccept(groupChats -> Platform.runLater(() -> {
                                ChatRoomDTO joinedGroup = groupChats.stream()
                                        .filter(group -> group.getId() == event.getRoomId())
                                        .findFirst()
                                        .orElse(null);

                                if (joinedGroup != null) {
                                    ChatItemData newChatItem = createChatItemFromGroupChat(joinedGroup);
                                    allChats.add(0, newChatItem); // Add to top of list

                                    if (event.getInvitedBy() != null && !event.getInvitedBy().equals("system")) {
                                        setErrorMessage("You were added to the group by " + event.getInvitedBy());
                                    }
                                }
                            }))
                            .exceptionally(throwable -> {
                                Platform.runLater(() -> {
                                    logger.warning(
                                            "Failed to load group details after joining: " + throwable.getMessage());
                                    loadChats();
                                });
                                return null;
                            });
                }
            });
        } else {
            logger.info("Other user " + event.getUser().getUsername() + " joined group " + event.getRoomId());

            Platform.runLater(() -> {
                boolean currentUserInGroup = allChats.stream()
                        .anyMatch(chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                                chat.getId() == event.getRoomId());

                if (currentUserInGroup) {
                    ChatItemData groupChat = allChats.stream()
                            .filter(chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                                    chat.getId() == event.getRoomId())
                            .findFirst()
                            .orElse(null);

                    if (groupChat != null) {
                        if (event.getInvitedBy() != null && event.getInvitedBy().equals(currentUsername)) {
                            setErrorMessage("Successfully added " + event.getUser().getUsername() + " to \""
                                    + groupChat.getName() + "\"");
                        } else {
                            setErrorMessage(event.getUser().getUsername() + " joined \"" + groupChat.getName() + "\"");
                        }
                    }
                }
            });
        }
    }

    private void handleUserLeftGroup(UserLeftGroupEvent event) {
        String currentUsername = authService.getCurrentUser().getUsername();

        logger.info("UserLeftGroupEvent received: roomId=" + event.getRoomId() +
                ", user=" + event.getUser().getUsername() +
                ", currentUser=" + currentUsername +
                ", wasRemoved=" + event.wasRemoved() +
                ", removedBy=" + event.getRemovedBy());

        if (event.getUser().getUsername().equals(currentUsername)) {
            Platform.runLater(() -> {
                long groupCountBefore = allChats.stream()
                        .filter(chat -> chat.getType() == ChatItemData.ChatType.GROUP)
                        .count();

                logger.info("Groups in allChats before removal: " + groupCountBefore);

                boolean removed = allChats.removeIf(chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                        chat.getId() == event.getRoomId());

                logger.info("Group removal successful: " + removed);

                long groupCountAfter = allChats.stream()
                        .filter(chat -> chat.getType() == ChatItemData.ChatType.GROUP)
                        .count();

                logger.info("Groups in allChats after removal: " + groupCountAfter);

                if (selectedChat != null &&
                        selectedChat.getType() == ChatItemData.ChatType.GROUP &&
                        selectedChat.getId() == event.getRoomId()) {

                    selectedChat = null;
                    currentMessages.clear();
                    showChatOptions.set(false);
                    currentUserRole.set("");
                }

                if (event.wasRemoved()) {
                    setErrorMessage("You were removed from the group by " + event.getRemovedBy());
                } else {
                    setErrorMessage("You left the group");
                }
            });
        } else {
            logger.info("Other user left group: " + event.getUser().getUsername());
        }
    }

    public void sendMessage() {
        String content = messageInput.get();
        if (content == null || content.trim().isEmpty() || selectedChat == null) {
            return;
        }

        content = content.trim();
        CompletableFuture<Void> sendFuture;

        if (selectedChat.getType() == ChatItemData.ChatType.DIRECT) {
            sendFuture = chatService.sendDirectMessageAsync(
                    selectedChat.getId(),
                    content);
        } else {
            sendFuture = chatService.sendGroupMessageAsync(
                    selectedChat.getId(),
                    content);
        }

        sendFuture
                .thenRun(() -> Platform.runLater(() -> {
                    messageInput.set("");
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setErrorMessage(
                            "Failed to send message: " + throwable.getMessage()));
                    return null;
                });
    }

    public void setFilter(ChatFilter filter) {
        logger.info("Setting filter from " + this.currentFilter + " to " + filter);
        this.currentFilter = filter;

        updateFilterPredicate();

        logger.info("Filter updated, filteredChats size: " + filteredChats.size());
    }

    public void clearSearch() {
        searchText.set("");
        showUserSearchResults.set(false);
        searchedUsers.clear();
    }

    private void loadMessagesForChat(ChatItemData chatItem) {
        currentMessages.clear();

        CompletableFuture<java.util.List<MessageDTO>> messagesFuture;

        if (chatItem.getType() == ChatItemData.ChatType.DIRECT) {
            messagesFuture = chatService.getDirectChatMessagesAsync(
                    chatItem.getId(),
                    50);
        } else {
            messagesFuture = chatService.getGroupChatMessagesAsync(
                    chatItem.getId(),
                    50);
        }

        messagesFuture
                .thenAccept(messages -> Platform.runLater(() -> currentMessages.addAll(messages)))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setErrorMessage(
                            "Failed to load messages: " + throwable.getMessage()));
                    return null;
                });
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        MessageDTO message = event.getMessage();

        Platform.runLater(() -> {
            if (selectedChat != null) {
                boolean messageForCurrentChat = false;

                if (selectedChat.getType() == ChatItemData.ChatType.DIRECT) {
                    messageForCurrentChat = message.isDirectMessage() &&
                            Math.abs(message.getRoomId()) == selectedChat.getId();
                } else {
                    messageForCurrentChat = message.isGroupMessage() &&
                            message.getRoomId() == selectedChat.getId();
                }

                if (messageForCurrentChat) {
                    currentMessages.add(message);
                }
            }

            updateChatPreview(message);
        });
    }

    private void handleMessageEdited(MessageEditedEvent event) {
        Platform.runLater(() -> {
            MessageDTO editedMessage = event.getMessage();

            if (isMessageForCurrentChat(editedMessage)) {
                updateMessageInList(editedMessage);
            }

            updateChatPreview(editedMessage);
        });
    }

    private void handleMessageDeleted(MessageDeletedEvent event) {
        Platform.runLater(() -> {
            if (isMessageForCurrentChat(event.getRoomId())) {
                removeMessageFromList(event.getMessageId());
            }
        });
    }

    public boolean isMessageForCurrentChat(MessageDTO message) {
        if (selectedChat == null) {
            return false;
        }

        if (selectedChat.getType() == ChatItemData.ChatType.DIRECT) {
            return (message.isDirectMessage() &&
                    Math.abs(message.getRoomId()) == selectedChat.getId());
        } else {
            return (message.isGroupMessage() &&
                    message.getRoomId() == selectedChat.getId());
        }
    }

    public boolean isMessageForCurrentChat(int roomId) {
        if (selectedChat == null) {
            return false;
        }

        if (selectedChat.getType() == ChatItemData.ChatType.DIRECT) {
            return roomId == -selectedChat.getId();
        } else {
            return roomId == selectedChat.getId();
        }
    }

    public void updateMessageInList(MessageDTO updatedMessage) {
        for (int i = 0; i < currentMessages.size(); i++) {
            MessageDTO msg = currentMessages.get(i);
            if (msg.getId() == updatedMessage.getId()) {
                currentMessages.set(i, updatedMessage);
                break;
            }
        }
    }

    public void removeMessageFromList(int messageId) {
        currentMessages.removeIf(msg -> msg.getId() == messageId);
    }

    private void updateChatPreview(MessageDTO message) {
        ChatItemData chatToUpdate = null;

        if (message.isDirectMessage()) {
            int directChatId = Math.abs(message.getRoomId());
            chatToUpdate = allChats
                    .stream()
                    .filter(
                            chat -> chat.getType() == ChatItemData.ChatType.DIRECT &&
                                    chat.getId() == directChatId)
                    .findFirst()
                    .orElse(null);
        } else {
            chatToUpdate = allChats
                    .stream()
                    .filter(
                            chat -> chat.getType() == ChatItemData.ChatType.GROUP &&
                                    chat.getId() == message.getRoomId())
                    .findFirst()
                    .orElse(null);
        }

        if (chatToUpdate != null) {
            allChats.remove(chatToUpdate);

            ChatItemData updatedChat = new ChatItemData(
                    chatToUpdate.getId(),
                    chatToUpdate.getName(),
                    message.getSenderUsername() + ": " + message.getContent(),
                    chatToUpdate.getAvatarText(),
                    formatTime(message.getTimestamp()),
                    chatToUpdate.getType(),
                    chatToUpdate.isOnline(),
                    chatToUpdate.getUnreadCount() +
                            (selectedChat != chatToUpdate ? 1 : 0));

            allChats.add(0, updatedChat);

            if (selectedChat == chatToUpdate) {
                selectedChat = updatedChat;
            }
        }
    }

    public void editMessage(int messageId, String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            setErrorMessage("Message content cannot be empty");
            return;
        }

        chatService
                .editMessageAsync(messageId, newContent.trim())
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setErrorMessage(
                                "Failed to edit message: " +
                                        extractErrorMessage(throwable));
                    });
                    return null;
                });
    }

    public void deleteMessage(int messageId) {
        chatService
                .deleteMessageAsync(messageId)
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setErrorMessage(
                                "Failed to delete message: " +
                                        extractErrorMessage(throwable));
                    });
                    return null;
                });
    }

    private ChatItemData createChatItemFromDirectChat(
            DirectChatDTO directChat) {
        String currentUsername = authService.getCurrentUser().getUsername();
        String otherUser = directChat.getOtherUser(currentUsername);

        return new ChatItemData(
                directChat.getId(),
                otherUser,
                "Click to start chatting...",
                getInitials(otherUser, ""),
                formatTime(directChat.getLastMessageTimestamp()),
                ChatItemData.ChatType.DIRECT,
                false, // TODO: Implement online status
                0 // TODO: Implement unread count
        );
    }

    private ChatItemData createChatItemFromGroupChat(ChatRoomDTO groupChat) {
        return new ChatItemData(
                groupChat.getId(),
                groupChat.getName(),
                groupChat.getDescription() != null
                        ? groupChat.getDescription()
                        : "Group chat",
                getInitials(groupChat.getName(), ""),
                formatTime(groupChat.getCreatedTimestamp()),
                ChatItemData.ChatType.GROUP,
                false,
                0 // TODO: Implement unread count
        );
    }

    private CompletableFuture<java.util.List<UserDTO>> searchForUsersAsync(
            String searchTerm) {
        return chatService
                .searchUsersAsync(searchTerm)
                .exceptionally(throwable -> {
                    logger.warning("User search failed: " + throwable.getMessage());
                    return java.util.List.of();
                });
    }

    private String getInitials(String firstName, String lastName) {
        StringBuilder initials = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }
        return initials.toString().toUpperCase();
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0)
            return "";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "Just now";
        } else if (diff < 3600000) {
            return (diff / 60000) + "m";
        } else if (diff < 86400000) {
            return (diff / 3600000) + "h";
        } else {
            return (diff / 86400000) + "d";
        }
    }

    private String extractErrorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return throwable.getMessage() != null
                ? throwable.getMessage()
                : "An unexpected error occurred";
    }

    private void setErrorMessage(String message) {
        errorMessage.set(message);
        logger.warning(message);
    }

    public void cleanup() {
        connectionManager
                .getEventBus()
                .unsubscribe(MessageReceivedEvent.class, messageReceivedListener);
        connectionManager.getEventBus().unsubscribe(MessageEditedEvent.class, messageEditedListener);
        connectionManager.getEventBus().unsubscribe(MessageDeletedEvent.class, messageDeletedListener);

        connectionManager.getEventBus().unsubscribe(UserLeftGroupEvent.class, userLeftGroupListener);
        connectionManager.getEventBus().unsubscribe(UserJoinedGroupEvent.class, userJoinedGroupListener);
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public StringProperty messageInputProperty() {
        return messageInput;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public BooleanProperty isLoadingProperty() {
        return isLoading;
    }

    public BooleanProperty showUserSearchResultsProperty() {
        return showUserSearchResults;
    }

    public BooleanProperty showChatOptionsProperty() {
        return showChatOptions;
    }

    public StringProperty currentUserRoleProperty() {
        return currentUserRole;
    }

    public ObservableList<ChatItemData> getFilteredChats() {
        return filteredChats;
    }

    public ObservableList<UserDTO> getSearchedUsers() {
        return searchedUsers;
    }

    public ObservableList<MessageDTO> getCurrentMessages() {
        return currentMessages;
    }

    public ChatItemData getSelectedChat() {
        return selectedChat;
    }

    public ChatFilter getCurrentFilter() {
        return currentFilter;
    }

    public UserDTO getCurrentUser() {
        return authService.getCurrentUser();
    }
}
