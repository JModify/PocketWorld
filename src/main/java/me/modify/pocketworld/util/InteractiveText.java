package me.modify.pocketworld.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.w3c.dom.Text;

public class InteractiveText {

    private final String text;
    private final ChatColor chatColor;
    private final boolean bold;
    private final boolean italic;

    private final ClickEvent clickEvent;
    private final HoverEvent hoverEvent;

    public InteractiveText(Builder builder) {
        this.text = builder.text;
        this.chatColor = builder.chatColor;
        this.bold = builder.bold;
        this.italic = builder.italic;
        this.clickEvent = builder.clickEvent;
        this.hoverEvent = builder.hoverEvent;
    }

    public TextComponent getMessage() {
        TextComponent textComponent = new TextComponent(text);

        if (chatColor != null) textComponent.setColor(chatColor);
        if (bold) textComponent.setBold(true);
        if (italic) textComponent.setItalic(true);

        if (clickEvent != null) textComponent.setClickEvent(clickEvent);
        if (hoverEvent != null) textComponent.setHoverEvent(hoverEvent);

        return textComponent;
    }

    public TextComponent append(InteractiveText... interactiveText) {
        TextComponent newComponent = getMessage();
        for (InteractiveText extra : interactiveText) {
            HoverEvent emptyHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("").create());
            ClickEvent emptyClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "");
            TextComponent emptyText = new TextComponent(" ");
            emptyText.setHoverEvent(emptyHover);
            emptyText.setClickEvent(emptyClick);

            newComponent.addExtra(emptyText);
            newComponent.addExtra(extra.getMessage());
        }
        return newComponent;
    }

    public static class Builder {

        private final String text;

        private ChatColor chatColor;
        private boolean bold;
        private boolean italic;

        private ClickEvent clickEvent;
        private HoverEvent hoverEvent;

        public Builder(String text) {
            this.text = text;
        }

        public Builder color(ChatColor chatColor) {
            this.chatColor = chatColor;
            return this;
        }

        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder italic(boolean italic) {
            this.italic = italic;
            return this;
        }

        public Builder clickEvent(ClickEvent clickEvent) {
            this.clickEvent = clickEvent;
            return this;
        }

        @SuppressWarnings("deprecation")
        public Builder hoverText(String text, ChatColor color, boolean bold, boolean italic) {
            this.hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text)
                    .color(color).bold(bold).italic(italic).create());
            return this;
        }

        public InteractiveText build() {
            return new InteractiveText(this);
        }
    }
}
