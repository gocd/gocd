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
;(function ($, c) {
  "use strict";

  function ConsoleLogObserver(url, transformer, options) {
    var me   = this;

    var startLineNumber = 0;
    var inFlight = false;
    var finished = false;

    function acquireLock() {
      if (inFlight) return false;
      if (finished) return false;

      return inFlight = true;
    }

    function clearLock() {
      inFlight = false;
    }

    function consumeBuildLog(jobResultJson) {
      // Might be a bug with the "beforeSend" AJAX option in this version of jQuery.
      // When acquireLock() returns false, $.ajax() returns false, and thus fails
      // to attach the done() and always() callbacks.
      //
      // Thus, just return early if acquireLock() returns false. this might be fixed
      // by upgrading jQuery.
      if (!acquireLock()) return;

      $.ajax({
        url:  url,
        type: "GET",
        dataType: "text",
        data: {"startLineNumber": startLineNumber}
      }).done(function processLogOutput(data, status, xhr) {
        var lineSet, slice, nextLine = JSON.parse(xhr.getResponseHeader("X-JSON") || "[]")[0];

        if (nextLine !== startLineNumber) {
          lineSet = data.match(/^.*([\n\r]+|$)/gm);

          if ("" === lineSet[lineSet.length - 1]) lineSet.pop(); // regex generally leaves a terminal blank line for each set

          while (lineSet.length) {
            slice = lineSet.splice(0, 1000);
            transformer.transform(slice);
          }
          startLineNumber = nextLine;
        }

        finished = jobResultJson[0].building_info.is_completed.toLowerCase() === "true";
        if (options && "function" === typeof options.onUpdate) {
          transformer.invoke(options.onUpdate);
        }

        if (finished && options && "function" === typeof options.onComplete) {
          transformer.invoke(options.onComplete);
        }
      }).fail($.noop).always(clearLock);
    }

    me.notify = consumeBuildLog;
  }

  // export
  window.ConsoleLogObserver = ConsoleLogObserver;
})(jQuery, crel);
