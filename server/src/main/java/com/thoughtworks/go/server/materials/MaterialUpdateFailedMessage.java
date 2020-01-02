/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.Material;

/**
 * @understands when a material update failed
 */
public class MaterialUpdateFailedMessage extends MaterialUpdateCompletedMessage {
    private final String reason;

    public MaterialUpdateFailedMessage(Material material, long trackingId, Exception e) {
        super(material, trackingId);
        this.reason = computeReason(e);
    }

    public String getReason() {
        return reason;
    }

    private String computeReason(Exception e) {
        String message = message(e);
        if (e.getCause() != null) {
            String causeMessage = message(e.getCause());
            if (!causeMessage.equals(message)) {
                return String.format("%s. Cause: %s", message, causeMessage);
            }
        }
        return message;
    }

    private String message(final Throwable e) {
        return e.getMessage() == null ? "" : e.getMessage();
    }
}
