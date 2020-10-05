package org.javacord.core.entity.message;

import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.internal.WebhookMessageBuilderDelegate;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.core.util.logging.LoggerUtil;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * The implementation of {@link WebhookMessageBuilderDelegate}.
 */
public class WebhookMessageBuilderDelegateImpl
        extends MessageBuilderDelegateImpl implements WebhookMessageBuilderDelegate {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(WebhookMessageBuilderDelegateImpl.class);

    /**
     * The avatar the webhook should use.
     */
    private URL avatarUrl = null;

    /**
     * The display name the webhook should use.
     */
    private String displayName = null;


    @Override
    public CompletableFuture<Message> send(IncomingWebhook webhook) throws IllegalStateException {
        return send(webhook, displayName, avatarUrl);
    }

    @Override
    public void addEmbeds(EmbedBuilder... embeds) {
        this.embeds.addAll(Arrays.asList(embeds));
    }

    @Override
    public void removeEmbed(EmbedBuilder embed) {
        this.embeds.remove(embed);
    }

    @Override
    public void removeEmbeds(EmbedBuilder... embeds) {
        this.embeds.removeAll(Arrays.asList(embeds));
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void setDisplayAvatar(URL avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public void setDisplayAvatar(Icon avatar) {
        this.avatarUrl = avatar.getUrl();
    }
}
