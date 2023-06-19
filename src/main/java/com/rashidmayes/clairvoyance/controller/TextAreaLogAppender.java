package com.rashidmayes.clairvoyance.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import javafx.scene.control.TextArea;

import java.util.Objects;

public class TextAreaLogAppender extends AppenderBase<ILoggingEvent> {

    private final TextArea textArea;

    TextAreaLogAppender(TextArea textArea) {
        Objects.requireNonNull(textArea);
        this.textArea = textArea;
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (loggingEvent.getLevel().isGreaterOrEqual(Level.INFO) && containsConsoleMarker(loggingEvent)) {
            var text = "$> " + loggingEvent.getFormattedMessage() + "\n";
            textArea.appendText(text);
        }
    }

    private boolean containsConsoleMarker(ILoggingEvent loggingEvent) {
        return loggingEvent.getMarkerList()
                .stream()
                .anyMatch(marker -> ClairvoyanceLogger.IN_APP_CONSOLE.getName().equals(marker.getName()));
    }

}
