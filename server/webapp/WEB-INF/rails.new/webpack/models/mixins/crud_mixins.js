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

var $        = require('jquery');
var _        = require('lodash');
var s        = require('string-plus');
var mrequest = require('helpers/mrequest');

var CrudMixins = {};

CrudMixins.Index = function (options) {
  var type     = options.type;
  var url      = options.indexUrl;
  var version  = options.version;
  var dataPath = options.dataPath;

  type.all = function (cb) {
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'GET',
        url:         url,
        timeout:     mrequest.timeout,
        beforeSend:  function (xhr) {
          mrequest.xhrConfig.forVersion(version)(xhr);
          if (cb) {
            cb(xhr);
          }
        },
        contentType: false
      });

      var didFulfill = function (data, _textStatus, _jqXHR) {
        deferred.resolve(type.fromJSON(_.get(data, dataPath)));
      };

      var didReject = function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Create = function (options) {
  var url     = options.indexUrl;
  var version = options.version;
  var type    = options.type;

  this.create = function () {
    var entity = this;
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'POST',
        url:         url,
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(version),
        data:        JSON.stringify(entity, s.snakeCaser),
        contentType: 'application/json',
      });

      var didFulfill = function (data, _textStatus, jqXHR) {
        deferred.resolve(mrequest.unwrapMessageOrEntity(type)(data, jqXHR));
      };

      var didReject = function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapMessageOrEntity(type)(jqXHR.responseJSON, jqXHR));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Delete = function (options) {
  var url     = options.resourceUrl;
  var version = options.version;

  this.delete = function () {
    var entity = this;
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'DELETE',
        url:         url(entity.id()),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(version),
        contentType: false
      });

      var didFulfill = function (data, _textStatus, _jqXHR) {
        deferred.resolve(data.message);
      };
      var didReject  = function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Update = function (options) {
  var url     = options.resourceUrl;
  var version = options.version;
  var type    = options.type;

  this.update = function () {
    var entity = this;

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'PUT',
        url:         url(entity.id()),
        timeout:     mrequest.timeout,
        beforeSend:  function (xhr) {
          mrequest.xhrConfig.forVersion(version)(xhr);
          xhr.setRequestHeader('If-Match', entity.etag());
        },
        data:        JSON.stringify(entity, s.snakeCaser),
        contentType: 'application/json',
      });

      var didFulfill = function (data, _textStatus, jqXHR) {
        deferred.resolve(mrequest.unwrapMessageOrEntity(type)(data, jqXHR));
      };

      var didReject = function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapMessageOrEntity(type, entity.etag())(jqXHR.responseJSON, jqXHR));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };
};

CrudMixins.Refresh = function (options) {
  var url     = options.resourceUrl;
  var version = options.version;
  var type    = options.type;

  this.refresh = function () {
    var entity = this;
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'GET',
        url:         url(entity.id()),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(version),
        contentType: false
      });

      var didFulfill = function (data, _textStatus, jqXHR) {
        var entity = type.fromJSON(data);
        entity.etag(jqXHR.getResponseHeader('ETag'));
        deferred.resolve(entity);
      };

      var didReject = function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();
  };
};

CrudMixins.AllOperations = function (operations, options) {
  if (_.includes(operations, 'create')) {
    CrudMixins.Create.call(this, options);
  }
  if (_.includes(operations, 'refresh')) {
    CrudMixins.Refresh.call(this, options);
  }
  if (_.includes(operations, 'update')) {
    CrudMixins.Update.call(this, options);
  }
  if (_.includes(operations, 'delete')) {
    CrudMixins.Delete.call(this, options);
  }
};

module.exports = CrudMixins;