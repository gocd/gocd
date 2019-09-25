/*
 * Copyright 2019 ThoughtWorks, Inc.
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

/* Only one popup is available in same time */
var ExclusivePopup = new (Class.create({
    create: function(id){
        if(this.isValid() && this.id != id){
            this.reset();
        }
        this.id = id;
        return this;
    },
    linkElement: function(){
        return $(this.id);
    },
    popupInnerContainer: function(){
        return this.linkElement().select('.build-cause-summary-container').first();
    },
    popupOutterContainer: function(){
        return this.linkElement().select('.popup').first();
    },
    isValid: function(){
        return Object.isElement(this.linkElement());
    },
    isOpen: function(){
        return !this.linkElement().hasClassName('popup-closed') && this.linkElement().hasClassName('popup-open');
    },
    getStatus: function(id){
        return this.id == id && this.isOpen() ? 'open' : 'closed';
    },
    getStyleText: function(id){
        return this.id == id && this.isOpen() && this.popupDimension ?
               'width:' + this.popupDimension.width + 'px;height:' + this.popupDimension.height + 'px': '';
    },
    getPositionClass: function(id){
        return this.id == id && this.isOpen() && this.positionClass ?
               this.positionClass : '';
    },
    toggle: function(){
        if(!this.isValid()){
            return;
        }

        if(this.isOpen()){
            this.hide();
        } else {
            this.show();
        }
    },
    hide: function(){
        delete(this.popupDimension);
        delete(this.positionClass);
        this.linkElement().addClassName('popup-closed').removeClassName('popup-open');
        Event.stopObserving(document, 'click', this.onGlobalClick);
    },
    show: function(){
        var linkElement = this.linkElement();
        var innerElement = this.popupInnerContainer();
        var outterElement = this.popupOutterContainer();
        outterElement.setStyle({visibility: 'hidden'});
        linkElement.addClassName('popup-open').removeClassName('popup-closed');
        var dimension = this.getMaxDimensionIn(innerElement);
        var positionClass = this.popupPositionClass(linkElement, innerElement, dimension);
        this.popupDimension = {
            height: dimension.height + 30,
            width: dimension.width > 520 ? dimension.width : null
        };
        this.positionClass = positionClass;
        outterElement.setStyle({
            height: this.popupDimension.height + 'px',
            width: this.popupDimension.width ? this.popupDimension.width + 'px' : ''
        });
        outterElement.setAttribute('class', 'popup ' + positionClass);
        outterElement.setStyle({visibility: ''});
        Event.observe(document, 'click', this.onGlobalClick);
    },
    getMaxDimensionIn: function(container){
        var dimension = container.getDimensions();
        var maxWidth = dimension.width;
        var maxHeight = dimension.height;
        Element.select(container, '*').each(function(child){
            if(Element.getWidth(child) > maxWidth){
                maxWidth = Element.getWidth(child);
            }
            if(Element.getHeight(child) > maxHeight){
                maxHeight = Element.getHeight(child);
            }
        });
        return {
            width: maxWidth,
            height: maxHeight
        }
    },
    reset: function(){
        if(this.isValid() && this.isOpen()){
            this.hide();
        }
    },
    onGlobalClick: function(event) {
        var element = Event.element(event);
        if(!element || !element.ancestors) return true;

        var isClickedOutSide = !element.ancestors().any(function(node){
            return node == ExclusivePopup.linkElement();
        });

        if(isClickedOutSide) {
          ExclusivePopup.hide();
        }
        return true;
    }
}));

var PopupPositionAwareModule = {
    calculatePosition: function(link, popup, variebles){
        var position = this._calculatePositionFromViewport(link, popup, variebles);
        if(position != 'bottom-right'){
            return position;
        }
        position = this._calculatePositionFromCanvas(link, popup, variebles);
        if(position != 'bottom-right'){
            return position;
        }

        //If either side has no enough space, use default bottom-right as result
        return 'bottom-right';
    },
    _calculatePositionFromViewport: function(link, popup, variebles){
        var offset_x_in_viewport = variebles.link.offset[0] - variebles.viewport.scrollOffset[0];
        var offset_y_in_viewport = variebles.link.offset[1] - variebles.viewport.scrollOffset[1];

        return this._decidePosition(offset_x_in_viewport + variebles.link.dimensions.width,
          variebles.viewport.dimensions.width - offset_x_in_viewport - variebles.link.dimensions.width,
          offset_y_in_viewport,
          variebles.viewport.dimensions.height - offset_y_in_viewport - variebles.link.dimensions.height,
          variebles);
    },
    _calculatePositionFromCanvas: function(card, popup, variebles){
        var offset_x_in_canvas = variebles.link.offset[0] - variebles.link.scrollOffset[0];
        var offset_y_in_canvas = variebles.link.offset[1] - variebles.link.scrollOffset[1];

        return this._decidePosition(offset_x_in_canvas + variebles.link.dimensions.width,
          variebles.canvas.dimensions.width - offset_x_in_canvas - variebles.link.dimensions.width,
          offset_y_in_canvas,
          variebles.canvas.dimensions.height - offset_y_in_canvas - variebles.link.dimensions.height,
          variebles);
    },
    _decidePosition: function(leftSpace, rightSpace, topSpace, bottomSpace, variebles){
        var reviseFactor = 20;//this is approximate height of the popup arrow
        var width = variebles.popup.dimensions.width;
        var height = variebles.popup.dimensions.height;
        var firstPart, lastPart;

        if(rightSpace >= width){
          lastPart = 'right';
        } else if (leftSpace >= width){
          lastPart = 'left';
        } else {
          lastPart = 'right'; //right is default popup position
        }

        if (bottomSpace >= height + reviseFactor){
          firstPart = 'bottom';
        } else if(topSpace >= height + reviseFactor){
          firstPart = 'top';
        } else {
          firstPart = 'bottom'; //bottom is default popup position
        }

        return firstPart + '-' + lastPart;
    },
    popupPositionClass: function(link, popup, popupDimensions){
        var variebles = this.getVariebles(link, popup, popupDimensions);
        return 'popup-at-' + this.calculatePosition(link, popup, variebles);
    },
    getVariebles: function(link, popup, popupDimensions){
        var canvasDimensions = $(document.body).getDimensions();
        var viewportDimensions = document.viewport.getDimensions();
        var viewportScrollOffsets = document.viewport.getScrollOffsets();

        var linkOffset = link.cumulativeOffset();
        var linkScrollOffset = link.cumulativeScrollOffset();
        var linkDimensions = link.getDimensions();

        return {
          canvas: {dimensions: canvasDimensions},
          viewport: {
            dimensions: viewportDimensions,
            scrollOffset: viewportScrollOffsets
          },
          link: {
            offset: linkOffset,
            scrollOffset: linkScrollOffset,
            dimensions: linkDimensions
          },
          popup: {dimensions: popupDimensions}
        }
    }
};

Object.extend(ExclusivePopup, PopupPositionAwareModule);

var CruiseBasicService = {
    nullStringRepresentation : function() {
        return 'not-set';
    },
    redirectToLoginPage: function(){
        window.location= window.location.protocol + '//' + window.location.host + context_path('auth/login');
    },
    convertNullString: function(str) {
        if(str == 'null'){
            return this.nullStringRepresentation();
        }
        return str;
    },
    nonSystemArtifactsIn: function(artifacts) {
        return artifacts.reject(function(artifact) {
            return (
                artifact.Src == 'cruise-output/log.xml' ||
                artifact.Src == 'cruise-output\\log.xml' ||
                artifact.Src == 'cruise-output/console.log' ||
                artifact.Src == 'cruise-output\\console.log');
        });
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
