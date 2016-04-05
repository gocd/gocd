package com.thoughtworks.go.helper;

import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.StreamConsumer;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class TestStreamConsumer implements StreamConsumer {
    private ConcurrentLinkedDeque<String> lines = new ConcurrentLinkedDeque<>();

    public void consumeLine(String line) {
        lines.add(line);
    }

    public String output() {
        return StringUtils.join(lines, "\n");
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

    public void waitForContain(String content, int timeoutInSeconds) throws InterruptedException {
        long start = System.nanoTime();
        while (true) {
            if (lines.contains(content)) {
                break;
            }
            if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(timeoutInSeconds)) {
                throw new RuntimeException("waiting timeout!");
            }
            Thread.sleep(10);
        }
    }

    public void clear() {
        lines.clear();
    }

}
