package com.fullonibus.twitchirc.parser;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.model.EmoteOccurrence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrcMessageParserTest {

    @Test
    void parse_normalPrivmsgWithTags() {
        String raw = "@badge-info=;badges=broadcaster/1;color=;display-name=testuser;emotes=25:0-4;id=abc;login=testuser;mod=0;msg-id=abc;room-id=123;subscriber=1;tmi-sent-ts=1615000000000;user-id=456;user-type= :testuser!testuser@testuser.tmi.twitch.tv PRIVMSG #channel :Kappa Hello";
        ChatMessage msg = IrcMessageParser.parse(raw);

        assertNotNull(msg);
        assertEquals("testuser", msg.getUsername());
        assertEquals("#channel", msg.getChannel());
        assertEquals("Kappa Hello", msg.getText());
        assertTrue(msg.isSubscriber());
        assertEquals("testuser", msg.getDisplayName());
        assertFalse(msg.getEmotes().isEmpty());
    }

    @Test
    void parse_privmsgWithoutTags() {
        String raw = ":testuser!testuser@testuser.tmi.twitch.tv PRIVMSG #channel :hello world";
        ChatMessage msg = IrcMessageParser.parse(raw);

        assertNotNull(msg);
        assertEquals("testuser", msg.getUsername());
        assertEquals("#channel", msg.getChannel());
        assertEquals("hello world", msg.getText());
        assertFalse(msg.isSubscriber());
    }

    @Test
    void parse_pingMessage() {
        assertTrue(IrcMessageParser.isPing("PING :tmi.twitch.tv"));
        assertTrue(IrcMessageParser.isPing("  PING  "));
        assertFalse(IrcMessageParser.isPing("PRIVMSG #test :hello"));
        assertEquals("PONG :tmi.twitch.tv", IrcMessageParser.pongResponse());
    }

    @Test
    void parse_joinReturnsNull() {
        String raw = ":testuser!testuser@testuser.tmi.twitch.tv JOIN #channel";
        assertNull(IrcMessageParser.parse(raw));
    }

    @Test
    void parse_partReturnsNull() {
        String raw = ":testuser!testuser@testuser.tmi.twitch.tv PART #channel";
        assertNull(IrcMessageParser.parse(raw));
    }

    @Test
    void parse_messageWithEmotesTag() {
        String raw = "@emotes=25:0-4,6-10/1902:12-16;display-name=user;login=user;subscriber=0;tmi-sent-ts=1615000000000 :user!user@user.tmi.twitch.tv PRIVMSG #channel :Kappa Kappa PogChamp";
        ChatMessage msg = IrcMessageParser.parse(raw);

        assertNotNull(msg);
        assertEquals(3, msg.getEmotes().size());
        assertEquals("25", msg.getEmotes().get(0).getEmoteId());
        assertEquals(0, msg.getEmotes().get(0).getStartIndex());
        assertEquals(4, msg.getEmotes().get(0).getEndIndex());
        assertEquals("1902", msg.getEmotes().get(2).getEmoteId());
    }

    @Test
    void parse_messageWithSubscriberTag() {
        String raw = "@display-name=subuser;login=subuser;subscriber=1;tmi-sent-ts=1615000000000 :subuser!subuser@subuser.tmi.twitch.tv PRIVMSG #channel :sub message";
        ChatMessage msg = IrcMessageParser.parse(raw);

        assertNotNull(msg);
        assertTrue(msg.isSubscriber());
    }

    @Test
    void parse_nonSubscriberMessage() {
        String raw = "@display-name=reguser;login=reguser;subscriber=0;tmi-sent-ts=1615000000000 :reguser!reguser@reguser.tmi.twitch.tv PRIVMSG #channel :regular message";
        ChatMessage msg = IrcMessageParser.parse(raw);

        assertNotNull(msg);
        assertFalse(msg.isSubscriber());
    }
}
