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

define(['lodash', 'models/pipeline_configs/argument'], function (_, Argument) {
  describe('Argument create', function () {
    it('should create from a list of arguments', function () {
      var arguments = ['arg1', 'arg2'];

      var argument = Argument.create(undefined, arguments);

      expect(argument.data()).toBe(arguments);
      expect(argument.isList()).toBe(true);
    });

    it('should create from a string argument', function () {
      var arguments = "args";

      var argument = Argument.create(arguments, undefined);

      expect(argument.data()).toBe(arguments);
      expect(argument.isList()).toBe(false);
    });

    it('should create a list type in absence of any arguments', function () {
      var argument = Argument.create(undefined, undefined);

      expect(argument.data().length).toBe(0);
      expect(argument.isList()).toBe(true);
    });
  });

  describe('Argument toJSON', function() {
    it('should give json representation of list type', function() {
      var arguments = ['arg1', 'arg2'];

      var argument = Argument.create(undefined, arguments);

      expect(argument.toJSON()).toEqual({arguments: arguments});
    });

    it('should convert argument object of type string to json', function() {
      var arguments = "arg";

      var argument = Argument.create(arguments, undefined);

      expect(argument.toJSON()).toEqual({args: arguments});
    });

    it('should be empty in absence of data', function() {
      var argument = Argument.create(undefined, undefined);

      expect(argument.toJSON()).toEqual({});
    });
  });

  describe('Argument list VM', function () {
    describe('setter', function () {
      it('should split the string args by newline char and assign to arguments', function () {
        var argument = Argument.create(undefined, []);

        Argument.vm(argument).data("bundle\nexec");

        expect(argument.data()).toEqual(['bundle', 'exec']);
      });

      it('should assign an empty array if args is empty', function () {
        var argument = Argument.create(undefined, []);

        Argument.vm(argument).data("");

        expect(argument.data()).toEqual([]);
      });
    });

    describe('getter', function () {
      it('should return args as string joined by newline char', function () {
        var argument = Argument.create(undefined, ['bundle', 'exec']);

        expect(Argument.vm(argument).data()).toEqual("bundle\nexec");
      });
    });
  });

  describe('Argument string VM', function () {
    describe('setter', function () {
      it('should assign the given val to argument', function () {
        var argument = Argument.create("bundle exec", undefined);

        Argument.vm(argument).data("rake db:migrate");

        expect(argument.data()).toEqual("rake db:migrate");
      });
    });

    describe('getter', function () {
      it('should return args as string joined by newline char', function () {
        var argument = Argument.create("bundle exec", undefined);

        expect(Argument.vm(argument).data()).toEqual("bundle exec");
      });
    });
  });
});
