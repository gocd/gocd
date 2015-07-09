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

//the wrapper just exists so that $ maps to jquery, instead of prototype inside the function.
(function ($) {
  $(function () {
    var autoScrollButton = $('.auto-scroll');

    if (autoScrollButton.length === 0) {
      return;
    }

    var globalBackToTopButton = $('#back_to_top'),
        consoleTab            = $('#tab-content-of-console'),
        failuresTab           = $('#tab-content-of-failures'),
        $window               = $(window)
      ;


    // hide the global "back to top" link, because the one on the console log goes well with the console log
    function maybeHideGlobalBackToTopButton() {
      var hideGlobalBackToTopButton = false,
          activeTab;

      if (consoleTab.is(':visible')) {
        hideGlobalBackToTopButton = true;
        activeTab                 = consoleTab;
      }

      if (failuresTab.is(':visible')) {
        hideGlobalBackToTopButton = true;
        activeTab                 = failuresTab;
      }

      if (!activeTab) {
        return;
      }

      if (!activeTab.data('enable-scroll-to-fixed')) {
        activeTab.data('enable-scroll-to-fixed', true);
        var topActionBar    = activeTab.find('.console-action-bar'),
            bottomActionBar = activeTab.find('.console-footer-action-bar');

        topActionBar.pinOnScroll({
          'z-index':      100,
          top:            90,
          requiredScroll: 199
        });

        bottomActionBar.pinOnScroll({
          'z-index':      100,
          requiredScroll: 0,
          bottomLimit:    function () {
            return Math.min($window.height(), bottomActionBar.parent().get(0).getBoundingClientRect().bottom) - bottomActionBar.outerHeight(true);
          }
        });
      } else {
        return;
      }

      globalBackToTopButton.toggleClass('back_to_top', !hideGlobalBackToTopButton);
    }

    maybeHideGlobalBackToTopButton();

    $('.sub_tabs_container a').on('click', function () {
      window.setTimeout(maybeHideGlobalBackToTopButton, 50);
    });

  });
})(jQuery);
