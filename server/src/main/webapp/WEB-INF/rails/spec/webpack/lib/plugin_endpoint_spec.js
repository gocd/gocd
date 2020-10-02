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
import AnalyticsEndpoint from "rails-shared/plugin-endpoint";

describe("AnalyticsEndpoint", () => {

  beforeEach(() => AnalyticsEndpoint.reset());
  afterEach(() => AnalyticsEndpoint.reset());

  it("should send response for matching request", (done) => {
    jasmine.fakeMessagePosting((restore) => {
      AnalyticsEndpoint.ensure("v1");

      let messageContent;
      let response;

      AnalyticsEndpoint.define({
        "go.cd.analytics.v1.should.receive": (message, trans) => {
          messageContent = message.body;
          trans.respond({ data: "correct" });
        },
        "go.cd.analytics.v1.should.not.receive": (_message, trans) => {
          trans.respond({ data: "incorrect" });
        }
      });

      AnalyticsEndpoint.onInit((_data, trans) => {
        trans.request("should.receive", "foo").done((data) => {
          response = data;
        }).fail((_error) => {
          fail(); // eslint-disable-line no-undef
        }).always(() => {
          expect(response).toBe("correct");
          expect(messageContent).toBe("foo");
          restore();
          done();
        });
      });

      AnalyticsEndpoint.init(window, "initialized");
    });
  });
});
