/*
Using the gadget_toolkit module
===============================
in your gadget.xml spec:

<Module>
  <ModulePrefs>
    <OAuth>
      <Service name="YourServiceName">
        ...
      </Service>
    </OAuth>
  </ModulePrefs>
  ...
  <Content type="html">
    <!DOCTYPE html><!-- make IE happy -->
    
    <!-- your usual css and JS -->
    <link href="..." media="screen" rel="stylesheet" type="text/css"></link>
    <script type="text/javascript" src="your.application.js"></script>

    <!-- our toolkit javascript -->
    <script type="text/javascript" src="gadget_toolkit.js"></script>

    <script type="text/javascript">
        gadgets.util.registerOnLoadHandler(function() {

          var protectedResourceUrl = "https://host.example.com/protected_content";
          var options = {view: 'gadget_content', serviceName: 'YourServiceName'};

          gadget_toolkit.makeOAuthRequest(protectedResourceUrl, options);
        });
    </script>
    <div id="gadget_content"></div>
  </Content>
</Module>


gadget_toolkit.makeOAuthRequest(url, options) accepts the following options:

Mandatory options:
==================
  * view                      : the id of the <div/> into which we should render out content and errors.
  * serviceName               : the service name from the <OAuth/Service/> tag in the gadget spec.

Optional options:
=================
  * gadgetTitle               : optional string to help gadget toolkit show better authorize message.
  * autoRefresh               : (default false) boolean to indicate whether the gadget should auto refresh itself.
  * refreshInterval           : (default 60000ms) the interval in ms. after which gadget should 
                                refresh iteself if autoRefresh is enabled.
  * onComplete                : callback function that will be triggered after every request completes
                                NOTE: This is invoked even on requests where there is an error
  * networkIndicator          : a javascript object that responds to show() and hide() methods.
                                This allows the application to render a network indicator when
                                a network request starts and finishes.
  * inlineOpeningLinkClasses  : A list of class names for <a/> tags that should be opened inside the gadget.
                                NOTE: When a user clicks on such a link, the toolkit will load the link content
                                      using another OAuth request.
                                NOTE: All other links will be _rewritten_ to 
                                      open in a new browser window <a target='_blank'/>
*/
gadget_toolkit = {};
(function(g) {
  
  function getElementsByClass(searchClass, node, tag) {
    var classElements = new Array();
    if (node == null)
      node = document;
    if ( tag == null )
      tag = '*';
    var els = node.getElementsByTagName(tag);
    var elsLen = els.length;
    var pattern = new RegExp("(^|\\s)"+searchClass+"(\\s|$)");
    for (i = 0, j = 0; i < elsLen; i++) {
      if ( pattern.test(els[i].className) ) {
        classElements[j] = els[i];
        j++;
      }
    }
    return classElements;
  }
  
  function observe(element, event, handler) {
    if (element.addEventListener) {
      element.addEventListener(event, handler, false);
    } else {
      element.attachEvent("on" + event, handler);
    }
  }
  

  g.OAuthGadgetRequest = function(resourceUrl, serviceName, listener) {
    var self = {};
    var httpMethod = (resourceUrl.length < 1024) ? gadgets.io.MethodType.GET : gadgets.io.MethodType.POST;
    var defaultErrorMessage = 'Unknown error when fetching resource.';
    
    var serverErrors = {
        '501': 'The server does not support the functionality required to fulfill the request.',
        '502': 'Cannot connect to server.',
        '503': 'The server is currently unable to handle the request due to temporary overload or server maintenance.',
        '504': 'Timed out waiting for the resource.',
        '505': 'The server does not support the HTTP protocol version that was used in the request message.'
    };
    
    var noneOAuthErrorMessage = function(httpCode, errorBody) {
      if (httpCode === undefined || httpCode === null) {
        return defaultErrorMessage;
      }
      
      if (httpCode === 404) {
        return errorBody || "The resource was not found.";
      }

      if (httpCode >= 500) {
        return serverErrors[httpCode.toString()] || defaultErrorMessage;
      }
      
      return errorBody || defaultErrorMessage;
    };
    
    self.requestCallback = function(response) {
      if (response.oauthApprovalUrl) {
        listener.oauthRequestNeedApproval(self, response.oauthApprovalUrl);
      } else if (response.rc == 200) {
        listener.oauthRequestDataReceived(self, response.text);
      } else if (response.oauthError) {
        listener.oauthRequestOAuthErrorHappened(self, response.oauthError, response.oauthErrorText);
      } else {
        listener.oauthRequestGenericRequestErrorHappened(self, response.rc, noneOAuthErrorMessage(response.rc, response.text));
      }
    };
    
    self.fetchData =  function() {
      var requestURL = resourceUrl;
      var params = {};
      params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.TEXT;
      params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.OAUTH;
      params[gadgets.io.RequestParameters.OAUTH_USE_TOKEN] = "always";
      
      params[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = serviceName;
      params[gadgets.io.RequestParameters.METHOD] = httpMethod;

      if (httpMethod == gadgets.io.MethodType.POST) {
          var urlComponents = resourceUrl.split('?');
          requestURL = urlComponents[0];
          params[gadgets.io.RequestParameters.POST_DATA] = urlComponents[1];
      }
      gadgets.io.makeRequest(requestURL, self.requestCallback, params);
    };
    
    return self;
  };
  
  g.OAuthGadgetDataController = function(dataView, options) {
    var inlineOpenLinkMatcher = new g.GadgetElementClassMatcher(options.inlineOpeningLinkClasses || []);
    var networkIndicator = options.networkIndicator || { show: function() {}, hide: function() {}};
    var onComplete = options.onComplete || function() {};
    var gadgetTitle = options.gadgetTitle;
    var serviceName = options.serviceName;
    var retryAttempts = 0;
    var maxRetryAttempts = 2;
    var autoRefresh = options.autoRefresh;
    var refreshInterval = options.refreshInterval || 60000;
  
    var serverUrl = function(approvalUrl) {
      // figure out the server url, or the application name using either the oauthApprovalUrl or the contentUrl
      var urlParts = approvalUrl.split('/');
      return urlParts[0] + "//" + urlParts[2];
    };
  
    var afterRequestComplete = function() {
      networkIndicator.hide();
      retryAttempts = 0;
      onComplete(dataView);
    };
  
    var hookLinkForInlineOpening = function(linkElement){
      var href = linkElement._fhref || linkElement.href;
      linkElement.href = 'javascript:void(0)';
      linkElement.onclick = "return false";  //to make the link selenium test friendly
      
      observe(linkElement, 'click', function() {
        new g.OAuthGadgetDataController(dataView, options).request(href);
        return false;
      });
    };
  
    // make every links in the dataview, either open inline using oauth request or open in new window
    var adjustLinks = function(links) {
      for (var i = 0; i < links.length; i++) {
        var link = links[i];
        if (inlineOpenLinkMatcher && inlineOpenLinkMatcher.match(link)) {
          hookLinkForInlineOpening(link);
        } else {
          link.target = "_blank";
        }
      }
    };
  
    var controller = {
      request: function(resourceUrl) {
        networkIndicator.show();
        new g.OAuthGadgetRequest(resourceUrl, serviceName, controller).fetchData();
      },
      
      oauthRequestNeedApproval: function(request, approvalUrl) {
        dataView.showInfoMessage("<p>In order to view "
                                  + gadgets.util.escapeString(gadgetTitle || "this")
                                  +" gadget, you must first authorize this site to get content from the "
                                  + gadgets.util.escapeString(serviceName)
                                  + " Server "
                                  + gadgets.util.escapeString(serverUrl(approvalUrl))
                                  + "</p>"
                                  + "<a id='popup' href='javascript:void(0)' onclick='return false'>Authorize</a>");
                                  
        var authorizeLink = document.getElementById("popup");
        var popup = new gadgets.oauth.Popup(approvalUrl, "height=550,width=900", 
                function() { authorizeLink.style.display = 'none'; }, 
                function() { request.fetchData(); });
        observe(authorizeLink, 'click', popup.createOpenerOnClick());
        afterRequestComplete();
      },
    
      oauthRequestDataReceived: function(request, data) {
        dataView.setGadgetContent(data);
        adjustLinks(dataView.links());
        afterRequestComplete();
        if (autoRefresh) {
          setTimeout(request.fetchData, refreshInterval);
        };
      },

      oauthRequestOAuthErrorHappened: function(request, errorCode, errorText) {
        var message = 'OAuth error (' + errorCode + ')';
        if (errorText) {
          message += ': ' + errorText;
        }
        dataView.showErrorMessage(message);
        afterRequestComplete();
      },

      oauthRequestGenericRequestErrorHappened: function(request, errorCode, errorDesc) {
        if(errorCode >= 500 && retryAttempts < maxRetryAttempts) {
          retryAttempts += 1;
          request.fetchData();
        } else {
          if (errorCode >= 500 && autoRefresh) {
            dataView.showErrorMessage(errorDesc + ' Retrying after ' + refreshInterval / 1000 + ' seconds');
            afterRequestComplete();
            setTimeout(function() {
              dataView.setGadgetContent("");
            }, refreshInterval);
            setTimeout(request.fetchData, refreshInterval);
          } else {
            dataView.showErrorMessage(errorDesc);
            afterRequestComplete();
          }
        }
      }
    };
    return controller;
  };


  // A element matcher using class name, nothing fancy but fast and not depening on prototype or jquery
  g.GadgetElementClassMatcher = function(classNames) {
    var matchingClassRegexpStr = "";
  
    for(var i=0;  i < classNames.length; i++ ) {
      matchingClassRegexpStr += classNames[i];
      if ( i < classNames.length - 1 ) {
        matchingClassRegexpStr += '|';
      }
    }
    var matchingClassRegexp = new RegExp("(^|\\s)(" + matchingClassRegexpStr + ")(\\s|$)");
  
    return {
      match: function(element) {
        if(!element) { return false; }
        var elementClassName = element.className;
        if (elementClassName.length <= 0) { return false; }
        return matchingClassRegexp.test(elementClassName);
      }
    };
  };

  var SimpleGadgetDataView = function(elementId, inlineOpenLinkMatcher) {
    var element = document.getElementById(elementId);
    
    var view = {};
  
    view.setGadgetContent = function(content) {
      element.innerHTML = "";
      element.innerHTML = content;
      gadgets.window.adjustHeight(document.body.outerHeight);
    };
      
    view.getElementById = function(id) {
      return document.getElementById(id);
    };
    
    view.getElementsByClassName = function(className) {
      return getElementsByClass(className, element);
    };
  
    view.links = function() {
      return element.getElementsByTagName('a');
    };
  
    view.showErrorMessage = function(message) {
      view.setGadgetContent('<div class="gadget-message error-box">' + message + "</div>");
    };
  
    view.showInfoMessage = function(message) {
      view.setGadgetContent('<div class="gadget-message info-box">' + message + '</div>');
    };
    
    return view;
  };

  var GadgetPrintingView = function(gadgetView, stylesheetUrl) {
    var printingWindow = window.open("about:blank", '_blank');
    var isStyleSheetWritten = false;

    var makeImagesSrcFullUrl = function() {
      var imgs = printingWindow.document.body.getElementsByTagName("img");
      for(var i=0; i < imgs.length; i++) {
        var img = imgs[i];
        img.setAttribute("src", convertPathToUrl(img.getAttribute("src")));
      }
    };
  
    var writeOutPrintStyleSheet = function () {
      if(isStyleSheetWritten) return;
      
      var head = printingWindow.document.getElementsByTagName("head")[0];
      var cssNode = printingWindow.document.createElement('link');
      cssNode.type = 'text/css';
      cssNode.rel = 'stylesheet';
      cssNode.href = window.location.protocol + gadgets.io.getProxyUrl(stylesheetUrl) 
                    + "&nocache=" + (gadgets.util.getUrlParameters().nocache || '0');
      cssNode.media = 'all';
      head.appendChild(cssNode);
      isStyleSheetWritten = true;
    };
  
    var convertPathToUrl = function(path) {
      if (path.indexOf('/') != 0) {
        return path;
      }
      return window.location.protocol + "//" + window.location.host + path;
    };
    
    var view = {};
    
    view.setGadgetContent = function(content) {
      if (printingWindow) {
        printingWindow.focus();
        printingWindow.document.body.innerHTML = "";
        writeOutPrintStyleSheet();
        printingWindow.document.body.innerHTML = content;
        makeImagesSrcFullUrl();
      }
    };
      
    view.setTitle = function(theTitle) {
      printingWindow.document.title = theTitle;        
    };
      
    view.getElementById = function(id) {
      return printingWindow.document.getElementById(id);
    };
    
    view.getElementsByClassName = function(className) {
      return getElementsByClass(className, printingWindow.document);
    };
  
    view.links = function() {
      return printingWindow.document.body.getElementsByTagName('a');
    };
  
    view.showErrorMessage = gadgetView.showErrorMessage;
    view.showInfoMessage =  gadgetView.showInfoMessage;
    return view;
  };
  
  g.createDataView = function(element) {
    return new SimpleGadgetDataView(element);
  };
  
  g.makeOAuthRequest = function(resourceUrl, options) {
    var dataView = new SimpleGadgetDataView(options.view);
    
    if (options.print) {
      dataView = new GadgetPrintingView(dataView, options.printingCss);
    }
    
    new g.OAuthGadgetDataController(dataView, options).request(resourceUrl);
  };

})(gadget_toolkit);