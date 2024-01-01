/*
 * Copyright 2024 Thoughtworks, Inc.
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
Util = function() {
  return {
    idToSelector: function(theId) {
      return '#' + theId.replace(/([:.])/g,'\\$1');
    },

    spinny: function(elementId) {
      if (_.isEmpty(elementId)) return;

      const element = $(Util.idToSelector(elementId));
      element.html('&nbsp;');
      element.addClass('spinny');
    },

    unspinny: function(elementId) {
      if (_.isEmpty(elementId)) return;

      const element = $(Util.idToSelector(elementId));
      element.removeClass('spinny');
    },

    ajaxUpdate: function(url, idForSpinner) {
      $("#message_pane").html('');
      AjaxRefreshers.disableAjax();
      Util.spinny(idForSpinner);
      $.ajax({
        url: url,
        type: 'post',
        dataType: 'json',
        headers: {
          'X-GoCD-Confirm': true,
          'Accept': 'application/vnd.go.cd+json'
        },
        complete: function() {
          Util.unspinny(idForSpinner);
          AjaxRefreshers.enableAjax();
        },
        error: function(xhr) {
          if (xhr.status === 401) {
            window.location = window.location.protocol + '//' + window.location.host + '/go/auth/login';
          }
          $("#message_pane").html(`<p class="error">${xhr.responseText}</p>`);
        }
      });
    }
  };
}();
