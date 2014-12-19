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

var tw_gadget = function() {
  var lastMoudleId = 0;
  var renderingUrl = null;
  
  function resizeHeight(height) {
    var iframe = document.getElementById(this.f);
    if(iframe) {
      iframe.style.height = height + "px";
    }
  }

  function buildupIframeUrl(moduleId, gadgetUrl, params) {
    var url =  renderingUrl;
    url += "?url=" + encodeURIComponent(gadgetUrl);
    url += "&parent=" + encodeURIComponent(document.location.href);
    url += "&mid=" + encodeURIComponent(moduleId);
    for (var key in params) {
      url += "&" + key + "=" + encodeURIComponent(params[key]);
    }
    return url;
  }

  function buildIframeId(moduleId) {
    return "gadget_iframe_" + moduleId;
  }

  return {
    init: function(url) {
      renderingUrl = url || '/gadgets/ifr';
      if(typeof gadgets != "undefined") {
        gadgets.rpc.register('resize_iframe', resizeHeight);
      }
    },
 
    addGadget: function(containerId, gadgetUrl, params) {
      params = params || {};
      var container = document.getElementById(containerId);
      var wrapper = document.createElement("div");
      wrapper.className = "gadget-wrapper";
      
      
      var moduleId = lastMoudleId++;
      
      var iframeId = buildIframeId(moduleId);
      
      wrapper.innerHTML = ('<iframe id="' +
          iframeId + '" name="' + iframeId + '" src="about:blank' +
          '" frameborder="no" scrolling="no"' +
          (params.height ? ' height="' + params.height + '"' : '') +
          (params.width ? ' width="' + params.width + '"' : '') +
          '></iframe>');
      container.appendChild(wrapper);
      
      document.getElementById(iframeId).src = buildupIframeUrl(moduleId, gadgetUrl, params);
    }
  };
}();