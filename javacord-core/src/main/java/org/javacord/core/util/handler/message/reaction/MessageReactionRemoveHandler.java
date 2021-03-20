package org.javacord.core.util.handler.message.reaction;

import com.fasterxml.jackson.databind.JsonNode;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.core.entity.emoji.UnicodeEmojiImpl;
import org.javacord.core.entity.message.MessageImpl;
import org.javacord.core.event.message.reaction.ReactionRemoveEventImpl;
import org.javacord.core.util.event.DispatchQueueSelector;
import org.javacord.core.util.gateway.PacketHandler;

import java.util.Optional;

/**
 * Handles the message reaction remove packet.
 */
public class MessageReactionRemoveHandler extends PacketHandler {

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public MessageReactionRemoveHandler(DiscordApi api) {
        super(api, true, "MESSAGE_REACTION_REMOVE");
    }

    @Override
    public void handle(JsonNode packet) {
        long messageId = packet.get("message_id").asLong();
        long userId = packet.get("user_id").asLong();
        long channelId = packet.get("channel_id").asLong();

        Optional<Message> message = api.getCachedMessageById(messageId);
        Optional<TextChannel> channel = api.getTextChannelById(channelId);

        Emoji emoji;
        JsonNode emojiJson = packet.get("emoji");
        if (!emojiJson.has("id") || emojiJson.get("id").isNull()) {
            emoji = UnicodeEmojiImpl.fromString(emojiJson.get("name").asText());
        } else {
            emoji = api.getKnownCustomEmojiOrCreateCustomEmoji(emojiJson);
        }

        message.ifPresent(msg -> ((MessageImpl) msg).removeReaction(emoji, userId == api.getYourself().getId()));

        ReactionRemoveEvent event = new ReactionRemoveEventImpl(api, messageId, channelId, emoji, userId);

        Optional<Server> optionalServer = channel.flatMap(TextChannel::asServerChannel).map(ServerChannel::getServer);
        api.getEventDispatcher().dispatchReactionRemoveEvent(
                optionalServer.map(DispatchQueueSelector.class::cast).orElse(api),
                messageId,
                optionalServer.orElse(null),
                channel.orElse(null),
                userId,
                event);
    }

}
