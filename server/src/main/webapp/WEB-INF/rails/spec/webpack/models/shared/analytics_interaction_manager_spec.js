/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import _ from "lodash";

import Interactions from "models/shared/analytics_interaction_manager";
import AnalyticsEndpoint from "rails-shared/plugin-endpoint";

function noop() {}

function fakeReq(key, body) {
  return {head: {type: "request", reqId: 0, key}, body};
}

const BASE64_RE         = /^[A-Za-z0-9/+]+(?:[=]{1,2})?$/;

describe("AnalyticsInteractionManager", () => {
  beforeEach(() => {
    AnalyticsEndpoint.reset();
    Interactions.purge();
  });

  afterEach(() => {
    Interactions.purge();
    AnalyticsEndpoint.reset();
  });

  it("ensure() should set up request handlers", () => {
    jasmine.fakeMessagePosting(() => {
      spyOn(window, "open").and.returnValue({focus: noop});

      Interactions.ensure();
      AnalyticsEndpoint.ensure("v1");

      window.postMessage(fakeReq("go.cd.analytics.v1.link-external", {url: "https://google.com"}), "*");
      expect(window.open).toHaveBeenCalledWith("https://google.com", "_blank");
    });
  });

  describe("Namespaces", () => {
    it("group() reports the namespace", () => {
      const Models = Interactions.ns("Namespacely");
      expect(Models.group()).toBe("Namespacely");
    });

    it("uid() generates a predictable, unique value based on content", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      expect(typeof uid).toBe("string", "uid() should always output a string");
      expect(uid.startsWith("Namespacely:")).toBe(true, "uids should be namespaced");
      expect(uid.replace(/^Namespacely:/, "").match(BASE64_RE)).not.toBe(null, "the uid body is a base64 output");
    });

    it("unpack() reverses uid() to its source data", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      expect(Models.unpack(uid)).toEqual({ordinal, plugin, type, id}, "unpack() returns an object with original source values");
    });

    it("toUrl() constructs an analytics URL based on uid", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      expect(Models.toUrl(uid)).toBe("/go/analytics/ohai/lolcat/canhas");
      expect(Models.toUrl(uid, {chzbrgr: "absolutely"})).toBe("/go/analytics/ohai/lolcat/canhas?chzbrgr=absolutely", "toUrl() converts extra params to query paramters");
    });

    it("toUrl() constructs an analytics URL based on uid", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      expect(Models.toUrl(uid)).toBe("/go/analytics/ohai/lolcat/canhas");
      expect(Models.toUrl(uid, {chzbrgr: "absolutely"})).toBe("/go/analytics/ohai/lolcat/canhas?chzbrgr=absolutely", "toUrl() converts extra params to query paramters");
    });

    it("modelFor() idempotently ensures a model exists for a given uid", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      const model = Models.modelFor(uid);
      expect(model.url()).toBe(Models.toUrl(uid), "modelFor() returns a model preconfigured to a specific analytics plugin frame");
      expect(Models.modelFor(uid)).toEqual(model, "modelFor() should be return the existing model for a given uid");
    });

    it("modelFor() takes an optional params arg to append to the url", () => {
      const Models = Interactions.ns("Namespacely");
      const ordinal = 5, plugin = "ohai", type = "lolcat", id = "canhas";
      const uid = Models.uid(ordinal, plugin, type, id);

      const model = Models.modelFor(uid, {hi: "there"});
      expect(model.url()).toBe(Models.toUrl(uid, {hi: "there"}), "modelFor() should append query params");
      expect(Models.modelFor(uid, {shouldNot: "change"}).url()).toBe(Models.toUrl(uid, {hi: "there"}), "subsequent calls to modelFor() do not alter query params of url once model has been constructed");
    });

    it("all() returns models pertaining only to a given namespace", () => {
      const M1 = Interactions.ns("Alpha");
      const M2 = Interactions.ns("Beta");
      const plugin = "ohai", type = "lolcat", id = "canhas";
      const table = [
        [M1, 0, plugin, type, id],
        [M1, 1, plugin, type, id],
        [M2, 0, plugin, type, id],
        [M2, 1, plugin, type, id]
      ];

      const world = {};

      for (let i = 0, len = table.length; i < len; i++) {
        const ns = table[i][0], args = table[i].slice(1);
        const uid = ns.uid.apply(null, args);
        world[uid] = ns.modelFor(uid);
      }

      const expectedAlpha = _.reduce(world, (m, v, k) => { if (k.startsWith("Alpha")) {m[k] = v;} return m; }, {});
      const expectedBeta = _.reduce(world, (m, v, k) => { if (k.startsWith("Beta")) {m[k] = v;} return m; }, {});

      expect(Object.keys(M1.all()).length).toBe(2, "all() returned wrong number of models returned from the Alpha namespace");
      expect(M1.all()).toEqual(expectedAlpha, "all() should return the entire subset of models under the Alpha namespace");

      expect(Object.keys(M2.all()).length).toBe(2, "all() returned wrong number of models returned from the Beta namespace");
      expect(M2.all()).toEqual(expectedBeta, "all() should return the entire subset of models under the Beta namespace");

      expect(Interactions.all()).toEqual(world, "AnalyticsInteractionManager.all() returns the global set");
    });
  });
});
