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
//iframe resizer + back to top
function iframeResizer(iframe){
  if (iframe.is(':visible') && iframe.attr('sandbox') === undefined) {
    iframe.contents().find("body").css({margin:0});
    iframe.contents().find("body > pre").css({whiteSpace:'pre-wrap',margin : 0,fontSize : '12px',fontFamily: 'consolas, monaco, menlo, courier', wordWrap:'break-word', overflow:'hidden'});
    setTimeout(function(){
      $('iframe').each(function(){
        iframe.height(iframe.contents().find("body").height());
      });
    }, 200);
  }
}
function loadIframes(){
  $('iframe').each(function(){
    $(this).off('load').on('load',function(){
      iframeResizer($(this));
    });
  });
}

//Back to top button
$(function () {
  loadIframes();//initial loading of all iframes
  $(window).on('resize',function(){
    $('iframe').each(function(){
      iframeResizer($(this));
    });
  });
  $('.sub_tabs_container ul li').click(function(){loadIframes();});
  $(window).scroll(function(){
    if($(this).scrollTop() > 300)
      $('.back_to_top').fadeIn(800);
    if($(this).scrollTop() < 300)
      $('.back_to_top').fadeOut(200);

  });
  $('#back_to_top,.back-to-top-in-console').click(function(){
    $('body,html').stop(true).animate({scrollTop : 0},'fast');
  });
});

//breadcrumb shadow
$(function(){
  $(window).scroll(function(){
    if($(this).scrollTop() > 50)
      $('.page_header').addClass('shadow');
    if($(this).scrollTop() < 50)
      $('.page_header').removeClass('shadow');
  });
});
