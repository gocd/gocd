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

(function ($) {
  "use strict";

  function BuildSummarySocket(fallbackObserver, transformer, consoleLogSocket) {
    var socket;
    var isConsoleLogStreamingStarted = false;

    var details              = $(".job_details_content");
    var fallingBackToPolling = false;

    if (!details.length) return;

    function genricEndpointUrl() {
      var l = document.location;
      var protocol = l.protocol.replace("http", "ws"), host = l.host, path = "browser-websocket";

      return protocol + "//" + host + context_path(path);
    }

    socket = new WebSocketWrapper({
      url:                          genricEndpointUrl(),
      indefiniteRetry:              true,
      failIfInitialConnectionFails: true
    });

    socket.on("open", function () {
      var msg = {
        action: "subscribe",
        events: [
          {
            'type': 'JobStatusChange',
            'job_identifier': {
              'pipeline_name': details.data('pipeline'),
              'pipeline_label': details.data('pipeline-label'),
              'stage_name': details.data('stage'),
              'stage_counter': details.data('stage-counter'),
              'build_name': details.data('build')
            }
          },
          {
            'type': 'ServerHealthMessageCount'
          }
        ]
      };

      // we call `toJSON` offered by prototype.js.
      // Calling `JSON.stringify()` fails because of monkey patched `toJSON` on all JS prototypes applied by prototype.js.
      socket.send(Object.toJSON(msg));
    });
    socket.on("message", renderJobStatus);

    socket.on("initialConnectFailed", retryConnectionOrFallbackToPollingOnError);
    socket.on("close", maybeResumeOnClose);

    function retryConnectionOrFallbackToPollingOnError(e) {
      fallingBackToPolling = true; // prevent close handler from trying to reconnect
      fallbackObserver.enable();
      fallbackObserver.notify();
    }

    function maybeResumeOnClose(e) {
      console.log("Closing socket!!");
      if (fallingBackToPolling) {
        return;
      }
    }


    function maybeGunzip(gzippedBuf) {
      var inflator = new pako.Inflate({to: 'string'});
      inflator.push(gzippedBuf, true);

      if (inflator.err) {
        return String.fromCharCode.apply(null, gzippedBuf);
      } else {
        return inflator.result;
      }
    }

    function startConsoleLog(jobStatus) {
      if (jobStatus[0].building_info.current_status == 'preparing' && !isConsoleLogStreamingStarted) {
        consoleLogSocket.start();
        isConsoleLogStreamingStarted = true;
      }

      if (jobStatus[0].building_info.is_completed == 'true' && !isConsoleLogStreamingStarted) {
        consoleLogSocket.start();
        isConsoleLogStreamingStarted = true;
      }
    }

    function renderJobStatus(e) {
      var jobStatusJSON = e.data, lines, slice = [];

      if (!jobStatusJSON || !(jobStatusJSON instanceof Blob)) {
        return;
      }

      var reader = new FileReader();

      reader.addEventListener("loadend", function () {
        var arrayBuffer   = reader.result;
        var gzippedBuf    = new Uint8Array(arrayBuffer);
        var websocketMessage = JSON.parse(maybeGunzip(gzippedBuf));
        if(websocketMessage.type === 'JobStatusChange') {
          var jobStatus = websocketMessage.response;
          startConsoleLog(jobStatus);
          fallbackObserver.notify(jobStatus);
        } else {
          console.log(websocketMessage);
        }
      });

      reader.readAsArrayBuffer(jobStatusJSON);
    }
  }

  window.BuildSummarySocket = BuildSummarySocket;
})(jQuery);
