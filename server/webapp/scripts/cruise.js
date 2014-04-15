/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END**********************************/

(function($){
    $.fn.getOuterHtml = function() {
        if(this[0]){
            var html = this[0].outerHTML;
            if (!html) {
                var div = this[0].ownerDocument.createElement("div");
                div.appendChild(this[0].cloneNode(true));
                html = div.innerHTML;
            }
            return html;
        }
    }
    $.fn.showInModal = function(callback) {
        var form = this;
        $(this).find('input[type="submit"]').click(function(event){
            event.preventDefault();
            var html = $(form).getOuterHtml().replace(/<noscript>/ig, '').replace(/<\/noscript>/ig, '');
            console.log(html);
            $(html).modal({
                    close:false,
                    position: ["20%",],
                    overlayId:'confirmModalOverlay',
                    containerId:'confirmModalContainer',
                    onShow: function (dialog) {
                        dialog.data.find('.message').append(html);

                        // if the user clicks "yes"
                        dialog.data.find('.yes').click(function () {
                            // call the callback
                            if ($.isFunction(callback)) {
                                callback.apply();
                            }
                            // close the dialog
                            $.modal.close();
                        });
                    }
                });
        });
    }
})(jQuery);

(function(){
    var Cruise = {
        enhancePipelineSummaryLink: function(selector) {
            $(selector).click(function(event) {
                event.preventDefault();
                var location = $(this).find('.label a').attr('href');
                Cruise.currentPage = new Cruise.HistoryPage(location);
                $("#primary-content").load(location.replace(/pipelineHistoryPage/i, "pipelineSummary"));
                $("#secondary-content").html('').load(location.replace(/pipelineHistoryPage/i, "pipelineChanges"));
            });
        },

        enhanceStageSummaryLink: function(selector) {
            $(selector).click(function(event) {
                event.preventDefault();
                event.stopPropagation();
                console.log(this.href)
                Cruise.currentPage = new Cruise.HistoryPage(this.href);
                $("#primary-content").load(this.href.replace(/pipelineHistoryPage/i, "pipelineSummary"));
                $("#secondary-content").load(this.href.replace(/pipelineHistoryPage/i, "stageSummary"));
            });
        },

        enhanceJobTabs: function(){
            var tabsWrapper = $('.job-tabs .job-tab');
            tabsWrapper.click(function(){
                tabsWrapper.removeClass('current');
                $(this).addClass('current');
            });
        },

        enhancePipelineActions: function(){
            $('#sidebar form.pause').showInModal();
        }
    }
    Cruise.HistoryPage = function(url){
        if(!url){
            url = window.location.href;
        }
        var page = this;
        var params = ['pipelineName', 'pipelineLabel', 'stageName', 'stageCounter', 'jobName'];

        var parseParamFromUrl = function(paramName) {
            var exp = new RegExp(paramName + '=([^&]*)', 'i')
            var result = url.match(exp);
            page[paramName] = undefined;

            if(result != null){
                page[paramName] = result[1];
                return result[1];
            }
        }

        var parseParamsFromUrl = function(paramNames){
            for(var i = 0;i<paramNames.length; i++){
                parseParamFromUrl(paramNames[i]);
            }
        }

        var toQueryString = function(paramNames){
            console.log(Cruise.currentPage)
            var pairs = [];
            for(var i = 0;i<paramNames.length;i++){
                var paramName = paramNames[i];
                pairs.push(paramName + '=' + (Cruise.currentPage[paramName] || ''));
            }
            return pairs.join('&');
        }

        var enhanceLinks = function(){
            Cruise.enhanceStageSummaryLink('#sidebar .stage a');
            Cruise.enhancePipelineSummaryLink('#sidebar .pipeline');
            Cruise.enhancePipelineActions();
            Cruise.enhanceJobTabs();
        }

        enhanceLinks();

        parseParamsFromUrl(params);
        Cruise.currentPage = page;

        $("#sidebar-content").at_intervals(function() {
            console.log(toQueryString(params))
            $("#sidebar-content").load('pipelineHistory?' + toQueryString(params), enhanceLinks);
            //$("#sidebar-content").load(window.location.href.replace(/pipelineHistoryPage/i, "pipelineHistory").replace(/pipelineLabel=\d+/i, "pipelineLabel=latest"), enhanceLinks);
            $("#primary-content").load("pipelineSummary?" + toQueryString(params), enhanceLinks);
        }, { name: "interval-updating", delay: 10000 });
    }

    Cruise.HistoryPage.onResize = function(){
        $('#job-content iframe').attr('width', $('#job-content').innerWidth());
    }

    Cruise.fixIframeHight = function() {
        // Set specific variable to represent all iframe tags.
        var iFrames = document.getElementsByTagName('iframe');

        // Resize heights.
        function iResize() {
            // Iterate through all iframes in the page.
            for (var i = 0, j = iFrames.length; i < j; i++) {
                // Set inline style to equal the body height of the iframed content.
                iFrames[i].style.height = iFrames[i].contentWindow.document.body.offsetHeight + 'px';
            }
        }

        // Check if browser is Safari or Opera.
        if ($.browser.safari || $.browser.opera) {
            // Start timer when loaded.
            $('iframe').load(function() {
                setTimeout(iResize, 0);
            });

            // Safari and Opera need a kick-start.
            for (var i = 0, j = iFrames.length; i < j; i++) {
                var iSource = iFrames[i].src;
                iFrames[i].src = '';
                iFrames[i].src = iSource;
            }
        } else {
            // For other good browsers.
            $('iframe').load(function() {
                // Set inline style to equal the body height of the iframed content.
                this.style.height = this.contentWindow.document.body.offsetHeight + 50 + 'px';
            });
        }
    }

    Cruise.pauseAllUpdating = function(){
        $("#sidebar-content").data("interval-updating").should_pause = true;
        Cruise.updating = false;
    }
    
    Cruise.resumeAllUpdating = function(){
        $("#sidebar-content").data("interval-updating").should_pause = false;
        Cruise.updating = true;
    }

    window.Cruise = Cruise;
})();

$(document).ready(function() {
    //detect browser limitation
    if($.browser.mozilla && $.browser.version < "1.9.1") {
        $(document.body).addClass('firefox-less-than-31');
    }

    if(window.location.href.match(/.*pipelineHistoryPage.*/i)){
        Cruise.currentPage = new Cruise.HistoryPage();
        Cruise.HistoryPage.onResize();
        $(window).bind('resize', Cruise.HistoryPage.onResize);
    }

    Cruise.fixIframeHight();

    $('#footer').click(function(event){
        Cruise.updating ? Cruise.pauseAllUpdating() : Cruise.resumeAllUpdating();
        if($('#footer .copyright .updating-status').size() == 0){
            $('#footer .copyright').append('<div class="updating-status"></div>');
        }
        $('#footer .copyright .updating-status').text('updating is ' + (Cruise.updating ? 'on' : 'off'));
    })
})