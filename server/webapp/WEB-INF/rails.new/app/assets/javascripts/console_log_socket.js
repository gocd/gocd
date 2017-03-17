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

  function startTailingConsole(observer) {
    var startLine = 0, socket, ansi = ansi_up.ansi_to_html_obj();

    var details = $(".job_details_content"), contentArea = $(".buildoutput_pre");

    if (!details.length) return;

    function endpointUrl(startLine) {
      var l = document.location;
      var protocol = l.protocol.replace("http", "ws"), host = l.host, path = [
        "client-websocket",
        details.data("pipeline"),
        details.data("pipeline-label"),
        details.data("stage"),
        details.data("stage-counter"),
        details.data("build")
      ].join("/");

      return protocol + "//" + host + context_path(path) + "?startLine=" + startLine;
    }

    function init() {
      socket = new WebSocket(endpointUrl(startLine));

      socket.addEventListener("message", renderLines);
      socket.addEventListener("error", maybeResume);
      socket.addEventListener("close", maybeResume);
    }

    function maybeResume(e) {
      if ((e instanceof CloseEvent || e.type === "close") && e.wasClean) {
        startLine = 0;
        return;
      } else {
        // assume connection closed abnormally - e.g. network disconnect
        socket.close();
      }
      setTimeout(500, init);
    }

    function renderLines(e) {
      var build_output = e.data, lines, slices = [];

      function dequeueConsoleSlice() {
        contentArea.append(slices.shift().join("\n") + "\n"); // ensure terminal newline
        if (observer.enableTailing) observer.scrollToBottom();
      }

      if (build_output) {
        lines = ansi.ansi_to_html(ansi.escape_for_html(build_output), {use_classes: true}).split(/\r?\n/);

        while (lines.length > 0) {
          slices.push(lines.splice(0, 250));

          window.requestAnimationFrame(dequeueConsoleSlice);
        }

        startLine += lines.length;
      }
    }

    init();
  }

  window.startTailingConsole = startTailingConsole;
})(jQuery);
