/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";

export const Capabilities = function (data) {
  this.canSearch           = Stream(data.canSearch);
  this.supportedAuthType   = Stream(data.supportedAuthType);
  this.canAuthorize        = Stream(data.canAuthorize);
};

Capabilities.fromJSON = (data = {}) => new Capabilities({
  canSearch:           data.can_search,
  supportedAuthType:   data.supported_auth_type,
  canAuthorize:        data.can_authorize,
});

