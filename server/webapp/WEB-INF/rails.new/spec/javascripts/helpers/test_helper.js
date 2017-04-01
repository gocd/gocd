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

        this.headersSentInRequest = {};
        this.bodyOfRequest = "";
        this.urlOfRequest = "";
        this.methodOfRequest = "";
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
        this.headersSentInRequest[name] = value;
    },
    open: function(method, url, asynchronous){
        this.urlOfRequest = url;
        this.methodOfRequest = url;
    },
    send: function(body){
        this.bodyOfRequest = body;
        this.process();
    }
});

function getReadyToMockRequests() {
    window.originalTransport = window.Ajax.getTransport;
}

function cleanUpMockRequests() {
    window.Ajax.getTransport = window.originalTransport;
    window.originalTransport = null;
    window.currentTransport = null;
}

function prepareMockRequest(responseHeaders, responseText){
    window.Ajax.getTransport = function() {
        window.currentTransport = new MockXHR(responseHeaders, responseText);
        return window.currentTransport;
    };
}

function getHeadersOfPreviousRequest() {
    return window.currentTransport.headersSentInRequest;
}

function getBodyOfPreviousRequest() {
    return window.currentTransport.bodyOfRequest;
}

function getUrlOfPreviousRequest() {
    return window.currentTransport.urlOfRequest;
}

/*
 * Minimal classList shim for IE 9
 * By Devon Govett
 * MIT LICENSE
 */


 if (!("classList" in document.documentElement) && Object.defineProperty && typeof HTMLElement !== 'undefined') {
    Object.defineProperty(HTMLElement.prototype, 'classList', {
        get: function() {
            var self = this;
            function update(fn) {
                return function(value) {
                    var classes = self.className.split(/\s+/),
                    index = classes.indexOf(value);

                    fn(classes, index, value);
                    self.className = classes.join(" ");
                }
            }

            var ret = {
                add: update(function(classes, index, value) {
                    ~index || classes.push(value);
                }),

                remove: update(function(classes, index) {
                    ~index && classes.splice(index, 1);
                }),

                toggle: update(function(classes, index, value) {
                    ~index ? classes.splice(index, 1) : classes.push(value);
                }),

                contains: function(value) {
                    return !!~self.className.split(/\s+/).indexOf(value);
                },

                item: function(i) {
                    return self.className.split(/\s+/)[i] || null;
                }
            };

            Object.defineProperty(ret, 'length', {
                get: function() {
                    return self.className.split(/\s+/).length;
                }
            });

            return ret;
        }
    });
}
