package org.javacord.core.entity.message;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.internal.MessageBuilderDelegate;
import org.javacord.api.entity.message.mention.AllowedMentions;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.core.DiscordApiImpl;
import org.javacord.core.entity.message.embed.EmbedBuilderDelegateImpl;
import org.javacord.core.entity.message.mention.AllowedMentionsImpl;
import org.javacord.core.entity.user.Member;
import org.javacord.core.util.FileContainer;
import org.javacord.core.util.logging.LoggerUtil;
import org.javacord.core.util.rest.RestEndpoint;
import org.javacord.core.util.rest.RestMethod;
import org.javacord.core.util.rest.RestRequest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The implementation of {@link MessageBuilderDelegate}.
 */
public class MessageBuilderDelegateImpl implements MessageBuilderDelegate {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(MessageBuilderDelegateImpl.class);

    /**
     * The receiver of the message.
     */
    private Messageable messageable = null;

    /**
     * The string builder used to create the message.
     */
    private final StringBuilder strBuilder = new StringBuilder();

    /**
     * The list of embed's of the message.
     */
    protected List<EmbedBuilder> embeds = new ArrayList<>();

    /**
     * If the message should be text to speech or not.
     */
    private boolean tts = false;

    /**
     * The nonce of the message.
     */
    private String nonce = null;

    /**
     * A list with all attachments which should be added to the message.
     */
    private final List<FileContainer> attachments = new ArrayList<>();

    /**
     * The MentionsBuilder used to control mention behavior.
     */
    private AllowedMentions allowedMentions = null;

    @Override
    public void appendCode(String language, String code) {
        strBuilder
                .append("\n")
                .append(MessageDecoration.CODE_LONG.getPrefix())
                .append(language)
                .append("\n")
                .append(code)
                .append(MessageDecoration.CODE_LONG.getSuffix());
    }

    @Override
    public void append(String message, MessageDecoration... decorations) {
        for (MessageDecoration decoration : decorations) {
            strBuilder.append(decoration.getPrefix());
        }
        strBuilder.append(message);
        for (int i = decorations.length - 1; i >= 0; i--) {
            strBuilder.append(decorations[i].getSuffix());
        }
    }

    @Override
    public void append(Mentionable entity) {
        strBuilder.append(entity.getMentionTag());
    }

    @Override
    public void append(Object object) {
        strBuilder.append(object);
    }

    @Override
    public void appendNewLine() {
        strBuilder.append("\n");
    }

    @Override
    public void setContent(String content) {
        strBuilder.setLength(0);
        strBuilder.append(content);
    }

    @Override
    public void addEmbed(EmbedBuilder embed) {
        if (embed != null) {
            embeds.add(embed);
        }
    }

    @Override
    public void removeAllEmbeds() {
        embeds.clear();
    }

    @Override
    public void setTts(boolean tts) {
        this.tts = tts;
    }

    @Override
    public void addFile(BufferedImage image, String fileName) {
        addAttachment(image, fileName);
    }

    @Override
    public void addFile(File file) {
        addAttachment(file);
    }

    @Override
    public void addFile(Icon icon) {
        addAttachment(icon);
    }

    @Override
    public void addFile(URL url) {
        addAttachment(url);
    }

    @Override
    public void addFile(byte[] bytes, String fileName) {
        addAttachment(bytes, fileName);
    }

    @Override
    public void addFile(InputStream stream, String fileName) {
        addAttachment(stream, fileName);
    }

    @Override
    public void addFileAsSpoiler(File file) {
        addAttachmentAsSpoiler(file);
    }

    @Override
    public void addFileAsSpoiler(Icon icon) {
        addAttachmentAsSpoiler(icon);
    }

    @Override
    public void addFileAsSpoiler(URL url) {
        addAttachmentAsSpoiler(url);
    }

    @Override
    public void addAttachment(BufferedImage image, String fileName) {
        if (image == null || fileName == null) {
            throw new IllegalArgumentException("image and fileName cannot be null!");
        }
        attachments.add(new FileContainer(image, fileName));
    }

    @Override
    public void addAttachment(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null!");
        }
        attachments.add(new FileContainer(file));
    }

    @Override
    public void addAttachment(Icon icon) {
        if (icon == null) {
            throw new IllegalArgumentException("icon cannot be null!");
        }
        attachments.add(new FileContainer(icon));
    }

    @Override
    public void addAttachment(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null!");
        }
        attachments.add(new FileContainer(url));
    }

    @Override
    public void addAttachment(byte[] bytes, String fileName) {
        if (bytes == null || fileName == null) {
            throw new IllegalArgumentException("bytes and fileName cannot be null!");
        }
        attachments.add(new FileContainer(bytes, fileName));
    }

    @Override
    public void addAttachment(InputStream stream, String fileName) {
        if (stream == null || fileName == null) {
            throw new IllegalArgumentException("stream and fileName cannot be null!");
        }
        attachments.add(new FileContainer(stream, fileName));
    }

    @Override
    public void addAttachmentAsSpoiler(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null!");
        }
        attachments.add(new FileContainer(file, true));
    }

    @Override
    public void addAttachmentAsSpoiler(Icon icon) {
        if (icon == null) {
            throw new IllegalArgumentException("icon cannot be null!");
        }
        attachments.add(new FileContainer(icon, true));
    }

    @Override
    public void addAttachmentAsSpoiler(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null!");
        }
        attachments.add(new FileContainer(url, true));
    }

    @Override
    public void setAllowedMentions(AllowedMentions allowedMentions) {
        if (allowedMentions == null) {
            throw new IllegalArgumentException("mention cannot be null!");
        }
        this.allowedMentions = allowedMentions;
    }

    @Override
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @Override
    public StringBuilder getStringBuilder() {
        return strBuilder;
    }

    @Override
    public CompletableFuture<Message> send(User user) {
        return send((Messageable) user);
    }

    @Override
    public CompletableFuture<Message> send(Messageable messageable) {
        if (messageable == null) {
            throw new IllegalStateException("Cannot send message without knowing the receiver");
        }
        if (messageable instanceof TextChannel) {
            return send((TextChannel) messageable);
        } else if (messageable instanceof User) {
            return ((User) messageable).openPrivateChannel().thenCompose(this::send);
        } else if (messageable instanceof Member) {
            return send(((Member) messageable).getUser());
        } else if (messageable instanceof IncomingWebhook) {
            return send((IncomingWebhook) messageable);
        }
        throw new IllegalStateException("Messageable of unknown type");
    }

    @Override
    public CompletableFuture<Message> send(TextChannel channel) {
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("content", toString() == null ? "" : toString())
                .put("tts", tts);
        body.putArray("mentions");


        if (allowedMentions != null) {
            ((AllowedMentionsImpl) allowedMentions).toJsonNode(body.putObject("allowed_mentions"));
        }
        if (embeds.size() > 0) {
            // As only messages sent by webhooks can contain more than one embed, it is enough to add the first.
            ((EmbedBuilderDelegateImpl) embeds.get(0).getDelegate()).toJsonNode(body.putObject("embed"));
        }
        if (nonce != null) {
            body.put("nonce", nonce);
        }

        RestRequest<Message> request = new RestRequest<Message>(channel.getApi(), RestMethod.POST, RestEndpoint.MESSAGE)
                .setUrlParameters(channel.getIdAsString());
        if (!attachments.isEmpty() || (embeds.size() > 0 && embeds.get(0).requiresAttachments())) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            // We access files etc. so this should be async
            channel.getApi().getThreadPool().getExecutorService().submit(() -> {
                try {
                    List<FileContainer> tempAttachments = new ArrayList<>(attachments);
                    // Add the attachments required for the embed
                    if (embeds.size() > 0) {
                        tempAttachments.addAll(
                                ((EmbedBuilderDelegateImpl) embeds.get(0).getDelegate()).getRequiredAttachments());
                    }

                    addMultipartBodyToRequest(request, body, tempAttachments, channel.getApi());

                    request.execute(result -> ((DiscordApiImpl) channel.getApi())
                            .getOrCreateMessage(channel, result.getJsonBody()))
                            .whenComplete((message, throwable) -> {
                                if (throwable != null) {
                                    future.completeExceptionally(throwable);
                                } else {
                                    future.complete(message);
                                }
                            });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        } else {
            request.setBody(body);
            return request.execute(result -> ((DiscordApiImpl) channel.getApi())
                    .getOrCreateMessage(channel, result.getJsonBody()));
        }
    }

    @Override
    public CompletableFuture<Message> send(IncomingWebhook webhook) throws IllegalStateException {
        return send(webhook, null, null, true);
    }

    protected CompletableFuture<Message> send(IncomingWebhook webhook, String displayName, URL avatarUrl, boolean wait)
            throws IllegalStateException {

        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("tts", this.tts);

        if (allowedMentions != null) {
            ((AllowedMentionsImpl) allowedMentions).toJsonNode(body.putObject("allowed_mentions"));
        }

        if (displayName != null) {
            body.put("username", displayName);
        }

        if (avatarUrl != null) {
            body.put("avatar_url", avatarUrl.toExternalForm());
        }

        if (embeds.size() != 0) {
            ArrayNode embedsNode = JsonNodeFactory.instance.objectNode().arrayNode();
            for (int i = 0; i < embeds.size() && i < 10; i++) {
                embedsNode.add(((EmbedBuilderDelegateImpl) embeds.get(i).getDelegate()).toJsonNode());
            }
            body.set("embeds", embedsNode);
        }

        if (strBuilder.length() != 0) {
            body.put("content", strBuilder.toString());
        }

        RestRequest<Message> request =
                new RestRequest<Message>(webhook.getApi(), RestMethod.POST, RestEndpoint.WEBHOOK_SEND)
                        .addQueryParameter("wait", Boolean.toString(wait))
                        .setUrlParameters(webhook.getIdAsString(), webhook.getToken());
        CompletableFuture<Message> future = new CompletableFuture<>();
        if (!attachments.isEmpty() || (embeds.size() > 0 && embeds.get(0).requiresAttachments())) {
            // We access files etc. so this should be async
            webhook.getApi().getThreadPool().getExecutorService().submit(() -> {
                try {
                    List<FileContainer> tempAttachments = new ArrayList<>(attachments);
                    // Add the attachments required for the embeds
                    for (int i = 0; i < embeds.size() && i < 10; i++) {
                        tempAttachments.addAll(
                                ((EmbedBuilderDelegateImpl) embeds.get(i).getDelegate()).getRequiredAttachments());
                    }

                    addMultipartBodyToRequest(request, body, tempAttachments, webhook.getApi());

                    executeWebhookRest(request, webhook, wait, future);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } else {
            request.setBody(body);
            executeWebhookRest(request, webhook, wait, future);
        }
        return future;
    }

    /**
     * Method which creates and adds a MultipartBody to a RestRequest.
     *
     * @param request The RestRequest to add the MultipartBody to
     * @param body The body to use as base for the MultipartBody
     * @param attachments The List of FileContainers to add as attachments
     * @param api the api instance needed to add the attachments
     */
    private void addMultipartBodyToRequest(RestRequest<Message> request, ObjectNode body,
                                           List<FileContainer> attachments, DiscordApi api) {
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", body.toString());

        Collections.reverse(attachments);
        for (int i = 0; i < attachments.size(); i++) {
            byte[] bytes = attachments.get(i).asByteArray(api).join();

            String mediaType = URLConnection
                    .guessContentTypeFromName(attachments.get(i).getFileTypeOrName());
            if (mediaType == null) {
                mediaType = "application/octet-stream";
            }
            multipartBodyBuilder.addFormDataPart("file" + i, attachments.get(i).getFileTypeOrName(),
                    RequestBody.create(MediaType.parse(mediaType), bytes));
        }

        request.setMultipartBody(multipartBodyBuilder.build());
    }

    /**
     * Method which executes the webhook rest request.
     * It gets the message from the channel and completes the future.
     *
     * @param request The rest request to execute
     * @param webhook The webhook to send the message to
     * @param wait If the wait param has been set
     * @param future The future to complete
     */
    private static void executeWebhookRest(RestRequest<Message> request, IncomingWebhook webhook, boolean wait,
                                           CompletableFuture<Message> future) {
        request.execute(result -> {
            TextChannel channel = webhook.getChannel().orElseThrow(() ->
                            new IllegalStateException("Cannot return a message when the channel isn't cached!")
                    );
            if (wait) {
                return ((DiscordApiImpl) webhook.getApi()).getOrCreateMessage(channel, result.getJsonBody());
            } else {
                return channel.getMessagesAsStream()
                        .filter(message -> message.getAuthor().isWebhook()
                                && message.getAuthor().getId() == webhook.getId())
                        .findFirst()
                        .orElseThrow(AssertionError::new);
            }
        }).whenComplete((message, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(message);
            }
        });
    }

    @Override
    public String toString() {
        return strBuilder.toString();
    }

}
