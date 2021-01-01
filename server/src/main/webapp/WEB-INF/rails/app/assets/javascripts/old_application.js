/*
 * Copyright 2021 ThoughtWorks, Inc.
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
// monkey patch ajax GETs: append current time so that IE does not use cached result
Ajax.Request.prototype.__request = Ajax.Request.prototype.request;
Ajax.Updater.prototype.__request = Ajax.Updater.prototype.request;
Ajax.Request.prototype.__respondToReadyState = Ajax.Request.prototype.respondToReadyState;
Ajax.Updater.prototype.__respondToReadyState = Ajax.Updater.prototype.respondToReadyState;

Ajax.RequestOverriden = {
  requestIndex: 1,
  request: function(url) {
    var now = new Date();
    this.requestIndex += 1;
    if (this.options.method == 'get') {
      url += (url.include('?') ? '&' : '?') + "ms=" + now.getTime() + "_" + this.requestIndex;
    }
    this.__request(url);
  },

  //disable event dispatching for interactive stage, which cause a hug memory spike on more than 2m data transfered
  respondToReadyState: function(readyState) {
    if(readyState == 3) { return; }
    this.__respondToReadyState(readyState);
  }
};
Object.extend(Ajax.Request.prototype, Ajax.RequestOverriden);
Object.extend(Ajax.Updater.prototype, Ajax.RequestOverriden);

function context_path(path_info) {
  if (path_info && path_info.startsWith(contextPath)) {
    return path_info;
  }
  var pathSeparator = (contextPath.endsWith("/") || path_info.startsWith("/") ? "" : "/");
  return contextPath + pathSeparator + path_info;
}

var CruiseBasicService = {
    redirectToLoginPage: function(){
        window.location= window.location.protocol + '//' + window.location.host + context_path('auth/login');
    }
};

Ajax.Responders.register({
    onComplete: function(request, transport, json){
        if(transport && (transport.status && transport.status == 401
                        || transport.statusText && transport.statusText == '401')) {
            //if 401 has been handled, just return
            if(request && request.options && request.options.on401) return;
            CruiseBasicService.redirectToLoginPage();
        }
    }
});


//iframe resizer + back to top
function iframeResizer(Iframe){
    if(Iframe.is(':visible')) {
        Iframe.contents().find("body").css({margin:0});
        Iframe.contents().find("body > pre").css({whiteSpace:'pre-wrap',margin : 0,fontSize : '12px',fontFamily: 'consolas, monaco, courier', wordWrap:'break-word', '-ms-word-wrap':'break-word', overflow:'hidden'});
        setTimeout(function(){
            jQuery('iframe').each(function(){
                Iframe.height(Iframe.contents().find("body").contents().height());
            });
        }, 200);
    }
}
 function loadIframes(){
     jQuery('iframe').each(function(){
         jQuery(this).off('load').on('load',function(){
             iframeResizer(jQuery(this));
         });
     });
 }

//Back to top button
jQuery(function () {
    loadIframes();//initial loading of all iframes
    jQuery(window).on('resize',function(){
            jQuery('iframe').each(function(){
                iframeResizer(jQuery(this));
            });
    });
    jQuery('.sub_tabs_container ul li').click(function(){loadIframes()});
    jQuery(window).scroll(function(){
        if(jQuery(this).scrollTop() > 300)
            jQuery('.back_to_top').fadeIn(800);
        if(jQuery(this).scrollTop() < 300)
            jQuery('.back_to_top').fadeOut(200);

    });
    jQuery('#back_to_top,.back-to-top-in-console').click(function(){
        jQuery('body,html').stop(true).animate({scrollTop : 0},'fast');
    });
});

//highlight your fields
function highlightElement(element) {
    element.stop().css('background-color','#ffe8aa').animate({backgroundColor:'#fff'},1500);
}

//breadcrumb shadow
jQuery(function(){
    jQuery(window).scroll(function(){
        if(jQuery(this).scrollTop() > 50)
            jQuery('.page_header').addClass('shadow');
        if(jQuery(this).scrollTop() < 50)
            jQuery('.page_header').removeClass('shadow');
    });
});
