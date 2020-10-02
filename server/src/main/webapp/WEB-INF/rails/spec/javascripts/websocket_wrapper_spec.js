/*
 * Copyright 2020 ThoughtWorks, Inc.
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
describe("Websocket Wrapper", function () {
  var MockedWebsocket, oftenWrapper, originalOften;

  beforeEach(function () {
    oftenWrapper = {
      often: jasmine.createSpy(),
      start: jasmine.createSpy(),
      wait:  jasmine.createSpy(),
      done:  jasmine.createSpy()
    };

    originalOften = often;

    often = oftenWrapper.often;
    start = oftenWrapper.start;
    wait  = oftenWrapper.wait;

    often.and.returnValue(oftenWrapper);
    start.and.returnValue(oftenWrapper);
    wait.and.returnValue(oftenWrapper);

    MockedWebsocket = function () {
      this.close = function () {
      }
    };
  });

  afterEach(function () {
    MockedWebsocket = undefined;
    often           = originalOften;

    WebSocketWrapper.TIMEOUT_DEFAULT  = 15000;
    WebSocketWrapper.TIMEOUT_START    = 5000;
    WebSocketWrapper.CLOSE_NORMAL     = 1000;
    WebSocketWrapper.CLOSE_ABNORMAL   = 1006;
    WebSocketWrapper.CLOSE_WILL_RETRY = 4100;
  });

  it("should expose websocket wrapper defaults", function () {
    expect(WebSocketWrapper.CLOSE_NORMAL).toEqual(1000);
    expect(WebSocketWrapper.CLOSE_ABNORMAL).toEqual(1006);
    expect(WebSocketWrapper.CLOSE_WILL_RETRY).toEqual(4100);

    expect(WebSocketWrapper.TIMEOUT_DEFAULT).toEqual(15000);
    expect(WebSocketWrapper.TIMEOUT_START).toEqual(5000);
  });

  it("should add events on the base websocket", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);

    expect(_.isFunction(myWebsocket.ws.onopen)).toEqual(true);
    expect(_.isFunction(myWebsocket.ws.onclose)).toEqual(true);
    expect(_.isFunction(myWebsocket.ws.onmessage)).toEqual(true);
    expect(_.isFunction(myWebsocket.ws.onerror)).toEqual(true);
  });

  it("should emit onopen event when websocket connection is established", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onOpen      = jasmine.createSpy();
    myWebsocket.on("open", onOpen);

    expect(onOpen).not.toHaveBeenCalled();
    expect(onOpen.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();

    expect(onOpen).toHaveBeenCalled();
    expect(onOpen.calls.count()).toEqual(1);
  });

  it("should emit onmessage event when websocket connection receives a message", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onMessage   = jasmine.createSpy();
    myWebsocket.on("message", onMessage);

    expect(onMessage).not.toHaveBeenCalled();
    expect(onMessage.calls.count()).toEqual(0);

    myWebsocket.ws.onmessage({data: "some random junk"});

    expect(onMessage).toHaveBeenCalled();
    expect(onMessage.calls.count()).toEqual(1);
  });

  it("should not emit onmessage event when websocket connection receives a ping message", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onMessage   = jasmine.createSpy();
    myWebsocket.on("message", onMessage);

    expect(onMessage).not.toHaveBeenCalled();
    expect(onMessage.calls.count()).toEqual(0);

    var pingFrame = JSON.stringify({type: 'ping'});
    myWebsocket.ws.onmessage({data: pingFrame});

    expect(onMessage).not.toHaveBeenCalled();
    expect(onMessage.calls.count()).toEqual(0);
  });

  it("should emit onerror event when an error occurs on websocket connection", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onError     = jasmine.createSpy();
    myWebsocket.on("error", onError);

    expect(onError).not.toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onError).toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(1);
  });

  it("should emit initial connection failed event when an error occurs while websocket handshake", function () {
    var options                   = {Websocket: MockedWebsocket};
    var myWebsocket               = new WebSocketWrapper(options);
    var onInitialConnectionFailed = jasmine.createSpy();
    myWebsocket.on("initialConnectFailed", onInitialConnectionFailed);

    expect(onInitialConnectionFailed).not.toHaveBeenCalled();
    expect(onInitialConnectionFailed.calls.count()).toEqual(0);

    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onInitialConnectionFailed).toHaveBeenCalled();
    expect(onInitialConnectionFailed.calls.count()).toEqual(1);
  });

  it("should retry on error event", function (done) {
    var options                    = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };
    var myWebsocket                = new WebSocketWrapper(options);
    WebSocketWrapper.TIMEOUT_START = 0;

    var onError = jasmine.createSpy();
    myWebsocket.on("error", onError);

    expect(onError).not.toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.on("beforeInitialize", done);
    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onError).toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(1);
  });

  it("should not retry on error event when indefiniteRetry is false", function () {
    var options                    = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: false
    };
    var myWebsocket                = new WebSocketWrapper(options);
    WebSocketWrapper.TIMEOUT_START = 0;
    var onError                    = jasmine.createSpy();
    myWebsocket.on("error", onError);

    myWebsocket.on("beforeInitialize", function () {
      fail("Expected callback not to be called");
    });

    expect(onError).not.toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onError).toHaveBeenCalled();
    expect(onError.calls.count()).toEqual(1);
  });

  it("should not retry on error event when initial connection fails", function () {
    var options = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };

    var myWebsocket                = new WebSocketWrapper(options);
    WebSocketWrapper.TIMEOUT_START = 0;
    myWebsocket.on("error", jasmine.createSpy());

    var onBeforeInitialize = jasmine.createSpy();
    myWebsocket.on("beforeInitialize", onBeforeInitialize);

    expect(onBeforeInitialize).not.toHaveBeenCalled();
    expect(onBeforeInitialize.calls.count()).toEqual(0);

    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onBeforeInitialize).not.toHaveBeenCalled();
    expect(onBeforeInitialize.calls.count()).toEqual(0);
  });

  it("should not retry on error event if websocket connection is stopped", function () {
    var options                    = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };
    var myWebsocket                = new WebSocketWrapper(options);
    WebSocketWrapper.TIMEOUT_START = 0;
    myWebsocket.on("error", jasmine.createSpy());

    var onBeforeInitialize = jasmine.createSpy();
    myWebsocket.on("beforeInitialize", onBeforeInitialize);

    expect(onBeforeInitialize).not.toHaveBeenCalled();
    expect(onBeforeInitialize.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.stop();
    myWebsocket.ws.onerror({data: "Boom!"});

    expect(onBeforeInitialize).not.toHaveBeenCalled();
    expect(onBeforeInitialize.calls.count()).toEqual(0);
  });

  it("should emit onclose event when a websocket connection is closed", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onClose     = jasmine.createSpy();
    myWebsocket.on("close", onClose);

    expect(onClose).not.toHaveBeenCalled();
    expect(onClose.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.ws.onclose({data: "Boom!"});

    expect(onClose).toHaveBeenCalled();
    expect(onClose.calls.count()).toEqual(1);
  });

  it("should not emit onclose event when a websocket connection close is an abnormal close", function () {
    var options     = {Websocket: MockedWebsocket};
    var myWebsocket = new WebSocketWrapper(options);
    var onClose     = jasmine.createSpy();
    myWebsocket.on("close", onClose);

    expect(onClose).not.toHaveBeenCalled();
    expect(onClose.calls.count()).toEqual(0);

    myWebsocket.ws.onopen();
    myWebsocket.ws.onclose({data: "Boom!", code: WebSocketWrapper.CLOSE_ABNORMAL});

    expect(onClose).not.toHaveBeenCalled();
    expect(onClose.calls.count()).toEqual(0);
  });

  it("should_not add websocket ping monitor when indefiniteRetry is false", function () {
    often       = jasmine.createSpy();
    var options = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: false
    };
    new WebSocketWrapper(options);

    expect(often).not.toHaveBeenCalled();
    expect(often.calls.count()).toEqual(0);
  });

  it("should add websocket ping monitor when indefiniteRetry is true", function () {
    var options = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };
    new WebSocketWrapper(options);

    expect(start).toHaveBeenCalled();
    expect(start.calls.count()).toEqual(1);
  });

  it("should add websocket ping monitor which triggers every 15 secs", function () {
    var options = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };
    new WebSocketWrapper(options);

    expect(wait).toHaveBeenCalled();
    expect(wait.calls.count()).toEqual(1);
    expect(wait.calls.first().args[0]).toEqual(15000);
  });

  it("should add websocket ping monitor which starts after 5 secs", function () {
    var options = {
      Websocket:       MockedWebsocket,
      indefiniteRetry: true
    };

    new WebSocketWrapper(options);

    expect(start).toHaveBeenCalled();
    expect(start.calls.count()).toEqual(1);
    expect(start.calls.first().args[0]).toEqual(5000);
  });
});
