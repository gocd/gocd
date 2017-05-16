/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const $        = require('jquery');
const _        = require('lodash');
const s        = require('string-plus');
const mrequest = require('helpers/mrequest');

const CrudMixins = {};

CrudMixins.Index = (options) => {
  const type     = options.type;
  const url      = options.indexUrl;
  const version  = options.version;
  const dataPath = options.dataPath;

  type.all = (cb, queryParams = {}) => $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url,
      data:        queryParams,
      timeout:     mrequest.timeout,
      beforeSend(xhr) {
        mrequest.xhrConfig.forVersion(version)(xhr);
        if (cb) {
          cb(xhr);
        }
      },
      contentType: false
    });

    const didFulfill = (data, _textStatus, _jqXHR) => {
      deferred.resolve(type.fromJSON(_.get(data, dataPath)));
    };

    const didReject = (jqXHR, _textStatus, _errorThrown) => {
      deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
    };

    jqXHR.then(didFulfill, didReject);
  }).promise();
};

CrudMixins.Create = function (options) {
  const url     = options.indexUrl;
  const version = options.version;
  const type    = options.type;

  this.create = function () {
    const entity = this;
    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'POST',
        url,
        timeout:     mrequest.timeout,
        beforeSend (xhr) {
          mrequest.xhrConfig.forVersion(version)(xhr);
          return validateEntity(entity, deferred);
        },
        data:        JSON.stringify(entity, s.snakeCaser),
        contentType: 'application/json',
      });

      const didFulfill = (data, _textStatus, jqXHR) => {
        deferred.resolve(mrequest.unwrapMessageOrEntity(type)(data, jqXHR));
      };

      const didReject = (jqXHR, _textStatus, _errorThrown) => {
        deferred.reject(mrequest.unwrapMessageOrEntity(type)(jqXHR.responseJSON, jqXHR));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Delete = function (options) {
  const url     = options.resourceUrl;
  const version = options.version;

  this.delete = function () {
    const entity = this;
    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'DELETE',
        url:         "function" === typeof url ? url(entity) : url,
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(version),
        contentType: false
      });

      const didFulfill = ({message}, _textStatus, _jqXHR) => {
        deferred.resolve(message);
      };
      const didReject  = ({responseJSON}, _textStatus, _errorThrown) => {
        deferred.reject(mrequest.unwrapErrorExtractMessage(responseJSON));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Update = function (options) {
  const url     = options.resourceUrl;
  const version = options.version;
  const type    = options.type;
  const method  = options.method; // some API requests use "PATCH"

  this.update = function () {
    const entity = this;

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      method || "PUT",
        url:         "function" === typeof url ? url(entity) : url,
        timeout:     mrequest.timeout,
        beforeSend (xhr) {
          mrequest.xhrConfig.forVersion(version)(xhr);
          xhr.setRequestHeader('If-Match', entity.etag());
          return validateEntity(entity, deferred);
        },
        data:        JSON.stringify(entity, s.snakeCaser),
        contentType: 'application/json',
      });

      const didFulfill = (data, _textStatus, jqXHR) => {
        deferred.resolve(mrequest.unwrapMessageOrEntity(type)(data, jqXHR));
      };

      const didReject = (jqXHR, _textStatus, _errorThrown) => {
        deferred.reject(mrequest.unwrapMessageOrEntity(type, entity.etag())(jqXHR.responseJSON, jqXHR));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Refresh = function (options) {
  const url     = options.resourceUrl;
  const version = options.version;
  const type    = options.type;

  this.refresh = function () {
    const entity = this;
    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      "GET",
        url:         "function" === typeof url ? url(entity) : url,
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(version),
        contentType: false
      });

      const didFulfill = (data, _textStatus, jqXHR) => {
        const entity = type.fromJSON(data);
        entity.etag(jqXHR.getResponseHeader('ETag'));
        deferred.resolve(entity);
      };

      const didReject = ({responseJSON}, _textStatus, _errorThrown) => {
        deferred.reject(mrequest.unwrapErrorExtractMessage(responseJSON));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();
  };
};

CrudMixins.AllOperations = function (operations, options, overrides = {}) {
  _.each(operations, (op) => {
    const mixin = CrudMixins[_.capitalize(op)];
    if ("function" === typeof mixin) {
      mixin.call(this, _.assign({}, options, overrides[op] || {}));
    }
  });
};

const validateEntity = function (entity, deferred) {
  const isValid = entity.validate()._isEmpty();
  if (!isValid) {
    deferred.reject(entity);
  }
  return entity.validate()._isEmpty();
};

module.exports = CrudMixins;
