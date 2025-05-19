/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.chat.message_container;

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.desktop.main.content.chat.message_container.components.ChatMentionPopupMenu;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.stream.Collectors;

import static javafx.scene.input.KeyEvent.KEY_PRESSED;

@Slf4j
public class ChatMessageContainerView extends bisq.desktop.common.view.View<VBox, ChatMessageContainerModel, ChatMessageContainerController> {
    private final static double CHAT_BOX_MAX_WIDTH = 1200;
    private final static double CAT_HASH_IMAGE_SIZE = 34;
    public final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

    private final BisqTextArea inputField = new BisqTextArea();
    private final EventHandler<KeyEvent> keyPressedHandler = this::processKeyPressed;
    private final Button sendButton = new Button();
    private final Pane messagesListView;
    private final VBox emptyMessageList;
    private final BisqTooltip myProfileNickNameTooltip = new BisqTooltip();
    private ImageView myProfileCatHashImageView;
    private ChatMentionPopupMenu userMentionPopup;
    private Pane userProfileSelectionRoot;
    private Subscription focusInputTextFieldPin, caretPositionPin, myUserProfilePin;

    public ChatMessageContainerView(ChatMessageContainerModel model,
                                    ChatMessageContainerController controller,
                                    Pane messagesListView,
                                    Pane quotedMessageBlock,
                                    UserProfileSelection userProfileSelection) {
        super(new VBox(), model, controller);

        this.messagesListView = messagesListView;
        VBox.setVgrow(messagesListView, Priority.ALWAYS);

        emptyMessageList = ChatUtil.createEmptyChatPlaceholder(
                new Label(Res.get("chat.private.messagebox.noChats.placeholder.title")),
                new Label(Res.get("chat.private.messagebox.noChats.placeholder.description")));

        VBox bottomBarContainer = createAndGetBottomBar(userProfileSelection);

        quotedMessageBlock.setMaxWidth(CHAT_BOX_MAX_WIDTH);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(messagesListView, emptyMessageList, quotedMessageBlock, bottomBarContainer);
    }

    @Override
    protected void onViewAttached() {
        userProfileSelectionRoot.visibleProperty().bind(model.getShouldShowUserProfileSelection());
        userProfileSelectionRoot.managedProperty().bind(model.getShouldShowUserProfileSelection());
        myProfileCatHashImageView.visibleProperty().bind(model.getShouldShowUserProfile());
        myProfileCatHashImageView.managedProperty().bind(model.getShouldShowUserProfile());

        inputField.textProperty().bindBidirectional(model.getTextInput());

        caretPositionPin = EasyBind.subscribe(model.getCaretPosition(), position -> {
            if (position != null) {
                inputField.positionCaret(position.intValue());
            }
        });

        myUserProfilePin = EasyBind.subscribe(model.getMyUserProfile(), userProfile -> {
            if (userProfile != null) {
                myProfileCatHashImageView.setImage(CatHash.getImage(userProfile, CAT_HASH_IMAGE_SIZE));
                myProfileCatHashImageView.setOnMouseClicked(e -> controller.onOpenProfileCard(userProfile));
                myProfileNickNameTooltip.setText(userProfile.getNickName());
            }
        });

        inputField.addEventFilter(KEY_PRESSED, keyPressedHandler);

        sendButton.setOnAction(event -> {
            controller.onSendMessage(inputField.getText().trim());
            inputField.clear();
        });

        userMentionPopup.getObservableList().setAll(model.getMentionableUsers().stream()
                .map(ChatMentionPopupMenu.ListItem::new)
                .collect(Collectors.toList()));

        createChatDialogEnabledSubscription();

        focusInputTextFieldPin = EasyBind.subscribe(model.getFocusInputTextField(), focusInputTextField -> {
            if (focusInputTextField != null && focusInputTextField) {
                inputField.requestFocus();
            }
        });
        userMentionPopup.init();

        UIThread.runOnNextRenderFrame(inputField::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        userProfileSelectionRoot.visibleProperty().unbind();
        userProfileSelectionRoot.managedProperty().unbind();
        myProfileCatHashImageView.visibleProperty().unbind();
        myProfileCatHashImageView.managedProperty().unbind();
        inputField.textProperty().unbindBidirectional(model.getTextInput());
        focusInputTextFieldPin.unsubscribe();
        caretPositionPin.unsubscribe();
        myUserProfilePin.unsubscribe();
        removeChatDialogEnabledSubscription();

        Tooltip.uninstall(myProfileCatHashImageView, myProfileNickNameTooltip);

        myProfileCatHashImageView.setOnMouseClicked(null);
        inputField.setOnKeyPressed(null);
        inputField.removeEventFilter(KEY_PRESSED, keyPressedHandler);
        sendButton.setOnAction(null);
        userMentionPopup.cleanup();
    }

    private VBox createAndGetBottomBar(UserProfileSelection userProfileSelection) {
        setUpUserProfileSelection(userProfileSelection);

        myProfileCatHashImageView = new ImageView();
        myProfileCatHashImageView.setFitWidth(CAT_HASH_IMAGE_SIZE);
        myProfileCatHashImageView.setFitHeight(CAT_HASH_IMAGE_SIZE);
        myProfileCatHashImageView.getStyleClass().add("hand-cursor");
        HBox.setMargin(myProfileCatHashImageView, new Insets(-4, 3, 4, 0));
        Tooltip.install(myProfileCatHashImageView, myProfileNickNameTooltip);

        HBox sendMessageBox = createAndGetSendMessageBox();

        HBox bottomBar = new HBox(4);
        bottomBar.getChildren().addAll(userProfileSelectionRoot, myProfileCatHashImageView, sendMessageBox);
        bottomBar.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        bottomBar.setPadding(new Insets(14, 20, 14, 20));
        bottomBar.setAlignment(Pos.BOTTOM_CENTER);

        VBox bottomBarContainer = new VBox(bottomBar);
        bottomBarContainer.setAlignment(Pos.CENTER);
        return bottomBarContainer;
    }

    private HBox createAndGetSendMessageBox() {
        inputField.setPromptText(Res.get("chat.message.input.prompt"));
        inputField.getStyleClass().addAll("chat-input-field", "medium-text");
        inputField.setPadding(new Insets(5, 0, 5, 5));
        HBox.setMargin(inputField, new Insets(0, 0, 1.5, 0));
        HBox.setHgrow(inputField, Priority.ALWAYS);
        setUpInputFieldAtMentions();

        sendButton.setGraphic(ImageUtil.getImageViewById("chat-send"));
        sendButton.setId("chat-messages-send-button");
        HBox.setMargin(sendButton, new Insets(0, 0, 5, 0));
        sendButton.setMinWidth(30);
        sendButton.setMaxWidth(30);
        sendButton.setTooltip(new BisqTooltip(Res.get("chat.message.input.send"), BisqTooltip.Style.DARK));

        HBox sendMessageBox = new HBox(inputField, sendButton);
        sendMessageBox.getStyleClass().add("chat-send-message-box");
        sendMessageBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(sendMessageBox, Priority.ALWAYS);
        return sendMessageBox;
    }

    private void setUpInputFieldAtMentions() {
        userMentionPopup = new ChatMentionPopupMenu(inputField, controller::onUserProfileSelected);
    }

    private void setUpUserProfileSelection(UserProfileSelection userProfileSelection) {
        userProfileSelection.openMenuUpwards();
        userProfileSelection.openMenuToTheRight();
        userProfileSelectionRoot = userProfileSelection.getRoot();
        userProfileSelectionRoot.setMaxHeight(45);
        userProfileSelectionRoot.setMinWidth(55);
    }

    private void createChatDialogEnabledSubscription() {
        inputField.disableProperty().bind(model.getChatDialogEnabled().not());
        sendButton.disableProperty().bind(model.getChatDialogEnabled().not());
        emptyMessageList.visibleProperty().bind(model.getChatDialogEnabled().not());
        emptyMessageList.managedProperty().bind(model.getChatDialogEnabled().not());
        messagesListView.visibleProperty().bind(model.getChatDialogEnabled());
        messagesListView.managedProperty().bind(model.getChatDialogEnabled());
        userProfileSelectionRoot.disableProperty().bind(model.getChatDialogEnabled().not());
    }

    private void removeChatDialogEnabledSubscription() {
        inputField.disableProperty().unbind();
        sendButton.disableProperty().unbind();
        emptyMessageList.visibleProperty().unbind();
        emptyMessageList.managedProperty().unbind();
        messagesListView.visibleProperty().unbind();
        messagesListView.managedProperty().unbind();
        userProfileSelectionRoot.disableProperty().unbind();
    }

    private void processKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            if (keyEvent.isShiftDown()) {
                int caretPosition = inputField.getCaretPosition();
                inputField.insertText(caretPosition, System.lineSeparator());
                inputField.positionCaret(caretPosition + System.lineSeparator().length());
            } else if (!inputField.getText().isEmpty()) {
                controller.onSendMessage(inputField.getText().trim());
                inputField.clear();
            }
        } else if (keyEvent.getCode() == KeyCode.UP) {
            if (inputField.getText().isEmpty() || inputField.getCaretPosition() == 0) {
                // Only consume event in this case, otherwise allow falling back to default behavior
                keyEvent.consume();
                controller.onArrowUpKeyPressed();
            } else {
                String normalizedText = StringUtils.normalizeLineBreaks(inputField.getText());
                // If no line break is found from the start to the caret position, it means we are in the first line, so we should move to the start
                if (normalizedText.indexOf(System.lineSeparator(), 0, inputField.getCaretPosition()) == -1) {
                    // Only consume event in this case, otherwise allow falling back to default behavior
                    keyEvent.consume();
                    inputField.positionCaret(0);
                }
            }
        }
    }
}
