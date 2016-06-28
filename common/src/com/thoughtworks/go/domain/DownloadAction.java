/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.log4j.Logger;

public class DownloadAction {

    private final HttpService httpService;
    private final GoPublisher goPublisher;
    private final Clock clock;
    private static final int DOWNLOAD_SLEEP_MILLIS = 5000;
    private static final Logger LOG = Logger.getLogger(DownloadAction.class);


    public DownloadAction(HttpService httpService, GoPublisher goPublisher, Clock clock) {
        this.httpService = httpService;
        this.goPublisher = goPublisher;
        this.clock = clock;
    }

    public void perform(String url, FetchHandler handler) throws InterruptedException {
        int retryCount = 0;
        while (true) {
            retryCount++;
            String message = "";
            try {
                int rc = download(httpService, url, handler);
                if (handler.handleResult(rc, goPublisher)) {
                    return;
                }
                message = String.format("Unsuccessful response '%s' from the server", rc);
            } catch (Exception e) {
                message = String.format("Caught an exception '%s'", e.getMessage());
            }
            if (retryCount > 3) {
                message = String.format("Giving up fetching resource '%s'. Tried 4 times and failed.", url);
                LOG.error(message);
                throw new RuntimeException(message);
            }
            long backout = Math.round(backout(retryCount));
            publishDownloadError(url, message, backout);
            clock.sleepForSeconds(backout);
        }
    }

    private void publishDownloadError(String url, String cause, long backout) throws InterruptedException {
        String message = String.format("Could not fetch artifact %s. Pausing %s seconds to retry. Error was : %s", url, backout, cause);
        goPublisher.consumeLineWithPrefix(message);
        LOG.warn(message);
    }

    private int download(HttpService httpService, String url, FetchHandler handler) throws Exception {
        int returnCode = httpService.download(url, handler);
        while (returnCode == HttpServletResponse.SC_ACCEPTED) {
            clock.sleepForMillis(DOWNLOAD_SLEEP_MILLIS);
            returnCode = httpService.download(url, handler);
        }
        return returnCode;
    }

    private double backout(int retryCount) {
        return (retryCount * 10.0) + (10 * Math.random());
    }
}
