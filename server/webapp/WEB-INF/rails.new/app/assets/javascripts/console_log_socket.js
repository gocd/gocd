/*
 * Copyright 2017 ThoughtWorks, Inc.
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

(function($) {
  "use strict";

  function buildWebSocketUrl() {
    var details = $(".job_details_content");
    var l = document.location;
    var protocol = l.protocol.replace("http", "ws"), host = l.host, path = [
      "client-websocket",
      details.data("pipeline"),
      details.data("pipeline-label"),
      details.data("stage"),
      details.data("stage-counter"),
      details.data("build")
    ].join("/");

    return protocol + "//" + host + context_path(path);
  }

  function displayOutput(e) {
    var build_output = e.data;
    if (build_output) {
      var lines = build_output.match(/^.*([\n\r]+|$)/gm);
      while (lines.length) {
        var slice        = lines.splice(0, 1000);
        var htmlContents = ansi_up.ansi_to_html_obj().ansi_to_html(slice.join("").escapeHTML(), {use_classes: true});
        jQuery('.buildoutput_pre').append(htmlContents);
        if (observer.enableTailing) {
          observer.scrollToBottom();
        }
      }
    }
  }

  $(function() {
    var logWebsocket = new WebSocket(buildWebSocketUrl());
    logWebsocket.addEventListener('message', displayOutput);
  });
})(jQuery);
