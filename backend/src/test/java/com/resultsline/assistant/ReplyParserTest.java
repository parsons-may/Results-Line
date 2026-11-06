package com.resultsline.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReplyParserTest {

    @Test
    void parsesValidAnswer() {
        AssistantReply reply = ReplyParser.parse(
                "{\"type\":\"answer\",\"answer\":\"They found 2 polyps.\",\"fields_used\":[\"polypCount\"]}");
        assertThat(reply).isEqualTo(new AssistantReply.Answer("They found 2 polyps.", java.util.List.of("polypCount")));
    }

    @Test
    void parsesValidHandoff() {
        AssistantReply reply = ReplyParser.parse("{\"type\":\"handoff\",\"reason\":\"medical advice\"}");
        assertThat(reply).isEqualTo(new AssistantReply.Handoff("medical advice"));
    }

    @Test
    void rejectsEmptyAndNonJson() {
        assertThatThrownBy(() -> ReplyParser.parse("")).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse(null)).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse("You're fine!")).isInstanceOf(ReplyFormatException.class);
    }

    @Test
    void rejectsJsonInsideMarkdownFence() {
        assertThatThrownBy(() -> ReplyParser.parse(
                "```json\n{\"type\":\"handoff\",\"reason\":\"x\"}\n```")).isInstanceOf(ReplyFormatException.class);
    }

    @Test
    void rejectsNonObjectAndUnknownType() {
        assertThatThrownBy(() -> ReplyParser.parse("[1,2]")).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse("{\"type\":\"chat\",\"reason\":\"x\"}"))
                .isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse("{\"reason\":\"x\"}")).isInstanceOf(ReplyFormatException.class);
    }

    @Test
    void rejectsExtraOrMissingKeys() {
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"handoff\",\"reason\":\"x\",\"note\":\"extra\"}")).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"answer\",\"answer\":\"hi\"}")).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"answer\",\"reason\":\"x\",\"fields_used\":[]}")).isInstanceOf(ReplyFormatException.class);
    }

    @Test
    void rejectsWrongValueTypes() {
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"answer\",\"answer\":\"hi\",\"fields_used\":\"polypCount\"}"))
                .isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"answer\",\"answer\":\"hi\",\"fields_used\":[1]}")).isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse(
                "{\"type\":\"answer\",\"answer\":\"  \",\"fields_used\":[\"polypCount\"]}"))
                .isInstanceOf(ReplyFormatException.class);
        assertThatThrownBy(() -> ReplyParser.parse("{\"type\":\"handoff\",\"reason\":\"\"}"))
                .isInstanceOf(ReplyFormatException.class);
    }
}
