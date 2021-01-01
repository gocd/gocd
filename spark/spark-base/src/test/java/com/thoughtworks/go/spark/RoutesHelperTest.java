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

package com.thoughtworks.go.spark;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import spark.Request;
import spark.Response;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;

class RoutesHelperTest {
    private static final String JSON_MESSAGE = "{\"message\":\"Boom!!\"}";

    private static Stream<Data> data() {
        return Stream.of(
                new Data(TEXT_HTML_VALUE, HtmlErrorPage.errorPage(404, "Boom!!")),
                new Data(APPLICATION_XHTML_XML_VALUE, HtmlErrorPage.errorPage(404, "Boom!!")),
                new Data(APPLICATION_XML_VALUE, new RecordNotFoundException("Boom!!").asXML()),
                new Data(TEXT_XML_VALUE, new RecordNotFoundException("Boom!!").asXML()),
                new Data(APPLICATION_RSS_XML_VALUE, new RecordNotFoundException("Boom!!").asXML()),
                new Data(APPLICATION_ATOM_XML_VALUE, new RecordNotFoundException("Boom!!").asXML()),
                new Data(APPLICATION_JSON_VALUE, JSON_MESSAGE),
                new Data("BAR", JSON_MESSAGE)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldSendHttpExceptionResponseBasedOnAcceptHeader(Data data) {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.headers("Accept")).thenReturn(data.mimeType);

        new RoutesHelper(mock(SparkController.class)).httpException(new RecordNotFoundException("Boom!!"), request, response);

        verify(response).body(data.responseText);
    }

    static class Data {
        String mimeType;
        String responseText;

        Data(String mimeType, String responseText) {
            this.mimeType = mimeType;
            this.responseText = responseText;
        }
    }

    @Test
    void shouldAddDeprecationHeaders() {
        DoNothingApiV1 doNothingApiV1 = new DoNothingApiV1();

        RoutesHelper helper = new RoutesHelper(doNothingApiV1);

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.url()).thenReturn("http://test.host:80/go");

        helper.setDeprecationHeaders(request, response, doNothingApiV1.getClass().getAnnotation(DeprecatedAPI.class));

        verify(response).header("X-GoCD-API-Deprecated-In", "v20.2.0");
        verify(response).header("X-GoCD-API-Removal-In", "v20.5.0");
        verify(response).header("X-GoCD-API-Deprecation-Info", "https://api.gocd.org/20.2.0/#api-changelog");
        verify(response).header("Link", "<http://test.host:80/go>; Accept=\"application/vnd.go.cd.v2+json\"; rel=\"successor-version\"");
        verify(response).header("Warning", "299 GoCD/v20.2.0 \"The Do Nothing API version v1 has been deprecated in GoCD Release v20.2.0. This version will be removed in GoCD Release v20.5.0. Version v2 of the API is available, and users are encouraged to use it\"");
    }

    @DeprecatedAPI(deprecatedApiVersion = ApiVersion.v1, successorApiVersion = ApiVersion.v2, deprecatedIn = "20.2.0", removalIn = "20.5.0", entityName = "Do Nothing")
    private class DoNothingApiV1 implements SparkSpringController {
        @Override
        public void setupRoutes() {
        }
    }
}
