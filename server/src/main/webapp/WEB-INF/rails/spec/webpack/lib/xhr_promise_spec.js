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
import XhrPromise from "rails-shared/xhr_promise";

describe("XhrPromise", () => {
  if ("function" !== typeof Promise) {
    return pending(); // eslint-disable-line no-undef
  }

  it("returns a promise that fires success and complete handlers", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("http://success.url/here", undefined, "GET").andReturn({
        response: "Great job!",
        status:   200
      });

      let success;
      let response;

      new XhrPromise({url: "http://success.url/here"}).
        then((res) => { success = true, response = res.data; }).
        catch(() => done.fail("request should have succeeded")).
        finally(() => {
          expect(success).toBe(true);
          expect(response).toBe("Great job!");
          done();
        });
    });
  });

  it("returns a promise that fires failure and complete handlers", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("http://i.fail/here", undefined, "GET").andReturn({
        response: "sucka.",
        status:   422
      });

      let success;
      let response;

      new XhrPromise({url: "http://i.fail/here"}).
        then(() => done.fail("request should have failed")).
        catch((res) => { success = false, response = res.error; }).
        finally(() => {
          expect(success).toBe(false);
          expect(response).toBe("sucka.");
          done();
        });
    });
  });

  it("fires beforeSend() handler", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("http://my.url", undefined, "GET").andReturn({
        response: "ok",
        status:   200
      });

      const steps = [];

      new XhrPromise({url: "http://my.url", beforeSend: () => steps.push("beforeSend()")}).
        then(() => steps.push("success()")).
        catch(() => done.fail("request should have succeeded")).
        finally(() => {
          expect(steps).toEqual(["beforeSend()", "success()"]);
          done();
        });
    });
  });
});
