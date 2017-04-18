/*
 * Copyright 2015 ThoughtWorks, Inc.
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
;(function ($) {
  "use strict";

  function ConsoleScroller(consoleContainer, consoleTab, consoleScrollToggle) {
    var self                    = this;
    this.consoleContainer       = consoleContainer;
    this.previousScrollPosition = $(window).scrollTop();
    this.consoleTab             = consoleTab;
    this.consoleScrollToggle    = consoleScrollToggle;
    this.consoleScrollToggle.addClass('tailing');

    function toggleEventHandler() {
      self.toggleScrolling();
    }

    this.consoleContainer.on("consoleUpdated", function () {
      self.scrollToBottom();
    });

    this.consoleContainer.on("consoleCompleted, consoleInteraction", function () {
      self.stopScroll();
    });

    this.consoleScrollToggle.on('click', toggleEventHandler);
    this.consoleTab.on('click', toggleEventHandler);
  }

  $.extend(ConsoleScroller.prototype, {
    toggleScrolling: function toggleScrolling() {
      if (!this.tailingEnabled) {
        this.startScroll();
      } else {
        this.stopScroll();
      }
    },
    startScroll:     function startScroll() {
      this.tailingEnabled = true;
      var self            = this;
      this.consoleScrollToggle.addClass('tailing');
      this.scrollToBottom(0);
      $(window).on('scroll.autoScroll resize.autoScroll', $.throttle(200, function () {
        if (self.previousScrollPosition - $(window).scrollTop() > 5) {
          self.stopScroll();
        }
      }));
    },
    stopScroll:      function stopScroll() {
      this.tailingEnabled = false;
      $(window).off('scroll.autoScroll resize.autoScroll');
      this.consoleScrollToggle.removeClass('tailing');
    },
    scrollToBottom:  function scrollToBottom(delay) {
      var self = this;

      if (!self.tailingEnabled) return;

      function captureScrollPosition() {
        self.previousScrollPosition = $(window).scrollTop();
      }

      $('body,html').stop(true).animate(
        {
          scrollTop: this.consoleContainer.outerHeight()
        },
        {
          duration: delay || 100,
          start:    captureScrollPosition, // start is not support with the current version, but we'll be upgrading
          complete: captureScrollPosition,
          step:     captureScrollPosition
        }
      );
    }
  });

  // export
  window.ConsoleScroller = ConsoleScroller;
})(jQuery);
