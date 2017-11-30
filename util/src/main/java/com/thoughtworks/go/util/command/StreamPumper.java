/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemTimeClock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;


public class StreamPumper implements Runnable {

    private BufferedReader in;

    private boolean completed;
    private final StreamConsumer streamConsumer;
    private final String prefix;
    private long lastHeard;
    private final Clock clock;

    private StreamPumper(InputStream in, StreamConsumer streamConsumer, String prefix, String encoding) {
        this(in, streamConsumer, prefix, encoding, new SystemTimeClock());
    }

    StreamPumper(InputStream in, StreamConsumer streamConsumer, String prefix, String encoding, Clock clock) {
        this.streamConsumer = streamConsumer;
        this.prefix = prefix;
        this.clock = clock;
        this.lastHeard = System.currentTimeMillis();
        try {
            this.in = new LineNumberReader(new InputStreamReader(in, encoding));
        } catch (UnsupportedEncodingException e) {
            bomb(format("Unable to use [%s] to decode stream.", encoding));
        }
    }

    public void run() {
        try {
            String s = in.readLine();
            while (s != null) {
                consumeLine(s);
                s = in.readLine();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            // do nothing
        } finally {
            IOUtils.closeQuietly(in);
            completed = true;
        }
    }

    private void consumeLine(String line) {
        lastHeard = System.currentTimeMillis();
        if (streamConsumer != null) {
            if (StringUtils.isBlank(prefix)) {
                streamConsumer.consumeLine(line);
            } else {
                streamConsumer.consumeLine(prefix + line);
            }
        }
    }


    public void readToEnd() {
        while (!completed) {
            try {
                clock.sleepForMillis(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static StreamPumper pump(InputStream stream, StreamConsumer streamConsumer, String prefix, String encoding) {
        StreamPumper pumper = new StreamPumper(stream, streamConsumer, prefix, encoding);
        new Thread(pumper).start();
        return pumper;
    }

    private Long timeSinceLastLine(TimeUnit unit) {
        long now = clock.currentTimeMillis();
        return unit.convert(now - lastHeard, TimeUnit.MILLISECONDS);
    }

    public boolean didTimeout(long duration, TimeUnit unit) {
        if (completed) { return false; }
        return timeSinceLastLine(unit) > duration;
    }

    public long getLastHeard() {
        return lastHeard;
    }
}
