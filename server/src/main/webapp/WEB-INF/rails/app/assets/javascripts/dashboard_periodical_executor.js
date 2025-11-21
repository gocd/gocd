/*
 * Copyright Thoughtworks, Inc.
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
function DashboardPeriodicalExecutor(url, pause_condition) {
  this.setUrl(url);
  this.sequenceNumber = 0;
  this.frequency = 5000; //milli-seconds;
  this.observers = [];
  this.is_paused = false;
  this.pause_condition = pause_condition;
}

DashboardPeriodicalExecutor.prototype = {
  start() {
    if (!this.is_paused) {
      this.onRequest();
    }
  },
  stop() {
    clearTimeout(this.timer);
  },
  redirectToLoginPage(){
    window.location= `${window.location.protocol}//${window.location.host}/go/auth/login`;
  },

  /*
     * We should use fireNow function trigger a manual request.
     * So we should call start() first. And when we can't wait for next automatically request,
     * then, we can use this fireNow method.
     */
  fireNow() {
    clearTimeout(this.timer);
    if (this.ongoingRequest && this.ongoingRequest.transport) {
      try {
        this.ongoingRequest.transport.abort();
      } catch {} // eslint-disable-line no-empty
    }
    this.start();
  },

  onRequest() {
    var executor = this;
    var requestSequenceNumber = this.generateSequenceNumber();
    this.ongoingRequest = $.ajax({
      url: executor.url,
      dataType: "json",
      success(json_array) {
        executor._loop_observers(json_array, requestSequenceNumber);
        if (executor.pause_condition && executor.pause_condition(json_array)) {
          executor.is_paused = true;
        }
      },
      error(jqXHR, textStatus) {
      },
      complete() {
        //makes sure only 1 timer in this executor
        clearTimeout(executor.timer);
        delete executor.timer;

        if (!executor.is_paused) {
          executor.timer = setTimeout(executor.onRequest.bind(executor), executor.frequency);
        }
        //avoid memory leak
        executor = null;
        requestSequenceNumber = null;
      },
      statusCode: {
        401 () {
          executor.redirectToLoginPage();
        }
      }
    });

  },
  _loop_observers(json, requestSequenceNumber) {
    if (json.error) {
      return;
    }

    for (var index = 0; index < this.observers.length; index++) {
      var observer = this.observers[index];

      if (!observer.notify) {return;}

      if (observer.dropExpiredCallback && !this.isSequenceNumberValid(requestSequenceNumber)) {
        //this request will be dropped, because it's expired
        return;
      }

      if (!this.is_paused) {
        observer.notify(json);
      }
    }
  },
  register() {
    for (var i = 0; i < arguments.length; i++) {
      this.observers.push(arguments[i]);
    }
  },
  unregister(observer) {
    this.observers = Array.from(this.observers);
    var position = this.observers.indexOf(observer);
    this.observers[position] = null;
    this.observers = _.compact(this.observers);
  },
  clean() {
    this.observers = [];
    this.is_paused = false;
  },
  pause() {
    this.is_paused = true;
  },
  resume() {
    this.is_paused = false;
  },
  generateSequenceNumber() {
    this.sequenceNumber++;
    return this.sequenceNumber;
  },
  isSequenceNumberValid(request_sequence_number){
    return this.sequenceNumber == request_sequence_number;
  },
  setUrl(url){
    try{
      this.url = context_path(url);
    } catch(e){
      this.url = `/go/${url}`;
    }
  }
};
