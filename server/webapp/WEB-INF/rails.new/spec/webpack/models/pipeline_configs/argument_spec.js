/*
 * Copyright 2016 ThoughtWorks, Inc.
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

describe("Argument", () => {

  const Argument = require('models/pipeline_configs/argument');

  describe('Argument create', () => {
    it('should create from a list of arguments', () => {
      const args = ['arg1', 'arg2'];

      const argument = Argument.create(undefined, args);

      expect(argument.data()).toBe(args);
      expect(argument.isList()).toBe(true);
    });

    it('should create from a string argument', () => {
      const args = "args";

      const argument = Argument.create(args, undefined);

      expect(argument.data()).toBe(args);
      expect(argument.isList()).toBe(false);
    });

    it('should create a list type in absence of any arguments', () => {
      const argument = Argument.create(undefined, undefined);

      expect(argument.data().length).toBe(0);
      expect(argument.isList()).toBe(true);
    });
  });

  describe('Argument toJSON', () => {
    it('should give json representation of list type', () => {
      const args = ['arg1', 'arg2'];

      const argument = Argument.create(undefined, args);

      expect(argument.toJSON()).toEqual({arguments: args});
    });

    it('should convert argument object of type string to json', () => {
      const args = "arg";

      const argument = Argument.create(args, undefined);

      expect(argument.toJSON()).toEqual({args});
    });

    it('should be empty in absence of data', () => {
      const argument = Argument.create(undefined, undefined);

      expect(argument.toJSON()).toEqual({});
    });
  });

  describe('Argument list VM', () => {
    describe('setter', () => {
      it('should split the string args by newline char and assign to arguments', () => {
        const argument = Argument.create(undefined, []);

        Argument.vm(argument).data("bundle\nexec");

        expect(argument.data()).toEqual(['bundle', 'exec']);
      });

      it('should assign an empty array if args is empty', () => {
        const argument = Argument.create(undefined, []);

        Argument.vm(argument).data("");

        expect(argument.data()).toEqual([]);
      });
    });

    describe('getter', () => {
      it('should return args as string joined by newline char', () => {
        const argument = Argument.create(undefined, ['bundle', 'exec']);

        expect(Argument.vm(argument).data()).toEqual("bundle\nexec");
      });
    });
  });

  describe('Argument string VM', () => {
    describe('setter', () => {
      it('should assign the given val to argument', () => {
        const argument = Argument.create("bundle exec", undefined);

        Argument.vm(argument).data("rake db:migrate");

        expect(argument.data()).toEqual("rake db:migrate");
      });
    });

    describe('getter', () => {
      it('should return args as string joined by newline char', () => {
        const argument = Argument.create("bundle exec", undefined);

        expect(Argument.vm(argument).data()).toEqual("bundle exec");
      });
    });
  });
});
