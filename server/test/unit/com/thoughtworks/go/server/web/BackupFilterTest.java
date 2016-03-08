/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */


package com.thoughtworks.go.server.web;

import com.google.gson.JsonObject;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.util.FileUtil;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

public class BackupFilterTest {

    private HttpServletResponse res;
    private BackupFilter backupFilter;
    private static final String BACKUP_STARTED_AT = "Some old date";
    private static final String BACKUP_STARTED_BY = "admin";
    private FilterChain chain;
    private FilterConfig filterConfig;
    private ByteArrayOutputStream output;
    private BackupService backupService;
    private PrintWriter writer;
    private InputStream inputStream;
    private String content;

    @Before
    public void setUp() throws ServletException, IOException {
        ServletHelper.init();

        res = mock(HttpServletResponse.class);
        backupService = mock(BackupService.class);
        chain = mock(FilterChain.class);
        filterConfig = mock(FilterConfig.class);
        inputStream = BackupFilter.class.getClassLoader().getResourceAsStream("backup_in_progress.html");
        output = new ByteArrayOutputStream();
        writer = mock(PrintWriter.class);
        when(res.getWriter()).thenReturn(writer);
        this.backupFilter = new BackupFilter(backupService);
        backupFilter.init(filterConfig);
    }

    @Test
    public void shouldPassRequestWhenBackupIsNotBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(false);
        Request request = request(HttpMethod.GET, "", "/go/agents");
        backupFilter.doFilter(request, res, chain);
        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
    }

    @Test
    public void shouldWriteToResponseWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);

        String content = FileUtil.readToEnd(inputStream);
        content = backupFilter.replaceStringLiterals(content);
        Request request = request(HttpMethod.GET, "", "/go/agents");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("text/html");
        verify(writer).print(content);
        verify(res).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(res).setDateHeader("Expires", 0);
    }

    @Test
    public void shouldGenerateHTMLResponseWhenBackupIsBeingTakenAndMessageJsonIsCalled() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);

        String content = FileUtil.readToEnd(inputStream);
        content = backupFilter.replaceStringLiterals(content);
        Request request = request(HttpMethod.GET, "", "/go/server/messages.json");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("text/html");
        verify(writer).print(content);
        verify(res).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(res).setDateHeader("Expires", 0);
    }


    @Test
    public void shouldReturnJsonResponseWhenBackupIsFinishedJsonAPIIsBeingCalled() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);
        Request request = request(HttpMethod.GET, "application/json", "/go/is_backup_finished.json");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("application/json");
        JsonObject json = new JsonObject();
        json.addProperty("is_backing_up", true);
        verify(writer).print(json);
        verify(res).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(res).setDateHeader("Expires", 0);
    }

    @Test
    public void shouldReturn503WhenJsonAPICallIsMadeAndBackupBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);
        Request request = request(HttpMethod.GET, "application/json", "/go/api/agents");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("application/json");
        JsonObject json = new JsonObject();
        assertResponse(json);
    }

    @Test
    public void shouldReturn503WhenXMLAPICallIsMadeAndBackupBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);
        Request request = request(HttpMethod.GET, "application/json", "/go/api/config.xml");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("application/json");
        JsonObject json = new JsonObject();
        assertResponse(json);
    }

    private void assertResponse(JsonObject json) {
        json.addProperty("message", "Server is under maintenance mode, please try later.");
        verify(writer).print(json);
        verify(res).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(res).setDateHeader("Expires", 0);
    }

    private Request request(HttpMethod method, String contentType, String uri) {
        Request request = new Request(mock(HttpChannel.class), mock(HttpInput.class));
        request.setContentType(contentType);
        request.setMethod(method, method.asString());
        request.setUri(new HttpURI(uri));
        return request;
    }
}
