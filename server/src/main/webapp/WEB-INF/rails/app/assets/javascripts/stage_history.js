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
var StageHistory = function() {
  function _bindHistoryLink(id, url, page_num) {
    var elem = $(id).get(0);
    if (!elem) return;        
    var element = $(elem);
    element.unbind();
    element.click(function() {
      changePage(url, page_num);
    });
  }

  function changePage(url, pageNum) {
    AjaxRefreshers.disableAjax();
    $.ajax({
      url: url,
      method: "GET",
      dataType: "html",
      complete: function(response) {
        $('.stage_history').html(response);
        AjaxRefreshers.enableAjax();
      }
    });
    setCurrentPage(pageNum);
  }

  function setCurrentPage(pageNum) {
    $("#stage-history-page").val(pageNum);
  }

  function init() {
  }


  init.prototype._changePage = changePage;

  init.prototype.bindHistoryLink = function(id, url, page_num) {
    return new Promise((resolve) => $(function() {
      _bindHistoryLink(id, url, page_num);
      AjaxRefreshers.main().afterRefreshOf('stage_history', function() {
        _bindHistoryLink(id, url, page_num);
      });
      resolve();
    }));
  };

  init.prototype.bindConfigChangeModal = function(modalId, contentSelector) {
    $(".stage_history .config_change a").click(function(event) {
      event.preventDefault();
      AjaxRefreshers.disableAjax();
      const url = $(this).attr("href");
      $.ajax({
        url: url,
        success: function(data) {
          $(contentSelector).html($(data).find("#body_content").html());
          document.getElementById(modalId).showModal();
        },
        error: function(response, textStatus) {
          $(contentSelector).html(`<div class="callout">There was an error loading changes (${response.status} ${textStatus})</div>`);
          document.getElementById(modalId).showModal();
        }
      });
    });

    $("#modal-close").click(function(event) {
      event.preventDefault();
      document.getElementById(modalId).close();
      AjaxRefreshers.enableAjax();
    });
  };

  return new init();
}();
