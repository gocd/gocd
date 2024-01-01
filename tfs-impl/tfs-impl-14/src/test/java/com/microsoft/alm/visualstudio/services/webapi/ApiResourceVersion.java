/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.microsoft.alm.visualstudio.services.webapi;

/**
 * Dummy class to allow mocking of TFSTeamProjectCollection. This does not seem to actually be required at runtime and
 * the class is not included with the Core SDK, however Mockito will see it on the TFSConnection, so this allows the
 * mocks to at least work.
 *
 * https://github.com/microsoft/team-explorer-everywhere/issues/268
 */
@SuppressWarnings("unused") //
public class ApiResourceVersion {
    public static class Version {}
}
