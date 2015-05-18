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

    autoScrollButton.on('mouseenter', function () {
      $('.scroll-help-text').show()
    });

    autoScrollButton.on('mouseleave', function () {
      $('.scroll-help-text').hide();
    });

    var globalBackToTopButton      = $('#back_to_top'),
        topActionBar               = $('.console-action-bar'),
        bottomActionBar            = $('.console-footer-action-bar'),
        historySidebarContainer    = $('.sidebar_history'),
        historySidebarHandle       = $('.sidebar-handle'),
        historySidebarHandleHeight = $('.sidebar-handle').outerHeight(true),
        historySidebar             = $('#build_history_holder'),
        sidebarPin                 = $('.sidebar-pin'),
        sidebarTop                 = historySidebar.offset().top,
        sidebarBottom              = sidebarTop + historySidebar.get(0).getBoundingClientRect().height
      ;

    // pin the console action bar on top
    topActionBar.scrollToFixed({marginTop: 90, zIndex: 100});
    // and the other action bar to bottom
    bottomActionBar.scrollToFixed({bottom: 0, limit: bottomActionBar.offset().top, zIndex: 100});

    $(window).on('scroll resize', function (evt) {
      var delta                 = 5;
      var currentHandlePosition = historySidebarHandle.position().top;
      var shouldHideHandle      = currentHandlePosition + historySidebarHandleHeight + delta > sidebarBottom;
      historySidebarHandle.toggleClass('hide-handle', shouldHideHandle);
    });

    // hide the global "back to top" link, because the one on the console log goes well with the console log
    function hideGlobalBackToTopButton() {
      var consoleTab = $('#tab-content-of-console');
      globalBackToTopButton.toggleClass('back_to_top', !consoleTab.is(':visible'));
    }

    hideGlobalBackToTopButton();

    $('.sub_tabs_container a').on('click', function () {
      hideGlobalBackToTopButton();
    });

    var currentPinned = localStorage && localStorage.getItem('job-detail-sidebar-collapsed') === 'true';

    sidebarPin.on('click', function () {
      historySidebarContainer.toggleClass('pin-this');

      $(this).toggleClass('pinned');
      if (localStorage) {
        localStorage.setItem('job-detail-sidebar-collapsed', $('.sidebar_history').hasClass('pin-this'));
      }
    });

    sidebarPin.toggleClass('pinned', currentPinned);
    historySidebarContainer.toggleClass('pin-this', currentPinned);
  });
})(jQuery);
