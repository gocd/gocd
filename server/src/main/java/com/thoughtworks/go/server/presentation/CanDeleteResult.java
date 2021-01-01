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
package com.thoughtworks.go.server.presentation;

/**
 * @understands being a view model of whether a pipeline can be deleted or not
 */
public class CanDeleteResult {
    private final boolean canDelete;
    private final String message;

    public CanDeleteResult(boolean canDelete, String message) {
        this.canDelete = canDelete;
        this.message = message;
    }

    public boolean canDelete() {
        return canDelete;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CanDeleteResult that = (CanDeleteResult) o;

        if (canDelete != that.canDelete) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (canDelete ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CanDeleteResult");
        sb.append("{canDelete=").append(canDelete);
        sb.append(", message=").append(message);
        sb.append('}');
        return sb.toString();
    }
}
