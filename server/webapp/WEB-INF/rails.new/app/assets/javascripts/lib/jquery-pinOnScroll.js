/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

// Author: Ketan Padegaonkar
(function ($) {
  var $window = $(window);

  function existingStyleAttrs(element) {
    return element.data('originalPos');
  }

  function createStyleAttrs(element, options) {
    return $.extend({}, {
      width:     element.width(),
      position:  'fixed',
      'z-index': 100
    }, options);
  }

  function applyStyle(element, styleAttrs) {
    if (_.isEqual(existingStyleAttrs(element), styleAttrs)) {
      return;
    }
    element.css(styleAttrs);
    element.data('originalPos', $.extend(existingStyleAttrs(element) || {}, styleAttrs));
  }

  function clearStyles(element) {
    element.css(_.mapValues(existingStyleAttrs(element), function () {
      return '';
    }));

    element.removeData('originalPos');
  }

  function fixElement(element, options) {
    var scroll     = $window.scrollTop();
    var styleAttrs = existingStyleAttrs(element) || createStyleAttrs(element, options);

    if (scroll >= options.requiredScroll && options.top) {
      applyStyle(element, styleAttrs);
    } else if (scroll >= options.requiredScroll && options.bottomLimit) {
      applyStyle(element, $.extend({}, styleAttrs, {
        top: options.bottomLimit()
      }));
    } else {
      clearStyles(element);
    }
  }

  function unFixElement(element, options) {
    clearStyles(element);
  }

  function maybeCallUsingRequestAnimationFrame(func, arguments) {
    if (window.requestAnimationFrame) {
      window.requestAnimationFrame(function () {
        func(arguments);
      });
    } else {
      func(arguments)
    }
  }

  $.fn.pinOnScroll = function (opts) {
    this.each(function () {
      var elem = $(this);

      var throttledFixElement   = _.throttle(fixElement, 100);
      var throttledUnFixElement = _.throttle(unFixElement, 100);

      window.setInterval(function () {
        maybeCallUsingRequestAnimationFrame(function () {
          throttledFixElement(elem, opts);
        });
      }, 100);

      $(window).on('scroll.pinOnScroll', function () {
        maybeCallUsingRequestAnimationFrame(function () {
          throttledFixElement(elem, opts);
        });
      });

      $(window).on('resize.pinOnScroll', function () {
        maybeCallUsingRequestAnimationFrame(function () {
          throttledUnFixElement(elem, opts);
          throttledFixElement(elem, opts);
        });
      })
    });

    return this;
  };
}(jQuery));
