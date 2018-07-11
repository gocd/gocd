/**
 * Copyright 2016 aixigo AG
 * Released under the MIT license.
 * http://laxarjs.org/license
 */

/* eslint-disable no-var, prefer-arrow-callback, object-shorthand, prefer-template */
/* global document */

// Fixup stack traces, using this approach: https://gist.github.com/guncha/f45ceef6d483c384290a
(function () {

  var sourceMappedStackTrace = require('sourcemapped-stacktrace');

  jasmine.getEnv().addReporter({
    jasmineDone: function () {
      if (window.location.search.indexOf('unmangle=true') >= 0) {
        return;
      }
      var traces = document.querySelectorAll('.jasmine-stack-trace');
      for (var i = 0; i < traces.length; i++) {
        fixStackTrace(traces[i]);
      }
    }
  });


  function fixStackTrace(node) {
    sourceMappedStackTrace.mapStackTrace(node.textContent, function (stack) {
      var prevNode     = node.previousSibling;
      var prevNodeText = prevNode.getAttribute('title') || prevNode.textContent;
      node.textContent = prevNodeText + '\n' + stack.join('\n');
    });
  }

})();
