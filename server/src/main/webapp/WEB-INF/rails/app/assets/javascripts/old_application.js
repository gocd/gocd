/*
 * Copyright 2023 Thoughtworks, Inc.
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
function iframeResizer(iframe){
  if (iframe.is(':visible') && iframe.attr('sandbox') === undefined) {
    iframe.contents().find("body").css({margin:0});
    iframe.contents().find("body > pre").css({whiteSpace:'pre-wrap',margin : 0,fontSize : '12px',fontFamily: 'consolas, monaco, menlo, courier', wordWrap:'break-word', '-ms-word-wrap':'break-word', overflow:'hidden'});
    setTimeout(function(){
      jQuery('iframe').each(function(){
        iframe.height(iframe.contents().find("body").height());
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
  jQuery('.sub_tabs_container ul li').click(function(){loadIframes();});
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

//breadcrumb shadow
jQuery(function(){
  jQuery(window).scroll(function(){
    if(jQuery(this).scrollTop() > 50)
      jQuery('.page_header').addClass('shadow');
    if(jQuery(this).scrollTop() < 50)
      jQuery('.page_header').removeClass('shadow');
  });
});
