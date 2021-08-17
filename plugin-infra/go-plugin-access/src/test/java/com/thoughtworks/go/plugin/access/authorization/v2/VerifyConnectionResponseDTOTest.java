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
package com.thoughtworks.go.plugin.access.authorization.v2;

import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class VerifyConnectionResponseDTOTest {

    @Test
    public void shouldDeserializeSuccessResponseFromJSON() throws Exception {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"message\": \"Connection check passed\"\n" +
                "}";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus(), is("success"));
        assertThat(response.getMessage(), is("Connection check passed"));
        assertNull(response.getValidationResult());
    }

    @Test
    public void shouldDeserializeFailureResponseFromJSON() throws Exception {
        String json = "{\n" +
                "  \"status\": \"failure\",\n" +
                "  \"message\": \"Connection check failed\"\n" +
                "}";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus(), is("failure"));
        assertThat(response.getMessage(), is("Connection check failed"));
        assertNull(response.getValidationResult());
    }

    @Test
    public void shouldDeserializeValidationFailedResponseFromJSON() throws Exception {
        String json = "{\n" +
                "  \"status\": \"validation-failed\",\n" +
                "  \"message\": \"Validation failed\",\n" +
                "  \"errors\": [\n" +
                "    {" +
                "      \"key\": \"url\",\n" +
                "      \"message\": \"URL cannot be blank\"\n" +
                "    },\n" +
                "    {" +
                "      \"key\": \"password\",\n" +
                "      \"message\": \"Password cannot be blank\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus(), is("validation-failed"));
        assertThat(response.getMessage(), is("Validation failed"));
        assertFalse(response.getValidationResult().isSuccessful());
        assertThat(response.getValidationResult().getErrors().get(0), is(new com.thoughtworks.go.plugin.domain.common.ValidationError("url", "URL cannot be blank")));
        assertThat(response.getValidationResult().getErrors().get(1), is(new com.thoughtworks.go.plugin.domain.common.ValidationError("password", "Password cannot be blank")));
    }
}
