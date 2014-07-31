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

var DashboardPeriodicalExecuter = Class.create();

DashboardPeriodicalExecuter.prototype = {
    initialize: function(url, frequency) {
        this.setUrl(url);
        this.sequenceNumber = 0;
        if (typeof frequency == "undefined") {
            frequency = 5; //seconds
        }
        this.frequency = frequency;
        this.observers = [];
        this.is_execution_start = false;
        this.is_paused = false;
        this.ignore_duplicate_response = true;
    },
    start: function() {
        this.is_execution_start = true;
        this.onRequest();
    },
    stop: function() {
        clearTimeout(this.timer);
        this.is_execution_start = false;
    },
    redirectToLoginPage: function(){
        window.location= window.location.protocol + '//' + window.location.host + context_repath('auth/login');
    },
    redirectToAboutPage : function () {
        window.location= window.location.protocol + '//' + window.location.host + context_path('about');
    },
    changeFrequency : function(newFrequency) {
        this.frequency = newFrequency;
    },
    /*
     * We should use fireNow function trigger a manual request.
     * So we should call start() first. And when we can't wait for next automatically request,
     * then, we can use this fireNow method.
     */
    fireNow : function() {
        clearTimeout(this.timer);
        if(this.ongoingRequest && this.ongoingRequest.transport){
            try{
                this.ongoingRequest.transport.abort();
            }catch(e){}
        }
        this.onRequest();
    },
    onRequest: function(){
        var executer = this;
        var requestSequenceNumber = this.generateSequenceNumber();
        this.ongoingRequest = new Ajax.Request(this.url, {
            method: 'GET',
            onSuccess: function(transport){
                executer._loop_observers(transport, requestSequenceNumber);
            },
            onException: function(transport, exception){
                executer.showError('Server cannot be reached (exception). Either there is a network problem or the server is down.', exception.toString());
            },
            on404: function(){
                executer.showError('Server cannot be reached (404). Either there is a network problem or the server is down.');
            },
            on401: function(){
                executer.redirectToLoginPage();
            },
            on402: function(){
                executer.redirectToAboutPage();
            },
            on500: function(transport){
                executer.showError('The server encountered an internal problem.', transport.responseText);
            },
            onFailure: function(){
                //onFailure means http response a header code (< 200) or (> 300)
                executer.showError('Server cannot be reached (failure). Either there is a network problem or the server is down.');
            },
            onComplete: function(){
                executer.onRequestComplete.apply(executer);
                //avoid memory leak
                executer = null;
                requestSequenceNumber = null;
            }
        });
    },
    onRequestComplete: function(){
        //makes sure only 1 timer in this executer
        clearTimeout(this.timer);
        this.timer = this.onRequest.bind(this).delay(this.frequency);
    },
    showError: function(title, body){
        FlashMessageLauncher.error(title, body);
    },
    _loop_observers : function(transport, requestSequenceNumber) {
        if(!transport || !transport.responseText){                                                
            return;
        }
        
        var jsonText = transport.responseText;
        //ignore json if there is no changes, you can set ignore_duplicate_response to false when in debug mode 
        this._json_text_cache = jsonText;

        var json;

        try{
            json = eval('('+jsonText+')');
        } catch(e){
            json = [];
            this.showError('The server encountered a problem (json error).', e.toString());
        }
        
        if(json.error){
            this.showError('The server encountered a problem.', json.error);
        }
        
        for(var index = 0; index < this.observers.length; index++){
            var observer = this.observers[index];
            
            if(!observer.notify) return;

            if(observer.dropExpiredCallback && !this.isSequenceNumberValid(requestSequenceNumber)){
                //this request will be dropped, because it's expired
                return;
            }

            if(!this.is_paused){
                observer.notify(json);
            }
        }
    },
    is_start : function() {
        return this.is_execution_start;
    },
    register : function() {
        for (var i = 0; i < arguments.length; i++) {
            this.observers.push(arguments[i]);
        }
    },
    unregister : function(observer) {
        this.observers = $A(this.observers);
        var position = this.observers.indexOf(observer);
        this.observers[position] = null;
        this.observers = this.observers.compact();
    },
    clean : function() {
        this.observers = [];
        this._json_text_cache = undefined;
    },
    pause : function() {
        this.is_paused = true;
    },
    resume: function() {
        this.is_paused = false;
    },
    generateSequenceNumber : function() {
        this.sequenceNumber++;
        return this.sequenceNumber;
    },
    isSequenceNumberValid : function(request_sequence_number){
        return this.sequenceNumber == request_sequence_number;
    },
    setUrl: function(url){
        try{
            this.url = context_path(url);
        } catch(e){
            this.url = '/go/' + url;
        }
    }
}
