/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.util.ServletHelper;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;

public class BackupFilterTest {

    private HttpServletResponse res;
    private BackupFilter backupFilter;
    private static final Optional<String> BACKUP_STARTED_AT = Optional.of("Some old date");
    private static final Optional<String> BACKUP_STARTED_BY = Optional.of("admin");
    private FilterChain chain;
    private BackupService backupService;
    private PrintWriter writer;
    private InputStream inputStream;

    @BeforeEach
    public void setUp() throws ServletException, IOException {
        ServletHelper.init();

        res = mock(HttpServletResponse.class);
        backupService = mock(BackupService.class);
        chain = mock(FilterChain.class);
        inputStream = BackupFilter.class.getClassLoader().getResourceAsStream("backup_in_progress.html");
        writer = mock(PrintWriter.class);
        when(res.getWriter()).thenReturn(writer);
        this.backupFilter = new BackupFilter(backupService);
    }

    @Test
    public void shouldPassRequestWhenBackupIsNotBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(false);
        Request request = request(HttpMethod.GET, "", "/go/agents");
        backupFilter.doFilter(request, res, chain);
        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldPassHealthCheckRequestWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        Request request = request(HttpMethod.GET, "", "/api/v1/health");
        backupFilter.doFilter(request, res, chain);
        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldPassBackupPageSPARequestWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        Request request = request(HttpMethod.GET, "", "/admin/backup");
        backupFilter.doFilter(request, res, chain);
        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldWriteToResponseWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);

        String content = IOUtils.toString(inputStream, UTF_8);
        content = backupFilter.replaceStringLiterals(content);
        Request request = request(HttpMethod.GET, "", "/go/agents");

        backupFilter.doFilter(request, res, chain);

        verify(res, times(1)).setContentType("text/html");
        verify(writer).print(content);
        verify(res).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(res).setDateHeader("Expires", 0);
        verify(res).setStatus(503);
    }

    @Test
    public void shouldGetServerBackupByIdWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        Request request = request(HttpMethod.GET, "", "/api/backups/12");
        backupFilter.doFilter(request, res, chain);

        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldGetRunningServerBackupWhenBackupIsBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        Request request = request(HttpMethod.GET, "", "/api/backups/running");
        backupFilter.doFilter(request, res, chain);

        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldGetStaticAssetsWhenBackupIsBeingTaken() throws IOException, ServletException {
        when(backupService.isBackingUp()).thenReturn(true);
        Request request = request(HttpMethod.GET, "", "/assets/foo.js");
        backupFilter.doFilter(request, res, chain);

        verify(res, times(0)).setContentType("text/html");
        verify(writer, times(0)).print("some test data for my input stream");
        verify(res, never()).setStatus(anyInt());
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
        verify(res, never()).setStatus(anyInt());
    }

    @Test
    public void shouldReturn503WhenJsonAPICallIsMadeAndBackupBeingTaken() throws Exception {
        when(backupService.isBackingUp()).thenReturn(true);
        when(backupService.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(backupService.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);
        MockHttpServletRequest request = HttpRequestBuilder.GET("/api/agents").withHeader("Accept", "application/json").build();

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
        MockHttpServletRequest request = HttpRequestBuilder.GET("/api/config.xml").withHeader("Accept", "application/json").build();

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
        verify(res).setStatus(503);
    }

    private Request request(HttpMethod method, String contentType, String uri) {
        Request request = new Request(mock(HttpChannel.class, RETURNS_DEEP_STUBS), mock(HttpInput.class));
        HttpURI httpURI = new HttpURI("http", "url", 8153, uri);
        MetaData.Request metadata = new MetaData.Request(method.asString(), httpURI, HttpVersion.HTTP_2, new HttpFields());
        request.setMetaData(metadata);
        request.setContentType(contentType);
        request.setHttpURI(httpURI);
        return request;
    }
}
