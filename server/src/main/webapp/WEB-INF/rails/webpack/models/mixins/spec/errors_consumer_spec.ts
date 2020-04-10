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

import Stream from "mithril/stream";
import {BaseErrorsConsumer, ErrorsConsumer} from "models/mixins/errors_consumer";
import {Configuration, Configurations} from "models/shared/configuration";
import {PlainTextValue} from "models/shared/config_value";

describe("ErrorsConsumer", () => {
  describe("consumeErrorsResponse()", () => {
    it("binds errors to raw primitive fields", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { raw: ["rawr"] }
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["raw"]);
      expect(model.errors().errorsForDisplay("raw")).toBe("rawr.");
    });

    it("binds all errors for a field", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { raw: ["rawr", "meow", "moo"] }
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["raw"]);
      expect(model.errors().errorsForDisplay("raw")).toBe("rawr. meow. moo.");
    });

    it("binds errors to primitive-value fields wrapped in streams", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { plain: ["fancy"] }
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["plain"]);
      expect(model.errors().errorsForDisplay("plain")).toBe("fancy.");
    });

    it("binds errors to a one-to-one submodel", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { one: ["two"] },
        one: { errors: { name: ["anonymous"] } }
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["one"]);
      expect(model.errors().errorsForDisplay("one")).toBe("two.");

      expect(model.one().errors().hasErrors()).toBe(true);
      expect(model.one().errors().count()).toBe(1);
      expect(model.one().errors().keys()).toEqual(["name"]);
      expect(model.one().errors().errorsForDisplay("name")).toBe("anonymous.");
    });

    it("binds errors to a multi-level nested submodel", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { one: ["two"] },
        one: { errors: { name: ["anonymous"] }, sub: { errors: { name: ["nesty-nest"] } } }
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["one"]);
      expect(model.errors().errorsForDisplay("one")).toBe("two.");

      expect(model.one().errors().hasErrors()).toBe(true);
      expect(model.one().errors().count()).toBe(1);
      expect(model.one().errors().keys()).toEqual(["name"]);
      expect(model.one().errors().errorsForDisplay("name")).toBe("anonymous.");

      expect(model.one().sub().errors().hasErrors()).toBe(true);
      expect(model.one().sub().errors().count()).toBe(1);
      expect(model.one().sub().errors().keys()).toEqual(["name"]);
      expect(model.one().sub().errors().errorsForDisplay("name")).toBe("nesty-nest.");
    });

    it("binds errors to one-to-many submodels", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { many: ["tons"] },
        many: [{ errors: { name: ["first"] } }, { errors: { name: ["second"] } }]
      });

      expect(unmatched.hasErrors()).toBe(false);

      expect(model.errors().count()).toBe(1);
      expect(model.errors().keys()).toEqual(["many"]);
      expect(model.errors().errorsForDisplay("many")).toBe("tons.");

      expect(model.many()[0].errors().hasErrors()).toBe(true);
      expect(model.many()[0].errors().count()).toBe(1);
      expect(model.many()[0].errors().keys()).toEqual(["name"]);
      expect(model.many()[0].errors().errorsForDisplay("name")).toBe("first.");

      expect(model.many()[1].errors().hasErrors()).toBe(true);
      expect(model.many()[1].errors().count()).toBe(1);
      expect(model.many()[1].errors().keys()).toEqual(["name"]);
      expect(model.many()[1].errors().errorsForDisplay("name")).toBe("second.");
    });

    it("binds errors at all levels in a complex tree and collects unmatched errors", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { plain: ["fancy"], many: ["tons"], raw: ["veggies"], one: ["two", "three"], oh: ["noes"] },
        one: { errors: { name: ["anonymous"] }, sub: { errors: { name: ["nesty-nest"], mystery: ["meat"] } } },
        many: [{ errors: { name: ["first"], unbeknownst: ["to you"] } }, { errors: { name: ["second"] } }],
        fuggles: { errors: { what: ["wtf"] }, muggles: { errors: { when: ["now", "then"] }, struggles: { errors: { how: ["who knows"] } } } },
        funky: [{}, { errors: { take_me: ["to funky town"] } }]
      });

      // unbound errors
      expect(unmatched.hasErrors()).toBe(true);
      expect(unmatched.count()).toBe(7);
      expect(unmatched.keys().sort()).toEqual([
        "dummy.oh",
        "dummy.one.sub.mystery",
        "dummy.many[0].unbeknownst",
        "dummy.fuggles.what",
        "dummy.fuggles.muggles.when",
        "dummy.fuggles.muggles.struggles.how",
        "dummy.funky[1].takeMe",
      ].sort());
      expect(unmatched.errorsForDisplay("dummy.oh")).toBe("noes.");
      expect(unmatched.errorsForDisplay("dummy.one.sub.mystery")).toBe("meat.");
      expect(unmatched.errorsForDisplay("dummy.many[0].unbeknownst")).toBe("to you.");
      expect(unmatched.errorsForDisplay("dummy.fuggles.what")).toBe("wtf.");
      expect(unmatched.errorsForDisplay("dummy.fuggles.muggles.when")).toBe("now. then.");
      expect(unmatched.errorsForDisplay("dummy.fuggles.muggles.struggles.how")).toBe("who knows.");
      expect(unmatched.errorsForDisplay("dummy.funky[1].takeMe")).toBe("to funky town.");

      // top-level
      expect(model.errors().count()).toBe(4);
      expect(model.errors().keys().sort()).toEqual(["plain", "raw", "one", "many"].sort());
      expect(model.errors().errorsForDisplay("plain")).toBe("fancy.");
      expect(model.errors().errorsForDisplay("raw")).toBe("veggies.");
      expect(model.errors().errorsForDisplay("one")).toBe("two. three.");
      expect(model.errors().errorsForDisplay("many")).toBe("tons.");

      // 1 -> 1
      expect(model.one().errors().count()).toBe(1);
      expect(model.one().errors().keys()).toEqual(["name"]);
      expect(model.one().errors().errorsForDisplay("name")).toBe("anonymous.");

      // 1 -> 1 -> 1
      expect(model.one().sub().errors().count()).toBe(1);
      expect(model.one().sub().errors().keys()).toEqual(["name"]);
      expect(model.one().sub().errors().errorsForDisplay("name")).toBe("nesty-nest.");

      // 1 -> *
      expect(model.many()[0].errors().count()).toBe(1);
      expect(model.many()[0].errors().keys()).toEqual(["name"]);
      expect(model.many()[0].errors().errorsForDisplay("name")).toBe("first.");
      expect(model.many()[1].errors().count()).toBe(1);
      expect(model.many()[1].errors().keys()).toEqual(["name"]);
      expect(model.many()[1].errors().errorsForDisplay("name")).toBe("second.");
    });

    it("consumeErrorsResponse() allows the root path to be overriden", () => {
      const model = new Dummy();

      const unmatched = model.consumeErrorsResponse({
        errors: { unknown: ["hello"] }
      }, "me");

      expect(unmatched.hasErrors()).toBe(true);
      expect(unmatched.keys()).toEqual(["me.unknown"]);
      expect(unmatched.errorsForDisplay("me.unknown")).toBe("hello.");
    });

    it("should bind errors onto to the configuration object", () => {
      const model = new DummyWithConfigurations();

      expect(model.config!.get(0).errors).toEqual([]);
      model.consumeErrorsResponse({config: [{errors: {pluginKey: ["Boom!"]}}]});
      expect(model.config!.get(0).errors).toEqual(["Boom!"]);
    });

    class DummyWithConfigurations extends BaseErrorsConsumer {
      config: Configurations;

      constructor() {
        super();
        const config = new Configuration("pluginKey", new PlainTextValue("pluginValue"));
        this.config  = new Configurations([config]);
      }
    }

    class Dummy extends BaseErrorsConsumer {
      one: Stream<Sub1> = Stream();
      many: Stream<Sub2[]> = Stream();

      plain: Stream<string> = Stream("plain");
      raw: string = "raw";

      constructor() {
        super();
        this.one(new Sub1());
        this.many([new Sub2(), new Sub2()]);
      }
    }

    class Sub1 extends BaseErrorsConsumer {
      name: Stream<string> = Stream("subby 1");
      sub: Stream<Sub2> = Stream();
      readonly kind: string = "sub-1";

      constructor() {
        super();
        this.sub(new Sub2());
      }
    }

    class Sub2 extends BaseErrorsConsumer {
      name: Stream<string> = Stream("subby 2");
      readonly kind: string = "sub-2";
    }
  });

  describe("errorContainerFor()", () => {
    it("allows consumeErrorsResponse() to work when the response hierarchy differs from the model hierarchy", () => {
      const model = new Foo();
      const unmatched = model.consumeErrorsResponse({
        bar: { errors: { name: ["boom"], beep: ["boop"] } },
      });

      expect(unmatched.hasErrors()).toBe(true);
      expect(unmatched.count()).toBe(1);
      expect(unmatched.keys()).toEqual(["foo.bar.beep"]);
      expect(unmatched.errorsForDisplay("foo.bar.beep")).toBe("boop.");

      expect(model.bar().attrs().errors().hasErrors()).toBe(true);
      expect(model.bar().attrs().errors().keys()).toEqual(["name"]);
      expect(model.bar().attrs().errors().errorsForDisplay("name")).toBe("boom.");
    });

    class Foo extends BaseErrorsConsumer {
      bar = Stream(new Bar());
    }

    class Bar extends BaseErrorsConsumer {
      attrs = Stream(new Baz());

      errorContainerFor(key: string): ErrorsConsumer {
        return this.attrs();
      }
    }

    class Baz extends BaseErrorsConsumer {
      name = "baz";
    }
  });
});
