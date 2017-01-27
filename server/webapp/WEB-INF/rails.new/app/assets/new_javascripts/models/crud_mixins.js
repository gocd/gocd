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

define([
  'mithril', 'lodash', 'string-plus', 'helpers/mrequest'
], function (m, _, s, mrequest) {

  var CrudMixins = {};

  var unwrapMessageOrEntity = function (type, originalEtag) {
    return function (data, xhr) {
      if (xhr.status === 200) {
        var entity = type.fromJSON(data);
        entity.etag(xhr.getResponseHeader('ETag'));
        return entity;
      }
      if (xhr.status === 422) {
        var fromJSON = new type.fromJSON(data.data);
        fromJSON.etag(originalEtag);
        return fromJSON;
      } else {
        return mrequest.unwrapErrorExtractMessage(data, xhr);
      }
    };
  };

  CrudMixins.Index = function (options) {
    var type     = options.type;
    var url      = options.indexUrl;
    var version  = options.version;
    var dataPath = options.dataPath;

    type.all = function () {
      return m.request({
        method:        "GET",
        url:           url,
        config:        mrequest.xhrConfig.forVersion(version),
        unwrapSuccess: function (data) {
          return type.fromJSON(_.get(data, dataPath));
        },
        unwrapError:   mrequest.unwrapErrorExtractMessage
      });
    };
  };

  CrudMixins.Create = function (options) {
    var url     = options.indexUrl;
    var version = options.version;
    var type    = options.type;

    this['create'] = function () {
      return m.request({
        method:        'POST',
        url:           url,
        config:        mrequest.xhrConfig.forVersion(version),
        data:          JSON.parse(JSON.stringify(this, s.snakeCaser)),
        unwrapSuccess: unwrapMessageOrEntity(type),
        unwrapError:   unwrapMessageOrEntity(type)
      });
    };
  };

  CrudMixins.Delete = function (options) {
    var url     = options.resourceUrl;
    var version = options.version;

    this['delete'] = function () {
      return m.request({
        method:        "DELETE",
        url:           url(this.id()),
        config:        mrequest.xhrConfig.forVersion(version),
        unwrapSuccess: function (data, xhr) {
          if (xhr.status === 200) {
            return data.message;
          }
        },
        unwrapError:   mrequest.unwrapErrorExtractMessage
      });
    };
  };

  CrudMixins.Update = function (options) {
    var url     = options.resourceUrl;
    var version = options.version;
    var type    = options.type;

    this['update'] = function () {
      var entity = this;
      return m.request({
        method:        'PUT',
        url:           url(this.id()),
        config:        function (xhr) {
          mrequest.xhrConfig.forVersion(version)(xhr);
          xhr.setRequestHeader('If-Match', entity.etag());
        },
        data:          JSON.parse(JSON.stringify(entity, s.snakeCaser)),
        unwrapSuccess: unwrapMessageOrEntity(type),
        unwrapError:   unwrapMessageOrEntity(type, entity.etag())
      });
    };
  };

  CrudMixins.Refresh = function (options) {
    var url     = options.resourceUrl;
    var version = options.version;
    var type    = options.type;

    this.refresh = function () {
      return m.request({
        method:        'GET',
        url:           url(this.id()),
        config:        mrequest.xhrConfig.forVersion(version),
        unwrapSuccess: function (data, xhr) {
          var entity = type.fromJSON(data);
          entity.etag(xhr.getResponseHeader('ETag'));
          return entity;
        },
        unwrapError:   mrequest.unwrapErrorExtractMessage
        /* eslint-enable camelcase */
      });
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

  return CrudMixins;
});