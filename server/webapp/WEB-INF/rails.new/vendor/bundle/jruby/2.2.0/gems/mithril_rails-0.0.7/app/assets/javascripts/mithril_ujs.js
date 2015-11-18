// Unobtrusive scripting adapter for React
(function(document, window, m) {
  var CLASS_NAME_ATTR = 'data-mithril-class';
  var PROPS_ATTR = 'data-mithril-props';

  // jQuery is optional. Use it to support legacy browsers.
  var $ = (typeof jQuery !== 'undefined') && jQuery;

  var findMithrilDOMNodes = function() {
    var SELECTOR = '[' + CLASS_NAME_ATTR + ']';
    if ($) {
      return $(SELECTOR);
    } else {
      return document.querySelectorAll(SELECTOR);
    }
  };

  var mountComponents = function() {
    var nodes = findMithrilDOMNodes();
    for (var i = 0; i < nodes.length; ++i) {
      var node = nodes[i];
      var className = node.getAttribute(CLASS_NAME_ATTR);
      // Assume className is simple and can be found at top-level (window).
      // Fallback to eval to handle cases
      var constructor = window[className] || eval.call(window, className);
      var propsJson = node.getAttribute(PROPS_ATTR);
      var props = propsJson && JSON.parse(propsJson);
      // insert props in to module
      constructor.properties = props;
      m.module(node, constructor);
    }
  };

  // Register page load & unload events
  if ($) {
    $(mountComponents);
  } else {
    document.addEventListener('DOMContentLoaded', mountComponents);
  }

  // Turbolinks specified events
  if (typeof Turbolinks !== 'undefined') {
    var handleEvent;
    if ($) {
      handleEvent = function(eventName, callback) {
        $(document).on(eventName, callback);
      }
    } else {
      handleEvent = function(eventName, callback) {
        document.addEventListener(eventName, callback);
      }
    }
    handleEvent('page:change', mountComponents);
  }
})(document, window, m);
