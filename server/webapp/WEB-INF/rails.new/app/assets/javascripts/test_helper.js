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

var MockXHR = Class.create({
  initialize: function(responseHeaders, responseText){
    this.status = 0;
    this.readyState = 0;
    this.onreadystatechange = Prototype.emptyFunction;
    this.headers = responseHeaders ? responseHeaders : {};
    this.setResponseCode();
    this.responseText = responseText ? responseText : '';
  },
  setResponseHeaders: function(headers){
    this.headers;
  },
  setResponseText: function(text){
    this.responseText = text;
  },
  setResponseCode: function(){
    if(this.headers.status){
      this.status = this.headers.status;
    }
    if(this.headers.statusText){
      this.statusText = this.headers.statusText;
    }
  },
  process: function(){
    if(this.readyState < 0 || this.readyState > 4){
      return;
    }
    this.readyState++;
    this.onreadystatechange();
    this.process();
  },
  /* mock methods */
  getResponseHeader: function(name){
    return this.headers[name];
  },
  setRequestHeader: function(name, value){
    //do nothing
  },
  open: function(method, url, asynchronous){
    
  },
  send: function(body){
    this.process();
  }
});

function prepareMockRequest(responseHeaders, responseText){
  window.Ajax.getTransport = function(){
    return new MockXHR(responseHeaders, responseText);
  };
}