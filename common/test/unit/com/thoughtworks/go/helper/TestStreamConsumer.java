package com.thoughtworks.go.helper;

import com.thoughtworks.go.util.command.StreamConsumer;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TestStreamConsumer implements StreamConsumer {
    private ConcurrentLinkedDeque<String> lines = new ConcurrentLinkedDeque<>();

    public void consumeLine(String line) {
        lines.add(line);
    }

    public String output() {
        return StringUtils.join(lines, "\n");
    }

    @Override
    public String toString() {
        return output();
    }

    public List<String> asList() {
        return new ArrayList<>(lines);
    }


    public String lastLine() {
        return lines.getLast();
    }

    public String firstLine() {
        return lines.getFirst();
    }

    public int lineCount() {
        return lines.size();
    }

    public void waitForContain(final String content, Timeout timeout) throws InterruptedException {
        Assertions.waitUntil(timeout, new Assertions.Predicate() {
            @Override
            public boolean call() throws Exception {
                return output().contains(content);
            }
        }, 250);
    }

    public void clear() {
        lines.clear();
    }

}
