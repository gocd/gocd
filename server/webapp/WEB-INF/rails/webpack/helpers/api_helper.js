/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const AjaxHelper = require("helpers/ajax_helper");
const parseError = require("helpers/mrequest").unwrapErrorExtractMessage;
const Dfr        = require("jquery").Deferred;

/** This helper parses data, etags, and errors for a convenient API */
function req(exec) {
  return Dfr(function ajax() {
    const success = (data, _s, xhr) => this.resolve(data, parseEtag(xhr), xhr.status);
    const failure = (xhr) => this.reject(parseError(xhr.responseJSON, xhr));

    exec().then(success, failure);
  }).promise();
}

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }

const ApiHelper = {};

for (const key in AjaxHelper) {
  ApiHelper[key] = (config) => req(() => AjaxHelper[key](config));
}

module.exports = ApiHelper;
