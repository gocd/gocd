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
import {Errors} from "models/mixins/errors";
import {Configuration} from "models/shared/configuration";
import s from "underscore.string";

/**
 * This class can either be used as a base class or a mixin. Currently, this is the base class
 * for `ValidatableMixin`.
 *
 * The concepts encapsulated here are error storage and consuming errors from a server API
 * response. This is important for any model handling CRUD via the API as there may be errors
 * that only the server can return because they cannot be validated on the client side. This
 * allows one to bind server errors back to the respective JS model fields so that the user
 * can be made aware of errors that prevent the success of user-initiated operations.
 */
export class BaseErrorsConsumer implements ErrorsConsumer {
  // Exposes `consume()` as a static method
  static consume = consume;

  // hide this from JSON serialization
  private __errors = Stream(new Errors());

  errors(newVal?: Errors): Errors {
    if (arguments.length) {
      this.__errors(newVal as Errors);
    }
    return this.__errors();
  }

  /**
   * Returns the model or sub-model responsible for recording errors for the field denoted
   * by `subkey`. This is particularly important when the error -> model hierarchy in the
   * server API response differs from the natural hierarchy in the JS models.
   */
  errorContainerFor(subkey: string): ErrorsConsumer {
    return this;
  }

  /**
   * Convenience function to consumes a server API response for errors on this model.
   *
   * @returns the set of unbound errors (erros which could not be bound to any known fields).
   */
  consumeErrorsResponse(response: ResponseWithErrors, path: string = pathName(this)): Errors {
    const unmatched = new Errors();
    BaseErrorsConsumer.consume(response, this, path, unmatched);
    return unmatched;
  }
}

/** API contract for the `ErrorsConsumer` behavior. */
export interface ErrorsConsumer {
  errors: (container?: Errors) => Errors;
  errorContainerFor: (subkey: string) => ErrorsConsumer;
  consumeErrorsResponse: (response: ResponseWithErrors, path?: string) => Errors;
}

/** Type-guard test to determine if a given object satisfies the `ErrorsConsumer` interface */
export function isErrorsConsumer(thing: ErrorsConsumer | any): thing is ErrorsConsumer {
  if (thing && "function" === typeof (thing as ErrorsConsumer).errorContainerFor) {
    return true;
  }
  return false;
}

/** Represents an `errors` object within a server API response */
interface ErrorMap {
  [key: string]: string[];
}

/** Represents a server API response with potential `errors` objects */
export type ResponseWithErrors = any & { errors?: ErrorMap; };

/**
 * Exposed as a static method on the `ErrorsConsumer` class/mixin.
 *
 * Recursively traverses (depth-first) a server API response to collect and bind errors
 * back to the given `model`'s (and any submodels') fields. Any errors that were not able
 * be bound to a known field are recorded in the `unmatchedErrors` instance, so that one
 * can display them (if so desired).
 *
 * @param response:  The server API response.
 * @param model:     The `model` object on which to bind errors to fields. Often this is an
 *                   `ErrorsConsumer` object; it is expected that the top-level invocation
 *                   is an `ErrorsConsumer`. Subsequent recursive invocations may or may
 *                   not be.
 * @param path:      A `string` representing the path to the current hierarchy. The top-level
 *                   invocation should be a meaningful name for the top-level model instance.
 * @param unmatched: An `Errors` object that will collect any errors that could not be bound
 *                   to a model field.
 */
function consume(response: any, model: any, path: string, unmatched: Errors): void {
  if (!response || "object" !== typeof response) { return; }

  if (response instanceof Array) { // assumes order from server is same as client-side model
    for (let i = response.length - 1; i >= 0; i--) {
      consume(response[i], item(model, i), sibl(path, i), unmatched);
    }
  } else {
    for (const key of Object.keys(response)) {
      if ("errors" === key) {
        addErrorsToModel(response.errors, model, path, unmatched);
      } else {
        consume(response[key], child(model, key), next(path, key), unmatched);
      }
    }
  }
}

/**
 * Binds errors to fields at the given hierarchy; if field does not exist in the model,
 * the errors is recorded in the `unmatched` Errors object. `path` allows us to report
 * the current hierarchy of the unmatched error.
 *
 * @param errors:    The `errors` hash detailing errors per field.
 * @param model:     The (sub-)`model` object on which to bind errors to fields. When this is
 *                   an `ErrorsConsumer` object, errors are recorded/bound on the model's
 *                   fields. When not an `ErrorConsumer`, any errors will be flagged as
 *                   unmatched.
 * @param path:      A `string` representing the path to the current hierarchy. This is used
 *                   strictly for marking unmatched/unbound errors.
 * @param unmatched: An `Errors` object that will collect any errors that could not be bound
 *                   to a model field.
 */
function addErrorsToModel(errors: ErrorMap, model: any, path: string, unmatched: Errors): void {
  for (const key of Object.keys(errors)) {
    const fieldName = field(key);
    const container = resolveErrorsHolder(model, fieldName); // may differ for each error key
    const matchesField = isAwareOfField(container, fieldName);
    for (const msg of errors[key]) {
      if (matchesField && !container.errors().hasError(fieldName, msg)) {
        container.errors().add(fieldName, msg);
      } else {
        addToUnmatchIfNotPluginProperty(container, unmatched, path, key, msg);
      }
    }
  }
}

//todo: Remove this special handling for Configuration class and make Configuration class an ErrorConsumer by containing Errors field.
function addToUnmatchIfNotPluginProperty(container: any, unmatched: Errors, path: string, key: string, msg: string) {
  if(container instanceof Configuration) {
    container.errors!.push(msg);
  } else {
    unmatched.add(next(path, key), msg);
  }
}

/**
 * @returns the error container responsible for a given `fieldName` with respect to the current
 * `model`. This might be the `model` itself, or a descendant model.
 */
function resolveErrorsHolder(model: any, fieldName: string): ErrorsConsumer | any {
  return isErrorsConsumer(model) ? model.errorContainerFor(fieldName) : model;
}

/** Tests if a given model is aware of an able to handle errors for a given field */
function isAwareOfField(model: any, fieldName: string): boolean {
  return isErrorsConsumer(model) && fieldName in model;
}

/** @returns a member of an Iterable by index as if it was an Array. Often for one->many relationships. */
function item(model: any, i: number): any {
  if (!model) { return undefined; }
  if (model instanceof Array) { return model[i]; }
  if (model[Symbol.iterator]) { return Array.from(model)[i]; }
  if (model.get) { return model.get(i); }
  return model;
}

/** @returns the sub-model of a `model` by `subkey`; understands how to deal with `Stream` objects. */
function child(model: any, subkey: string): any {
  const fieldName = field(subkey);
  if (model && "object" === typeof model) {
    if ("function" === typeof model[fieldName]) {
      return model[fieldName]();
    }
    return model[fieldName];
  }

  return model;
}

/**
 * Converts a subkey in the error response to a field name in the model; works by convention,
 * and essentially camelizes a string. If this convention is broken by a model, this will not work.
 *
 * @returns the resultant field name associated with the `subkey`.
 */
function field(subkey: string): string {
  return s.camelize(subkey, true);
}

/** @return a sensible path name for a given model; typically used to generate a default root path for consume(). */
function pathName(model: ErrorsConsumer): string {
  return s.camelize(model.constructor.name, true);
}

/** @returns the full path for a subkey by joining with the current path. */
function next(path: string, subkey: string): string {
  return path + `.${field(subkey)}`;
}

/** @returns the full path for a sibling element in an array for a given index. */
function sibl(path: string, index: number): string {
  return path + `[${index}]`;
}
