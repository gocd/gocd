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
package com.thoughtworks.go.domain.materials;

public enum ModifiedAction {
    added,
    modified,
    deleted,
    unknown;

    public static ModifiedAction parseSvnAction(char action) {
        switch (action) {
            case 'A':
                return added;
            case 'M':
                return modified;
            case 'D':
                return deleted;
            default:
                return unknown;
        }
    }

    public static ModifiedAction parseP4Action(String action) {
        if ("add".equals(action))    { return ModifiedAction.added; }
        if ("edit".equals(action))   { return ModifiedAction.modified; }
        if ("delete".equals(action)) { return ModifiedAction.deleted; }
        return ModifiedAction.unknown;
    }

    public static ModifiedAction parseGitAction(char action) {
        return parseSvnAction(action);
    }
}
