window = this;/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Define a module along with a payload.
 * @param {string} moduleName Name for the payload
 * @param {ignored} deps Ignored. For compatibility with CommonJS AMD Spec
 * @param {function} payload Function with (require, exports, module) params
 */
function define(moduleName, deps, payload) {
  if (typeof moduleName != "string") {
    throw new TypeError('Expected string, got: ' + moduleName);
  }

  if (arguments.length == 2) {
    payload = deps;
  }

  if (moduleName in define.modules) {
    throw new Error("Module already defined: " + moduleName);
  }
  define.modules[moduleName] = payload;
};

/**
 * The global store of un-instantiated modules
 */
define.modules = {};


/**
 * We invoke require() in the context of a Domain so we can have multiple
 * sets of modules running separate from each other.
 * This contrasts with JSMs which are singletons, Domains allows us to
 * optionally load a CommonJS module twice with separate data each time.
 * Perhaps you want 2 command lines with a different set of commands in each,
 * for example.
 */
function Domain() {
  this.modules = {};
  this._currentModule = null;
}

(function () {

  /**
   * Lookup module names and resolve them by calling the definition function if
   * needed.
   * There are 2 ways to call this, either with an array of dependencies and a
   * callback to call when the dependencies are found (which can happen
   * asynchronously in an in-page context) or with a single string an no callback
   * where the dependency is resolved synchronously and returned.
   * The API is designed to be compatible with the CommonJS AMD spec and
   * RequireJS.
   * @param {string[]|string} deps A name, or names for the payload
   * @param {function|undefined} callback Function to call when the dependencies
   * are resolved
   * @return {undefined|object} The module required or undefined for
   * array/callback method
   */
  Domain.prototype.require = function(deps, callback) {
    if (Array.isArray(deps)) {
      var params = deps.map(function(dep) {
        return this.lookup(dep);
      }, this);
      if (callback) {
        callback.apply(null, params);
      }
      return undefined;
    }
    else {
      return this.lookup(deps);
    }
  };

  function normalize(path) {
    var bits = path.split('/');
    var i = 1;
    while (i < bits.length) {
      if (bits[i] === '..') {
        bits.splice(i-1, 1);
      } else if (bits[i] === '.') {
        bits.splice(i, 1);
      } else {
        i++;
      }
    }
    return bits.join('/');
  }

  function join(a, b) {
    a = a.trim();
    b = b.trim();
    if (/^\//.test(b)) {
      return b;
    } else {
      return a.replace(/\/*$/, '/') + b;
    }
  }

  function dirname(path) {
    var bits = path.split('/');
    bits.pop();
    return bits.join('/');
  }

  /**
   * Lookup module names and resolve them by calling the definition function if
   * needed.
   * @param {string} moduleName A name for the payload to lookup
   * @return {object} The module specified by aModuleName or null if not found.
   */
  Domain.prototype.lookup = function(moduleName) {
    if (/^\./.test(moduleName)) {
      moduleName = normalize(join(dirname(this._currentModule), moduleName));
    }

    if (moduleName in this.modules) {
      var module = this.modules[moduleName];
      return module;
    }

    if (!(moduleName in define.modules)) {
      throw new Error("Module not defined: " + moduleName);
    }

    var module = define.modules[moduleName];

    if (typeof module == "function") {
      var exports = {};
      var previousModule = this._currentModule;
      this._currentModule = moduleName;
      module(this.require.bind(this), exports, { id: moduleName, uri: "" });
      this._currentModule = previousModule;
      module = exports;
    }

    // cache the resulting module object for next time
    this.modules[moduleName] = module;

    return module;
  };

}());

define.Domain = Domain;
define.globalDomain = new Domain();
var require = define.globalDomain.require.bind(define.globalDomain);
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/source-map-generator', ['require', 'exports', 'module' ,  'source-map/base64-vlq', 'source-map/util', 'source-map/array-set'], function(require, exports, module) {

  var base64VLQ = require('./base64-vlq');
  var util = require('./util');
  var ArraySet = require('./array-set').ArraySet;

  /**
   * An instance of the SourceMapGenerator represents a source map which is
   * being built incrementally. You may pass an object with the following
   * properties:
   *
   *   - file: The filename of the generated source.
   *   - sourceRoot: A root for all relative URLs in this source map.
   */
  function SourceMapGenerator(aArgs) {
    if (!aArgs) {
      aArgs = {};
    }
    this._file = util.getArg(aArgs, 'file', null);
    this._sourceRoot = util.getArg(aArgs, 'sourceRoot', null);
    this._sources = new ArraySet();
    this._names = new ArraySet();
    this._mappings = [];
    this._sourcesContents = null;
  }

  SourceMapGenerator.prototype._version = 3;

  /**
   * Creates a new SourceMapGenerator based on a SourceMapConsumer
   *
   * @param aSourceMapConsumer The SourceMap.
   */
  SourceMapGenerator.fromSourceMap =
    function SourceMapGenerator_fromSourceMap(aSourceMapConsumer) {
      var sourceRoot = aSourceMapConsumer.sourceRoot;
      var generator = new SourceMapGenerator({
        file: aSourceMapConsumer.file,
        sourceRoot: sourceRoot
      });
      aSourceMapConsumer.eachMapping(function (mapping) {
        var newMapping = {
          generated: {
            line: mapping.generatedLine,
            column: mapping.generatedColumn
          }
        };

        if (mapping.source != null) {
          newMapping.source = mapping.source;
          if (sourceRoot != null) {
            newMapping.source = util.relative(sourceRoot, newMapping.source);
          }

          newMapping.original = {
            line: mapping.originalLine,
            column: mapping.originalColumn
          };

          if (mapping.name != null) {
            newMapping.name = mapping.name;
          }
        }

        generator.addMapping(newMapping);
      });
      aSourceMapConsumer.sources.forEach(function (sourceFile) {
        var content = aSourceMapConsumer.sourceContentFor(sourceFile);
        if (content != null) {
          generator.setSourceContent(sourceFile, content);
        }
      });
      return generator;
    };

  /**
   * Add a single mapping from original source line and column to the generated
   * source's line and column for this source map being created. The mapping
   * object should have the following properties:
   *
   *   - generated: An object with the generated line and column positions.
   *   - original: An object with the original line and column positions.
   *   - source: The original source file (relative to the sourceRoot).
   *   - name: An optional original token name for this mapping.
   */
  SourceMapGenerator.prototype.addMapping =
    function SourceMapGenerator_addMapping(aArgs) {
      var generated = util.getArg(aArgs, 'generated');
      var original = util.getArg(aArgs, 'original', null);
      var source = util.getArg(aArgs, 'source', null);
      var name = util.getArg(aArgs, 'name', null);

      this._validateMapping(generated, original, source, name);

      if (source != null && !this._sources.has(source)) {
        this._sources.add(source);
      }

      if (name != null && !this._names.has(name)) {
        this._names.add(name);
      }

      this._mappings.push({
        generatedLine: generated.line,
        generatedColumn: generated.column,
        originalLine: original != null && original.line,
        originalColumn: original != null && original.column,
        source: source,
        name: name
      });
    };

  /**
   * Set the source content for a source file.
   */
  SourceMapGenerator.prototype.setSourceContent =
    function SourceMapGenerator_setSourceContent(aSourceFile, aSourceContent) {
      var source = aSourceFile;
      if (this._sourceRoot != null) {
        source = util.relative(this._sourceRoot, source);
      }

      if (aSourceContent != null) {
        // Add the source content to the _sourcesContents map.
        // Create a new _sourcesContents map if the property is null.
        if (!this._sourcesContents) {
          this._sourcesContents = {};
        }
        this._sourcesContents[util.toSetString(source)] = aSourceContent;
      } else {
        // Remove the source file from the _sourcesContents map.
        // If the _sourcesContents map is empty, set the property to null.
        delete this._sourcesContents[util.toSetString(source)];
        if (Object.keys(this._sourcesContents).length === 0) {
          this._sourcesContents = null;
        }
      }
    };

  /**
   * Applies the mappings of a sub-source-map for a specific source file to the
   * source map being generated. Each mapping to the supplied source file is
   * rewritten using the supplied source map. Note: The resolution for the
   * resulting mappings is the minimium of this map and the supplied map.
   *
   * @param aSourceMapConsumer The source map to be applied.
   * @param aSourceFile Optional. The filename of the source file.
   *        If omitted, SourceMapConsumer's file property will be used.
   * @param aSourceMapPath Optional. The dirname of the path to the source map
   *        to be applied. If relative, it is relative to the SourceMapConsumer.
   *        This parameter is needed when the two source maps aren't in the same
   *        directory, and the source map to be applied contains relative source
   *        paths. If so, those relative source paths need to be rewritten
   *        relative to the SourceMapGenerator.
   */
  SourceMapGenerator.prototype.applySourceMap =
    function SourceMapGenerator_applySourceMap(aSourceMapConsumer, aSourceFile, aSourceMapPath) {
      var sourceFile = aSourceFile;
      // If aSourceFile is omitted, we will use the file property of the SourceMap
      if (aSourceFile == null) {
        if (aSourceMapConsumer.file == null) {
          throw new Error(
            'SourceMapGenerator.prototype.applySourceMap requires either an explicit source file, ' +
            'or the source map\'s "file" property. Both were omitted.'
          );
        }
        sourceFile = aSourceMapConsumer.file;
      }
      var sourceRoot = this._sourceRoot;
      // Make "sourceFile" relative if an absolute Url is passed.
      if (sourceRoot != null) {
        sourceFile = util.relative(sourceRoot, sourceFile);
      }
      // Applying the SourceMap can add and remove items from the sources and
      // the names array.
      var newSources = new ArraySet();
      var newNames = new ArraySet();

      // Find mappings for the "sourceFile"
      this._mappings.forEach(function (mapping) {
        if (mapping.source === sourceFile && mapping.originalLine != null) {
          // Check if it can be mapped by the source map, then update the mapping.
          var original = aSourceMapConsumer.originalPositionFor({
            line: mapping.originalLine,
            column: mapping.originalColumn
          });
          if (original.source != null) {
            // Copy mapping
            mapping.source = original.source;
            if (aSourceMapPath != null) {
              mapping.source = util.join(aSourceMapPath, mapping.source)
            }
            if (sourceRoot != null) {
              mapping.source = util.relative(sourceRoot, mapping.source);
            }
            mapping.originalLine = original.line;
            mapping.originalColumn = original.column;
            if (original.name != null && mapping.name != null) {
              // Only use the identifier name if it's an identifier
              // in both SourceMaps
              mapping.name = original.name;
            }
          }
        }

        var source = mapping.source;
        if (source != null && !newSources.has(source)) {
          newSources.add(source);
        }

        var name = mapping.name;
        if (name != null && !newNames.has(name)) {
          newNames.add(name);
        }

      }, this);
      this._sources = newSources;
      this._names = newNames;

      // Copy sourcesContents of applied map.
      aSourceMapConsumer.sources.forEach(function (sourceFile) {
        var content = aSourceMapConsumer.sourceContentFor(sourceFile);
        if (content != null) {
          if (aSourceMapPath != null) {
            sourceFile = util.join(aSourceMapPath, sourceFile);
          }
          if (sourceRoot != null) {
            sourceFile = util.relative(sourceRoot, sourceFile);
          }
          this.setSourceContent(sourceFile, content);
        }
      }, this);
    };

  /**
   * A mapping can have one of the three levels of data:
   *
   *   1. Just the generated position.
   *   2. The Generated position, original position, and original source.
   *   3. Generated and original position, original source, as well as a name
   *      token.
   *
   * To maintain consistency, we validate that any new mapping being added falls
   * in to one of these categories.
   */
  SourceMapGenerator.prototype._validateMapping =
    function SourceMapGenerator_validateMapping(aGenerated, aOriginal, aSource,
                                                aName) {
      if (aGenerated && 'line' in aGenerated && 'column' in aGenerated
          && aGenerated.line > 0 && aGenerated.column >= 0
          && !aOriginal && !aSource && !aName) {
        // Case 1.
        return;
      }
      else if (aGenerated && 'line' in aGenerated && 'column' in aGenerated
               && aOriginal && 'line' in aOriginal && 'column' in aOriginal
               && aGenerated.line > 0 && aGenerated.column >= 0
               && aOriginal.line > 0 && aOriginal.column >= 0
               && aSource) {
        // Cases 2 and 3.
        return;
      }
      else {
        throw new Error('Invalid mapping: ' + JSON.stringify({
          generated: aGenerated,
          source: aSource,
          original: aOriginal,
          name: aName
        }));
      }
    };

  /**
   * Serialize the accumulated mappings in to the stream of base 64 VLQs
   * specified by the source map format.
   */
  SourceMapGenerator.prototype._serializeMappings =
    function SourceMapGenerator_serializeMappings() {
      var previousGeneratedColumn = 0;
      var previousGeneratedLine = 1;
      var previousOriginalColumn = 0;
      var previousOriginalLine = 0;
      var previousName = 0;
      var previousSource = 0;
      var result = '';
      var mapping;

      // The mappings must be guaranteed to be in sorted order before we start
      // serializing them or else the generated line numbers (which are defined
      // via the ';' separators) will be all messed up. Note: it might be more
      // performant to maintain the sorting as we insert them, rather than as we
      // serialize them, but the big O is the same either way.
      this._mappings.sort(util.compareByGeneratedPositions);

      for (var i = 0, len = this._mappings.length; i < len; i++) {
        mapping = this._mappings[i];

        if (mapping.generatedLine !== previousGeneratedLine) {
          previousGeneratedColumn = 0;
          while (mapping.generatedLine !== previousGeneratedLine) {
            result += ';';
            previousGeneratedLine++;
          }
        }
        else {
          if (i > 0) {
            if (!util.compareByGeneratedPositions(mapping, this._mappings[i - 1])) {
              continue;
            }
            result += ',';
          }
        }

        result += base64VLQ.encode(mapping.generatedColumn
                                   - previousGeneratedColumn);
        previousGeneratedColumn = mapping.generatedColumn;

        if (mapping.source != null) {
          result += base64VLQ.encode(this._sources.indexOf(mapping.source)
                                     - previousSource);
          previousSource = this._sources.indexOf(mapping.source);

          // lines are stored 0-based in SourceMap spec version 3
          result += base64VLQ.encode(mapping.originalLine - 1
                                     - previousOriginalLine);
          previousOriginalLine = mapping.originalLine - 1;

          result += base64VLQ.encode(mapping.originalColumn
                                     - previousOriginalColumn);
          previousOriginalColumn = mapping.originalColumn;

          if (mapping.name != null) {
            result += base64VLQ.encode(this._names.indexOf(mapping.name)
                                       - previousName);
            previousName = this._names.indexOf(mapping.name);
          }
        }
      }

      return result;
    };

  SourceMapGenerator.prototype._generateSourcesContent =
    function SourceMapGenerator_generateSourcesContent(aSources, aSourceRoot) {
      return aSources.map(function (source) {
        if (!this._sourcesContents) {
          return null;
        }
        if (aSourceRoot != null) {
          source = util.relative(aSourceRoot, source);
        }
        var key = util.toSetString(source);
        return Object.prototype.hasOwnProperty.call(this._sourcesContents,
                                                    key)
          ? this._sourcesContents[key]
          : null;
      }, this);
    };

  /**
   * Externalize the source map.
   */
  SourceMapGenerator.prototype.toJSON =
    function SourceMapGenerator_toJSON() {
      var map = {
        version: this._version,
        sources: this._sources.toArray(),
        names: this._names.toArray(),
        mappings: this._serializeMappings()
      };
      if (this._file != null) {
        map.file = this._file;
      }
      if (this._sourceRoot != null) {
        map.sourceRoot = this._sourceRoot;
      }
      if (this._sourcesContents) {
        map.sourcesContent = this._generateSourcesContent(map.sources, map.sourceRoot);
      }

      return map;
    };

  /**
   * Render the source map being generated to a string.
   */
  SourceMapGenerator.prototype.toString =
    function SourceMapGenerator_toString() {
      return JSON.stringify(this);
    };

  exports.SourceMapGenerator = SourceMapGenerator;

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Based on the Base 64 VLQ implementation in Closure Compiler:
 * https://code.google.com/p/closure-compiler/source/browse/trunk/src/com/google/debugging/sourcemap/Base64VLQ.java
 *
 * Copyright 2011 The Closure Compiler Authors. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Google Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
define('source-map/base64-vlq', ['require', 'exports', 'module' ,  'source-map/base64'], function(require, exports, module) {

  var base64 = require('./base64');

  // A single base 64 digit can contain 6 bits of data. For the base 64 variable
  // length quantities we use in the source map spec, the first bit is the sign,
  // the next four bits are the actual value, and the 6th bit is the
  // continuation bit. The continuation bit tells us whether there are more
  // digits in this value following this digit.
  //
  //   Continuation
  //   |    Sign
  //   |    |
  //   V    V
  //   101011

  var VLQ_BASE_SHIFT = 5;

  // binary: 100000
  var VLQ_BASE = 1 << VLQ_BASE_SHIFT;

  // binary: 011111
  var VLQ_BASE_MASK = VLQ_BASE - 1;

  // binary: 100000
  var VLQ_CONTINUATION_BIT = VLQ_BASE;

  /**
   * Converts from a two-complement value to a value where the sign bit is
   * is placed in the least significant bit.  For example, as decimals:
   *   1 becomes 2 (10 binary), -1 becomes 3 (11 binary)
   *   2 becomes 4 (100 binary), -2 becomes 5 (101 binary)
   */
  function toVLQSigned(aValue) {
    return aValue < 0
      ? ((-aValue) << 1) + 1
      : (aValue << 1) + 0;
  }

  /**
   * Converts to a two-complement value from a value where the sign bit is
   * is placed in the least significant bit.  For example, as decimals:
   *   2 (10 binary) becomes 1, 3 (11 binary) becomes -1
   *   4 (100 binary) becomes 2, 5 (101 binary) becomes -2
   */
  function fromVLQSigned(aValue) {
    var isNegative = (aValue & 1) === 1;
    var shifted = aValue >> 1;
    return isNegative
      ? -shifted
      : shifted;
  }

  /**
   * Returns the base 64 VLQ encoded value.
   */
  exports.encode = function base64VLQ_encode(aValue) {
    var encoded = "";
    var digit;

    var vlq = toVLQSigned(aValue);

    do {
      digit = vlq & VLQ_BASE_MASK;
      vlq >>>= VLQ_BASE_SHIFT;
      if (vlq > 0) {
        // There are still more digits in this value, so we must make sure the
        // continuation bit is marked.
        digit |= VLQ_CONTINUATION_BIT;
      }
      encoded += base64.encode(digit);
    } while (vlq > 0);

    return encoded;
  };

  /**
   * Decodes the next base 64 VLQ value from the given string and returns the
   * value and the rest of the string.
   */
  exports.decode = function base64VLQ_decode(aStr) {
    var i = 0;
    var strLen = aStr.length;
    var result = 0;
    var shift = 0;
    var continuation, digit;

    do {
      if (i >= strLen) {
        throw new Error("Expected more digits in base 64 VLQ value.");
      }
      digit = base64.decode(aStr.charAt(i++));
      continuation = !!(digit & VLQ_CONTINUATION_BIT);
      digit &= VLQ_BASE_MASK;
      result = result + (digit << shift);
      shift += VLQ_BASE_SHIFT;
    } while (continuation);

    return {
      value: fromVLQSigned(result),
      rest: aStr.slice(i)
    };
  };

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/base64', ['require', 'exports', 'module' , ], function(require, exports, module) {

  var charToIntMap = {};
  var intToCharMap = {};

  'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
    .split('')
    .forEach(function (ch, index) {
      charToIntMap[ch] = index;
      intToCharMap[index] = ch;
    });

  /**
   * Encode an integer in the range of 0 to 63 to a single base 64 digit.
   */
  exports.encode = function base64_encode(aNumber) {
    if (aNumber in intToCharMap) {
      return intToCharMap[aNumber];
    }
    throw new TypeError("Must be between 0 and 63: " + aNumber);
  };

  /**
   * Decode a single base 64 digit to an integer.
   */
  exports.decode = function base64_decode(aChar) {
    if (aChar in charToIntMap) {
      return charToIntMap[aChar];
    }
    throw new TypeError("Not a valid base 64 digit: " + aChar);
  };

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/util', ['require', 'exports', 'module' , ], function(require, exports, module) {

  /**
   * This is a helper function for getting values from parameter/options
   * objects.
   *
   * @param args The object we are extracting values from
   * @param name The name of the property we are getting.
   * @param defaultValue An optional value to return if the property is missing
   * from the object. If this is not specified and the property is missing, an
   * error will be thrown.
   */
  function getArg(aArgs, aName, aDefaultValue) {
    if (aName in aArgs) {
      return aArgs[aName];
    } else if (arguments.length === 3) {
      return aDefaultValue;
    } else {
      throw new Error('"' + aName + '" is a required argument.');
    }
  }
  exports.getArg = getArg;

  var urlRegexp = /^(?:([\w+\-.]+):)?\/\/(?:(\w+:\w+)@)?([\w.]*)(?::(\d+))?(\S*)$/;
  var dataUrlRegexp = /^data:.+\,.+$/;

  function urlParse(aUrl) {
    var match = aUrl.match(urlRegexp);
    if (!match) {
      return null;
    }
    return {
      scheme: match[1],
      auth: match[2],
      host: match[3],
      port: match[4],
      path: match[5]
    };
  }
  exports.urlParse = urlParse;

  function urlGenerate(aParsedUrl) {
    var url = '';
    if (aParsedUrl.scheme) {
      url += aParsedUrl.scheme + ':';
    }
    url += '//';
    if (aParsedUrl.auth) {
      url += aParsedUrl.auth + '@';
    }
    if (aParsedUrl.host) {
      url += aParsedUrl.host;
    }
    if (aParsedUrl.port) {
      url += ":" + aParsedUrl.port
    }
    if (aParsedUrl.path) {
      url += aParsedUrl.path;
    }
    return url;
  }
  exports.urlGenerate = urlGenerate;

  /**
   * Normalizes a path, or the path portion of a URL:
   *
   * - Replaces consequtive slashes with one slash.
   * - Removes unnecessary '.' parts.
   * - Removes unnecessary '<dir>/..' parts.
   *
   * Based on code in the Node.js 'path' core module.
   *
   * @param aPath The path or url to normalize.
   */
  function normalize(aPath) {
    var path = aPath;
    var url = urlParse(aPath);
    if (url) {
      if (!url.path) {
        return aPath;
      }
      path = url.path;
    }
    var isAbsolute = (path.charAt(0) === '/');

    var parts = path.split(/\/+/);
    for (var part, up = 0, i = parts.length - 1; i >= 0; i--) {
      part = parts[i];
      if (part === '.') {
        parts.splice(i, 1);
      } else if (part === '..') {
        up++;
      } else if (up > 0) {
        if (part === '') {
          // The first part is blank if the path is absolute. Trying to go
          // above the root is a no-op. Therefore we can remove all '..' parts
          // directly after the root.
          parts.splice(i + 1, up);
          up = 0;
        } else {
          parts.splice(i, 2);
          up--;
        }
      }
    }
    path = parts.join('/');

    if (path === '') {
      path = isAbsolute ? '/' : '.';
    }

    if (url) {
      url.path = path;
      return urlGenerate(url);
    }
    return path;
  }
  exports.normalize = normalize;

  /**
   * Joins two paths/URLs.
   *
   * @param aRoot The root path or URL.
   * @param aPath The path or URL to be joined with the root.
   *
   * - If aPath is a URL or a data URI, aPath is returned, unless aPath is a
   *   scheme-relative URL: Then the scheme of aRoot, if any, is prepended
   *   first.
   * - Otherwise aPath is a path. If aRoot is a URL, then its path portion
   *   is updated with the result and aRoot is returned. Otherwise the result
   *   is returned.
   *   - If aPath is absolute, the result is aPath.
   *   - Otherwise the two paths are joined with a slash.
   * - Joining for example 'http://' and 'www.example.com' is also supported.
   */
  function join(aRoot, aPath) {
    var aPathUrl = urlParse(aPath);
    var aRootUrl = urlParse(aRoot);
    if (aRootUrl) {
      aRoot = aRootUrl.path || '/';
    }

    // `join(foo, '//www.example.org')`
    if (aPathUrl && !aPathUrl.scheme) {
      if (aRootUrl) {
        aPathUrl.scheme = aRootUrl.scheme;
      }
      return urlGenerate(aPathUrl);
    }

    if (aPathUrl || aPath.match(dataUrlRegexp)) {
      return aPath;
    }

    // `join('http://', 'www.example.com')`
    if (aRootUrl && !aRootUrl.host && !aRootUrl.path) {
      aRootUrl.host = aPath;
      return urlGenerate(aRootUrl);
    }

    var joined = aPath.charAt(0) === '/'
      ? aPath
      : normalize(aRoot.replace(/\/+$/, '') + '/' + aPath);

    if (aRootUrl) {
      aRootUrl.path = joined;
      return urlGenerate(aRootUrl);
    }
    return joined;
  }
  exports.join = join;

  /**
   * Because behavior goes wacky when you set `__proto__` on objects, we
   * have to prefix all the strings in our set with an arbitrary character.
   *
   * See https://github.com/mozilla/source-map/pull/31 and
   * https://github.com/mozilla/source-map/issues/30
   *
   * @param String aStr
   */
  function toSetString(aStr) {
    return '$' + aStr;
  }
  exports.toSetString = toSetString;

  function fromSetString(aStr) {
    return aStr.substr(1);
  }
  exports.fromSetString = fromSetString;

  function relative(aRoot, aPath) {
    aRoot = aRoot.replace(/\/$/, '');

    var url = urlParse(aRoot);
    if (aPath.charAt(0) == "/" && url && url.path == "/") {
      return aPath.slice(1);
    }

    return aPath.indexOf(aRoot + '/') === 0
      ? aPath.substr(aRoot.length + 1)
      : aPath;
  }
  exports.relative = relative;

  function strcmp(aStr1, aStr2) {
    var s1 = aStr1 || "";
    var s2 = aStr2 || "";
    return (s1 > s2) - (s1 < s2);
  }

  /**
   * Comparator between two mappings where the original positions are compared.
   *
   * Optionally pass in `true` as `onlyCompareGenerated` to consider two
   * mappings with the same original source/line/column, but different generated
   * line and column the same. Useful when searching for a mapping with a
   * stubbed out mapping.
   */
  function compareByOriginalPositions(mappingA, mappingB, onlyCompareOriginal) {
    var cmp;

    cmp = strcmp(mappingA.source, mappingB.source);
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.originalLine - mappingB.originalLine;
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.originalColumn - mappingB.originalColumn;
    if (cmp || onlyCompareOriginal) {
      return cmp;
    }

    cmp = strcmp(mappingA.name, mappingB.name);
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.generatedLine - mappingB.generatedLine;
    if (cmp) {
      return cmp;
    }

    return mappingA.generatedColumn - mappingB.generatedColumn;
  };
  exports.compareByOriginalPositions = compareByOriginalPositions;

  /**
   * Comparator between two mappings where the generated positions are
   * compared.
   *
   * Optionally pass in `true` as `onlyCompareGenerated` to consider two
   * mappings with the same generated line and column, but different
   * source/name/original line and column the same. Useful when searching for a
   * mapping with a stubbed out mapping.
   */
  function compareByGeneratedPositions(mappingA, mappingB, onlyCompareGenerated) {
    var cmp;

    cmp = mappingA.generatedLine - mappingB.generatedLine;
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.generatedColumn - mappingB.generatedColumn;
    if (cmp || onlyCompareGenerated) {
      return cmp;
    }

    cmp = strcmp(mappingA.source, mappingB.source);
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.originalLine - mappingB.originalLine;
    if (cmp) {
      return cmp;
    }

    cmp = mappingA.originalColumn - mappingB.originalColumn;
    if (cmp) {
      return cmp;
    }

    return strcmp(mappingA.name, mappingB.name);
  };
  exports.compareByGeneratedPositions = compareByGeneratedPositions;

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/array-set', ['require', 'exports', 'module' ,  'source-map/util'], function(require, exports, module) {

  var util = require('./util');

  /**
   * A data structure which is a combination of an array and a set. Adding a new
   * member is O(1), testing for membership is O(1), and finding the index of an
   * element is O(1). Removing elements from the set is not supported. Only
   * strings are supported for membership.
   */
  function ArraySet() {
    this._array = [];
    this._set = {};
  }

  /**
   * Static method for creating ArraySet instances from an existing array.
   */
  ArraySet.fromArray = function ArraySet_fromArray(aArray, aAllowDuplicates) {
    var set = new ArraySet();
    for (var i = 0, len = aArray.length; i < len; i++) {
      set.add(aArray[i], aAllowDuplicates);
    }
    return set;
  };

  /**
   * Add the given string to this set.
   *
   * @param String aStr
   */
  ArraySet.prototype.add = function ArraySet_add(aStr, aAllowDuplicates) {
    var isDuplicate = this.has(aStr);
    var idx = this._array.length;
    if (!isDuplicate || aAllowDuplicates) {
      this._array.push(aStr);
    }
    if (!isDuplicate) {
      this._set[util.toSetString(aStr)] = idx;
    }
  };

  /**
   * Is the given string a member of this set?
   *
   * @param String aStr
   */
  ArraySet.prototype.has = function ArraySet_has(aStr) {
    return Object.prototype.hasOwnProperty.call(this._set,
                                                util.toSetString(aStr));
  };

  /**
   * What is the index of the given string in the array?
   *
   * @param String aStr
   */
  ArraySet.prototype.indexOf = function ArraySet_indexOf(aStr) {
    if (this.has(aStr)) {
      return this._set[util.toSetString(aStr)];
    }
    throw new Error('"' + aStr + '" is not in the set.');
  };

  /**
   * What is the element at the given index?
   *
   * @param Number aIdx
   */
  ArraySet.prototype.at = function ArraySet_at(aIdx) {
    if (aIdx >= 0 && aIdx < this._array.length) {
      return this._array[aIdx];
    }
    throw new Error('No element indexed by ' + aIdx);
  };

  /**
   * Returns the array representation of this set (which has the proper indices
   * indicated by indexOf). Note that this is a copy of the internal array used
   * for storing the members so that no one can mess with internal state.
   */
  ArraySet.prototype.toArray = function ArraySet_toArray() {
    return this._array.slice();
  };

  exports.ArraySet = ArraySet;

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/source-map-consumer', ['require', 'exports', 'module' ,  'source-map/util', 'source-map/binary-search', 'source-map/array-set', 'source-map/base64-vlq'], function(require, exports, module) {

  var util = require('./util');
  var binarySearch = require('./binary-search');
  var ArraySet = require('./array-set').ArraySet;
  var base64VLQ = require('./base64-vlq');

  /**
   * A SourceMapConsumer instance represents a parsed source map which we can
   * query for information about the original file positions by giving it a file
   * position in the generated source.
   *
   * The only parameter is the raw source map (either as a JSON string, or
   * already parsed to an object). According to the spec, source maps have the
   * following attributes:
   *
   *   - version: Which version of the source map spec this map is following.
   *   - sources: An array of URLs to the original source files.
   *   - names: An array of identifiers which can be referrenced by individual mappings.
   *   - sourceRoot: Optional. The URL root from which all sources are relative.
   *   - sourcesContent: Optional. An array of contents of the original source files.
   *   - mappings: A string of base64 VLQs which contain the actual mappings.
   *   - file: Optional. The generated file this source map is associated with.
   *
   * Here is an example source map, taken from the source map spec[0]:
   *
   *     {
   *       version : 3,
   *       file: "out.js",
   *       sourceRoot : "",
   *       sources: ["foo.js", "bar.js"],
   *       names: ["src", "maps", "are", "fun"],
   *       mappings: "AA,AB;;ABCDE;"
   *     }
   *
   * [0]: https://docs.google.com/document/d/1U1RGAehQwRypUTovF1KRlpiOFze0b-_2gc6fAH0KY0k/edit?pli=1#
   */
  function SourceMapConsumer(aSourceMap) {
    var sourceMap = aSourceMap;
    if (typeof aSourceMap === 'string') {
      sourceMap = JSON.parse(aSourceMap.replace(/^\)\]\}'/, ''));
    }

    var version = util.getArg(sourceMap, 'version');
    var sources = util.getArg(sourceMap, 'sources');
    // Sass 3.3 leaves out the 'names' array, so we deviate from the spec (which
    // requires the array) to play nice here.
    var names = util.getArg(sourceMap, 'names', []);
    var sourceRoot = util.getArg(sourceMap, 'sourceRoot', null);
    var sourcesContent = util.getArg(sourceMap, 'sourcesContent', null);
    var mappings = util.getArg(sourceMap, 'mappings');
    var file = util.getArg(sourceMap, 'file', null);

    // Once again, Sass deviates from the spec and supplies the version as a
    // string rather than a number, so we use loose equality checking here.
    if (version != this._version) {
      throw new Error('Unsupported version: ' + version);
    }

    // Pass `true` below to allow duplicate names and sources. While source maps
    // are intended to be compressed and deduplicated, the TypeScript compiler
    // sometimes generates source maps with duplicates in them. See Github issue
    // #72 and bugzil.la/889492.
    this._names = ArraySet.fromArray(names, true);
    this._sources = ArraySet.fromArray(sources, true);

    this.sourceRoot = sourceRoot;
    this.sourcesContent = sourcesContent;
    this._mappings = mappings;
    this.file = file;
  }

  /**
   * Create a SourceMapConsumer from a SourceMapGenerator.
   *
   * @param SourceMapGenerator aSourceMap
   *        The source map that will be consumed.
   * @returns SourceMapConsumer
   */
  SourceMapConsumer.fromSourceMap =
    function SourceMapConsumer_fromSourceMap(aSourceMap) {
      var smc = Object.create(SourceMapConsumer.prototype);

      smc._names = ArraySet.fromArray(aSourceMap._names.toArray(), true);
      smc._sources = ArraySet.fromArray(aSourceMap._sources.toArray(), true);
      smc.sourceRoot = aSourceMap._sourceRoot;
      smc.sourcesContent = aSourceMap._generateSourcesContent(smc._sources.toArray(),
                                                              smc.sourceRoot);
      smc.file = aSourceMap._file;

      smc.__generatedMappings = aSourceMap._mappings.slice()
        .sort(util.compareByGeneratedPositions);
      smc.__originalMappings = aSourceMap._mappings.slice()
        .sort(util.compareByOriginalPositions);

      return smc;
    };

  /**
   * The version of the source mapping spec that we are consuming.
   */
  SourceMapConsumer.prototype._version = 3;

  /**
   * The list of original sources.
   */
  Object.defineProperty(SourceMapConsumer.prototype, 'sources', {
    get: function () {
      return this._sources.toArray().map(function (s) {
        return this.sourceRoot != null ? util.join(this.sourceRoot, s) : s;
      }, this);
    }
  });

  // `__generatedMappings` and `__originalMappings` are arrays that hold the
  // parsed mapping coordinates from the source map's "mappings" attribute. They
  // are lazily instantiated, accessed via the `_generatedMappings` and
  // `_originalMappings` getters respectively, and we only parse the mappings
  // and create these arrays once queried for a source location. We jump through
  // these hoops because there can be many thousands of mappings, and parsing
  // them is expensive, so we only want to do it if we must.
  //
  // Each object in the arrays is of the form:
  //
  //     {
  //       generatedLine: The line number in the generated code,
  //       generatedColumn: The column number in the generated code,
  //       source: The path to the original source file that generated this
  //               chunk of code,
  //       originalLine: The line number in the original source that
  //                     corresponds to this chunk of generated code,
  //       originalColumn: The column number in the original source that
  //                       corresponds to this chunk of generated code,
  //       name: The name of the original symbol which generated this chunk of
  //             code.
  //     }
  //
  // All properties except for `generatedLine` and `generatedColumn` can be
  // `null`.
  //
  // `_generatedMappings` is ordered by the generated positions.
  //
  // `_originalMappings` is ordered by the original positions.

  SourceMapConsumer.prototype.__generatedMappings = null;
  Object.defineProperty(SourceMapConsumer.prototype, '_generatedMappings', {
    get: function () {
      if (!this.__generatedMappings) {
        this.__generatedMappings = [];
        this.__originalMappings = [];
        this._parseMappings(this._mappings, this.sourceRoot);
      }

      return this.__generatedMappings;
    }
  });

  SourceMapConsumer.prototype.__originalMappings = null;
  Object.defineProperty(SourceMapConsumer.prototype, '_originalMappings', {
    get: function () {
      if (!this.__originalMappings) {
        this.__generatedMappings = [];
        this.__originalMappings = [];
        this._parseMappings(this._mappings, this.sourceRoot);
      }

      return this.__originalMappings;
    }
  });

  /**
   * Parse the mappings in a string in to a data structure which we can easily
   * query (the ordered arrays in the `this.__generatedMappings` and
   * `this.__originalMappings` properties).
   */
  SourceMapConsumer.prototype._parseMappings =
    function SourceMapConsumer_parseMappings(aStr, aSourceRoot) {
      var generatedLine = 1;
      var previousGeneratedColumn = 0;
      var previousOriginalLine = 0;
      var previousOriginalColumn = 0;
      var previousSource = 0;
      var previousName = 0;
      var mappingSeparator = /^[,;]/;
      var str = aStr;
      var mapping;
      var temp;

      while (str.length > 0) {
        if (str.charAt(0) === ';') {
          generatedLine++;
          str = str.slice(1);
          previousGeneratedColumn = 0;
        }
        else if (str.charAt(0) === ',') {
          str = str.slice(1);
        }
        else {
          mapping = {};
          mapping.generatedLine = generatedLine;

          // Generated column.
          temp = base64VLQ.decode(str);
          mapping.generatedColumn = previousGeneratedColumn + temp.value;
          previousGeneratedColumn = mapping.generatedColumn;
          str = temp.rest;

          if (str.length > 0 && !mappingSeparator.test(str.charAt(0))) {
            // Original source.
            temp = base64VLQ.decode(str);
            mapping.source = this._sources.at(previousSource + temp.value);
            previousSource += temp.value;
            str = temp.rest;
            if (str.length === 0 || mappingSeparator.test(str.charAt(0))) {
              throw new Error('Found a source, but no line and column');
            }

            // Original line.
            temp = base64VLQ.decode(str);
            mapping.originalLine = previousOriginalLine + temp.value;
            previousOriginalLine = mapping.originalLine;
            // Lines are stored 0-based
            mapping.originalLine += 1;
            str = temp.rest;
            if (str.length === 0 || mappingSeparator.test(str.charAt(0))) {
              throw new Error('Found a source and line, but no column');
            }

            // Original column.
            temp = base64VLQ.decode(str);
            mapping.originalColumn = previousOriginalColumn + temp.value;
            previousOriginalColumn = mapping.originalColumn;
            str = temp.rest;

            if (str.length > 0 && !mappingSeparator.test(str.charAt(0))) {
              // Original name.
              temp = base64VLQ.decode(str);
              mapping.name = this._names.at(previousName + temp.value);
              previousName += temp.value;
              str = temp.rest;
            }
          }

          this.__generatedMappings.push(mapping);
          if (typeof mapping.originalLine === 'number') {
            this.__originalMappings.push(mapping);
          }
        }
      }

      this.__generatedMappings.sort(util.compareByGeneratedPositions);
      this.__originalMappings.sort(util.compareByOriginalPositions);
    };

  /**
   * Find the mapping that best matches the hypothetical "needle" mapping that
   * we are searching for in the given "haystack" of mappings.
   */
  SourceMapConsumer.prototype._findMapping =
    function SourceMapConsumer_findMapping(aNeedle, aMappings, aLineName,
                                           aColumnName, aComparator) {
      // To return the position we are searching for, we must first find the
      // mapping for the given position and then return the opposite position it
      // points to. Because the mappings are sorted, we can use binary search to
      // find the best mapping.

      if (aNeedle[aLineName] <= 0) {
        throw new TypeError('Line must be greater than or equal to 1, got '
                            + aNeedle[aLineName]);
      }
      if (aNeedle[aColumnName] < 0) {
        throw new TypeError('Column must be greater than or equal to 0, got '
                            + aNeedle[aColumnName]);
      }

      return binarySearch.search(aNeedle, aMappings, aComparator);
    };

  /**
   * Returns the original source, line, and column information for the generated
   * source's line and column positions provided. The only argument is an object
   * with the following properties:
   *
   *   - line: The line number in the generated source.
   *   - column: The column number in the generated source.
   *
   * and an object is returned with the following properties:
   *
   *   - source: The original source file, or null.
   *   - line: The line number in the original source, or null.
   *   - column: The column number in the original source, or null.
   *   - name: The original identifier, or null.
   */
  SourceMapConsumer.prototype.originalPositionFor =
    function SourceMapConsumer_originalPositionFor(aArgs) {
      var needle = {
        generatedLine: util.getArg(aArgs, 'line'),
        generatedColumn: util.getArg(aArgs, 'column')
      };

      var mapping = this._findMapping(needle,
                                      this._generatedMappings,
                                      "generatedLine",
                                      "generatedColumn",
                                      util.compareByGeneratedPositions);

      if (mapping && mapping.generatedLine === needle.generatedLine) {
        var source = util.getArg(mapping, 'source', null);
        if (source != null && this.sourceRoot != null) {
          source = util.join(this.sourceRoot, source);
        }
        return {
          source: source,
          line: util.getArg(mapping, 'originalLine', null),
          column: util.getArg(mapping, 'originalColumn', null),
          name: util.getArg(mapping, 'name', null)
        };
      }

      return {
        source: null,
        line: null,
        column: null,
        name: null
      };
    };

  /**
   * Returns the original source content. The only argument is the url of the
   * original source file. Returns null if no original source content is
   * availible.
   */
  SourceMapConsumer.prototype.sourceContentFor =
    function SourceMapConsumer_sourceContentFor(aSource) {
      if (!this.sourcesContent) {
        return null;
      }

      if (this.sourceRoot != null) {
        aSource = util.relative(this.sourceRoot, aSource);
      }

      if (this._sources.has(aSource)) {
        return this.sourcesContent[this._sources.indexOf(aSource)];
      }

      var url;
      if (this.sourceRoot != null
          && (url = util.urlParse(this.sourceRoot))) {
        // XXX: file:// URIs and absolute paths lead to unexpected behavior for
        // many users. We can help them out when they expect file:// URIs to
        // behave like it would if they were running a local HTTP server. See
        // https://bugzilla.mozilla.org/show_bug.cgi?id=885597.
        var fileUriAbsPath = aSource.replace(/^file:\/\//, "");
        if (url.scheme == "file"
            && this._sources.has(fileUriAbsPath)) {
          return this.sourcesContent[this._sources.indexOf(fileUriAbsPath)]
        }

        if ((!url.path || url.path == "/")
            && this._sources.has("/" + aSource)) {
          return this.sourcesContent[this._sources.indexOf("/" + aSource)];
        }
      }

      throw new Error('"' + aSource + '" is not in the SourceMap.');
    };

  /**
   * Returns the generated line and column information for the original source,
   * line, and column positions provided. The only argument is an object with
   * the following properties:
   *
   *   - source: The filename of the original source.
   *   - line: The line number in the original source.
   *   - column: The column number in the original source.
   *
   * and an object is returned with the following properties:
   *
   *   - line: The line number in the generated source, or null.
   *   - column: The column number in the generated source, or null.
   */
  SourceMapConsumer.prototype.generatedPositionFor =
    function SourceMapConsumer_generatedPositionFor(aArgs) {
      var needle = {
        source: util.getArg(aArgs, 'source'),
        originalLine: util.getArg(aArgs, 'line'),
        originalColumn: util.getArg(aArgs, 'column')
      };

      if (this.sourceRoot != null) {
        needle.source = util.relative(this.sourceRoot, needle.source);
      }

      var mapping = this._findMapping(needle,
                                      this._originalMappings,
                                      "originalLine",
                                      "originalColumn",
                                      util.compareByOriginalPositions);

      if (mapping) {
        return {
          line: util.getArg(mapping, 'generatedLine', null),
          column: util.getArg(mapping, 'generatedColumn', null)
        };
      }

      return {
        line: null,
        column: null
      };
    };

  SourceMapConsumer.GENERATED_ORDER = 1;
  SourceMapConsumer.ORIGINAL_ORDER = 2;

  /**
   * Iterate over each mapping between an original source/line/column and a
   * generated line/column in this source map.
   *
   * @param Function aCallback
   *        The function that is called with each mapping.
   * @param Object aContext
   *        Optional. If specified, this object will be the value of `this` every
   *        time that `aCallback` is called.
   * @param aOrder
   *        Either `SourceMapConsumer.GENERATED_ORDER` or
   *        `SourceMapConsumer.ORIGINAL_ORDER`. Specifies whether you want to
   *        iterate over the mappings sorted by the generated file's line/column
   *        order or the original's source/line/column order, respectively. Defaults to
   *        `SourceMapConsumer.GENERATED_ORDER`.
   */
  SourceMapConsumer.prototype.eachMapping =
    function SourceMapConsumer_eachMapping(aCallback, aContext, aOrder) {
      var context = aContext || null;
      var order = aOrder || SourceMapConsumer.GENERATED_ORDER;

      var mappings;
      switch (order) {
      case SourceMapConsumer.GENERATED_ORDER:
        mappings = this._generatedMappings;
        break;
      case SourceMapConsumer.ORIGINAL_ORDER:
        mappings = this._originalMappings;
        break;
      default:
        throw new Error("Unknown order of iteration.");
      }

      var sourceRoot = this.sourceRoot;
      mappings.map(function (mapping) {
        var source = mapping.source;
        if (source != null && sourceRoot != null) {
          source = util.join(sourceRoot, source);
        }
        return {
          source: source,
          generatedLine: mapping.generatedLine,
          generatedColumn: mapping.generatedColumn,
          originalLine: mapping.originalLine,
          originalColumn: mapping.originalColumn,
          name: mapping.name
        };
      }).forEach(aCallback, context);
    };

  exports.SourceMapConsumer = SourceMapConsumer;

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/binary-search', ['require', 'exports', 'module' , ], function(require, exports, module) {

  /**
   * Recursive implementation of binary search.
   *
   * @param aLow Indices here and lower do not contain the needle.
   * @param aHigh Indices here and higher do not contain the needle.
   * @param aNeedle The element being searched for.
   * @param aHaystack The non-empty array being searched.
   * @param aCompare Function which takes two elements and returns -1, 0, or 1.
   */
  function recursiveSearch(aLow, aHigh, aNeedle, aHaystack, aCompare) {
    // This function terminates when one of the following is true:
    //
    //   1. We find the exact element we are looking for.
    //
    //   2. We did not find the exact element, but we can return the next
    //      closest element that is less than that element.
    //
    //   3. We did not find the exact element, and there is no next-closest
    //      element which is less than the one we are searching for, so we
    //      return null.
    var mid = Math.floor((aHigh - aLow) / 2) + aLow;
    var cmp = aCompare(aNeedle, aHaystack[mid], true);
    if (cmp === 0) {
      // Found the element we are looking for.
      return aHaystack[mid];
    }
    else if (cmp > 0) {
      // aHaystack[mid] is greater than our needle.
      if (aHigh - mid > 1) {
        // The element is in the upper half.
        return recursiveSearch(mid, aHigh, aNeedle, aHaystack, aCompare);
      }
      // We did not find an exact match, return the next closest one
      // (termination case 2).
      return aHaystack[mid];
    }
    else {
      // aHaystack[mid] is less than our needle.
      if (mid - aLow > 1) {
        // The element is in the lower half.
        return recursiveSearch(aLow, mid, aNeedle, aHaystack, aCompare);
      }
      // The exact needle element was not found in this haystack. Determine if
      // we are in termination case (2) or (3) and return the appropriate thing.
      return aLow < 0
        ? null
        : aHaystack[aLow];
    }
  }

  /**
   * This is an implementation of binary search which will always try and return
   * the next lowest value checked if there is no exact hit. This is because
   * mappings between original and generated line/col pairs are single points,
   * and there is an implicit region between each of them, so a miss just means
   * that you aren't on the very start of a region.
   *
   * @param aNeedle The element you are looking for.
   * @param aHaystack The array that is being searched.
   * @param aCompare A function which takes the needle and an element in the
   *     array and returns -1, 0, or 1 depending on whether the needle is less
   *     than, equal to, or greater than the element, respectively.
   */
  exports.search = function search(aNeedle, aHaystack, aCompare) {
    return aHaystack.length > 0
      ? recursiveSearch(-1, aHaystack.length, aNeedle, aHaystack, aCompare)
      : null;
  };

});
/* -*- Mode: js; js-indent-level: 2; -*- */
/*
 * Copyright 2011 Mozilla Foundation and contributors
 * Licensed under the New BSD license. See LICENSE or:
 * http://opensource.org/licenses/BSD-3-Clause
 */
define('source-map/source-node', ['require', 'exports', 'module' ,  'source-map/source-map-generator', 'source-map/util'], function(require, exports, module) {

  var SourceMapGenerator = require('./source-map-generator').SourceMapGenerator;
  var util = require('./util');

  // Matches a Windows-style `\r\n` newline or a `\n` newline used by all other
  // operating systems these days (capturing the result).
  var REGEX_NEWLINE = /(\r?\n)/;

  // Matches a Windows-style newline, or any character.
  var REGEX_CHARACTER = /\r\n|[\s\S]/g;

  /**
   * SourceNodes provide a way to abstract over interpolating/concatenating
   * snippets of generated JavaScript source code while maintaining the line and
   * column information associated with the original source code.
   *
   * @param aLine The original line number.
   * @param aColumn The original column number.
   * @param aSource The original source's filename.
   * @param aChunks Optional. An array of strings which are snippets of
   *        generated JS, or other SourceNodes.
   * @param aName The original identifier.
   */
  function SourceNode(aLine, aColumn, aSource, aChunks, aName) {
    this.children = [];
    this.sourceContents = {};
    this.line = aLine == null ? null : aLine;
    this.column = aColumn == null ? null : aColumn;
    this.source = aSource == null ? null : aSource;
    this.name = aName == null ? null : aName;
    if (aChunks != null) this.add(aChunks);
  }

  /**
   * Creates a SourceNode from generated code and a SourceMapConsumer.
   *
   * @param aGeneratedCode The generated code
   * @param aSourceMapConsumer The SourceMap for the generated code
   * @param aRelativePath Optional. The path that relative sources in the
   *        SourceMapConsumer should be relative to.
   */
  SourceNode.fromStringWithSourceMap =
    function SourceNode_fromStringWithSourceMap(aGeneratedCode, aSourceMapConsumer, aRelativePath) {
      // The SourceNode we want to fill with the generated code
      // and the SourceMap
      var node = new SourceNode();

      // All even indices of this array are one line of the generated code,
      // while all odd indices are the newlines between two adjacent lines
      // (since `REGEX_NEWLINE` captures its match).
      // Processed fragments are removed from this array, by calling `shiftNextLine`.
      var remainingLines = aGeneratedCode.split(REGEX_NEWLINE);
      var shiftNextLine = function() {
        var lineContents = remainingLines.shift();
        // The last line of a file might not have a newline.
        var newLine = remainingLines.shift() || "";
        return lineContents + newLine;
      };

      // We need to remember the position of "remainingLines"
      var lastGeneratedLine = 1, lastGeneratedColumn = 0;

      // The generate SourceNodes we need a code range.
      // To extract it current and last mapping is used.
      // Here we store the last mapping.
      var lastMapping = null;

      aSourceMapConsumer.eachMapping(function (mapping) {
        if (lastMapping !== null) {
          // We add the code from "lastMapping" to "mapping":
          // First check if there is a new line in between.
          if (lastGeneratedLine < mapping.generatedLine) {
            var code = "";
            // Associate first line with "lastMapping"
            addMappingWithCode(lastMapping, shiftNextLine());
            lastGeneratedLine++;
            lastGeneratedColumn = 0;
            // The remaining code is added without mapping
          } else {
            // There is no new line in between.
            // Associate the code between "lastGeneratedColumn" and
            // "mapping.generatedColumn" with "lastMapping"
            var nextLine = remainingLines[0];
            var code = nextLine.substr(0, mapping.generatedColumn -
                                          lastGeneratedColumn);
            remainingLines[0] = nextLine.substr(mapping.generatedColumn -
                                                lastGeneratedColumn);
            lastGeneratedColumn = mapping.generatedColumn;
            addMappingWithCode(lastMapping, code);
            // No more remaining code, continue
            lastMapping = mapping;
            return;
          }
        }
        // We add the generated code until the first mapping
        // to the SourceNode without any mapping.
        // Each line is added as separate string.
        while (lastGeneratedLine < mapping.generatedLine) {
          node.add(shiftNextLine());
          lastGeneratedLine++;
        }
        if (lastGeneratedColumn < mapping.generatedColumn) {
          var nextLine = remainingLines[0];
          node.add(nextLine.substr(0, mapping.generatedColumn));
          remainingLines[0] = nextLine.substr(mapping.generatedColumn);
          lastGeneratedColumn = mapping.generatedColumn;
        }
        lastMapping = mapping;
      }, this);
      // We have processed all mappings.
      if (remainingLines.length > 0) {
        if (lastMapping) {
          // Associate the remaining code in the current line with "lastMapping"
          addMappingWithCode(lastMapping, shiftNextLine());
        }
        // and add the remaining lines without any mapping
        node.add(remainingLines.join(""));
      }

      // Copy sourcesContent into SourceNode
      aSourceMapConsumer.sources.forEach(function (sourceFile) {
        var content = aSourceMapConsumer.sourceContentFor(sourceFile);
        if (content != null) {
          if (aRelativePath != null) {
            sourceFile = util.join(aRelativePath, sourceFile);
          }
          node.setSourceContent(sourceFile, content);
        }
      });

      return node;

      function addMappingWithCode(mapping, code) {
        if (mapping === null || mapping.source === undefined) {
          node.add(code);
        } else {
          var source = aRelativePath
            ? util.join(aRelativePath, mapping.source)
            : mapping.source;
          node.add(new SourceNode(mapping.originalLine,
                                  mapping.originalColumn,
                                  source,
                                  code,
                                  mapping.name));
        }
      }
    };

  /**
   * Add a chunk of generated JS to this source node.
   *
   * @param aChunk A string snippet of generated JS code, another instance of
   *        SourceNode, or an array where each member is one of those things.
   */
  SourceNode.prototype.add = function SourceNode_add(aChunk) {
    if (Array.isArray(aChunk)) {
      aChunk.forEach(function (chunk) {
        this.add(chunk);
      }, this);
    }
    else if (aChunk instanceof SourceNode || typeof aChunk === "string") {
      if (aChunk) {
        this.children.push(aChunk);
      }
    }
    else {
      throw new TypeError(
        "Expected a SourceNode, string, or an array of SourceNodes and strings. Got " + aChunk
      );
    }
    return this;
  };

  /**
   * Add a chunk of generated JS to the beginning of this source node.
   *
   * @param aChunk A string snippet of generated JS code, another instance of
   *        SourceNode, or an array where each member is one of those things.
   */
  SourceNode.prototype.prepend = function SourceNode_prepend(aChunk) {
    if (Array.isArray(aChunk)) {
      for (var i = aChunk.length-1; i >= 0; i--) {
        this.prepend(aChunk[i]);
      }
    }
    else if (aChunk instanceof SourceNode || typeof aChunk === "string") {
      this.children.unshift(aChunk);
    }
    else {
      throw new TypeError(
        "Expected a SourceNode, string, or an array of SourceNodes and strings. Got " + aChunk
      );
    }
    return this;
  };

  /**
   * Walk over the tree of JS snippets in this node and its children. The
   * walking function is called once for each snippet of JS and is passed that
   * snippet and the its original associated source's line/column location.
   *
   * @param aFn The traversal function.
   */
  SourceNode.prototype.walk = function SourceNode_walk(aFn) {
    var chunk;
    for (var i = 0, len = this.children.length; i < len; i++) {
      chunk = this.children[i];
      if (chunk instanceof SourceNode) {
        chunk.walk(aFn);
      }
      else {
        if (chunk !== '') {
          aFn(chunk, { source: this.source,
                       line: this.line,
                       column: this.column,
                       name: this.name });
        }
      }
    }
  };

  /**
   * Like `String.prototype.join` except for SourceNodes. Inserts `aStr` between
   * each of `this.children`.
   *
   * @param aSep The separator.
   */
  SourceNode.prototype.join = function SourceNode_join(aSep) {
    var newChildren;
    var i;
    var len = this.children.length;
    if (len > 0) {
      newChildren = [];
      for (i = 0; i < len-1; i++) {
        newChildren.push(this.children[i]);
        newChildren.push(aSep);
      }
      newChildren.push(this.children[i]);
      this.children = newChildren;
    }
    return this;
  };

  /**
   * Call String.prototype.replace on the very right-most source snippet. Useful
   * for trimming whitespace from the end of a source node, etc.
   *
   * @param aPattern The pattern to replace.
   * @param aReplacement The thing to replace the pattern with.
   */
  SourceNode.prototype.replaceRight = function SourceNode_replaceRight(aPattern, aReplacement) {
    var lastChild = this.children[this.children.length - 1];
    if (lastChild instanceof SourceNode) {
      lastChild.replaceRight(aPattern, aReplacement);
    }
    else if (typeof lastChild === 'string') {
      this.children[this.children.length - 1] = lastChild.replace(aPattern, aReplacement);
    }
    else {
      this.children.push(''.replace(aPattern, aReplacement));
    }
    return this;
  };

  /**
   * Set the source content for a source file. This will be added to the SourceMapGenerator
   * in the sourcesContent field.
   *
   * @param aSourceFile The filename of the source file
   * @param aSourceContent The content of the source file
   */
  SourceNode.prototype.setSourceContent =
    function SourceNode_setSourceContent(aSourceFile, aSourceContent) {
      this.sourceContents[util.toSetString(aSourceFile)] = aSourceContent;
    };

  /**
   * Walk over the tree of SourceNodes. The walking function is called for each
   * source file content and is passed the filename and source content.
   *
   * @param aFn The traversal function.
   */
  SourceNode.prototype.walkSourceContents =
    function SourceNode_walkSourceContents(aFn) {
      for (var i = 0, len = this.children.length; i < len; i++) {
        if (this.children[i] instanceof SourceNode) {
          this.children[i].walkSourceContents(aFn);
        }
      }

      var sources = Object.keys(this.sourceContents);
      for (var i = 0, len = sources.length; i < len; i++) {
        aFn(util.fromSetString(sources[i]), this.sourceContents[sources[i]]);
      }
    };

  /**
   * Return the string representation of this source node. Walks over the tree
   * and concatenates all the various snippets together to one string.
   */
  SourceNode.prototype.toString = function SourceNode_toString() {
    var str = "";
    this.walk(function (chunk) {
      str += chunk;
    });
    return str;
  };

  /**
   * Returns the string representation of this source node along with a source
   * map.
   */
  SourceNode.prototype.toStringWithSourceMap = function SourceNode_toStringWithSourceMap(aArgs) {
    var generated = {
      code: "",
      line: 1,
      column: 0
    };
    var map = new SourceMapGenerator(aArgs);
    var sourceMappingActive = false;
    var lastOriginalSource = null;
    var lastOriginalLine = null;
    var lastOriginalColumn = null;
    var lastOriginalName = null;
    this.walk(function (chunk, original) {
      generated.code += chunk;
      if (original.source !== null
          && original.line !== null
          && original.column !== null) {
        if(lastOriginalSource !== original.source
           || lastOriginalLine !== original.line
           || lastOriginalColumn !== original.column
           || lastOriginalName !== original.name) {
          map.addMapping({
            source: original.source,
            original: {
              line: original.line,
              column: original.column
            },
            generated: {
              line: generated.line,
              column: generated.column
            },
            name: original.name
          });
        }
        lastOriginalSource = original.source;
        lastOriginalLine = original.line;
        lastOriginalColumn = original.column;
        lastOriginalName = original.name;
        sourceMappingActive = true;
      } else if (sourceMappingActive) {
        map.addMapping({
          generated: {
            line: generated.line,
            column: generated.column
          }
        });
        lastOriginalSource = null;
        sourceMappingActive = false;
      }
      chunk.match(REGEX_CHARACTER).forEach(function (ch, idx, array) {
        if (REGEX_NEWLINE.test(ch)) {
          generated.line++;
          generated.column = 0;
          // Mappings end at eol
          if (idx + 1 === array.length) {
            lastOriginalSource = null;
            sourceMappingActive = false;
          } else if (sourceMappingActive) {
            map.addMapping({
              source: original.source,
              original: {
                line: original.line,
                column: original.column
              },
              generated: {
                line: generated.line,
                column: generated.column
              },
              name: original.name
            });
          }
        } else {
          generated.column += ch.length;
        }
      });
    });
    this.walkSourceContents(function (sourceFile, sourceContent) {
      map.setSourceContent(sourceFile, sourceContent);
    });

    return { code: generated.code, map: map };
  };

  exports.SourceNode = SourceNode;

});
/* -*- Mode: js; js-indent-level: 2; -*- */
///////////////////////////////////////////////////////////////////////////////

this.sourceMap = {
  SourceMapConsumer: require('source-map/source-map-consumer').SourceMapConsumer,
  SourceMapGenerator: require('source-map/source-map-generator').SourceMapGenerator,
  SourceNode: require('source-map/source-node').SourceNode
};
MOZ_SourceMap = sourceMap;(function(exports,global){global["UglifyJS"]=exports;/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function array_to_hash(a){var ret=Object.create(null);for(var i=0;i<a.length;++i)ret[a[i]]=true;return ret}function slice(a,start){return Array.prototype.slice.call(a,start||0)}function characters(str){return str.split("")}function member(name,array){for(var i=array.length;--i>=0;)if(array[i]==name)return true;return false}function find_if(func,array){for(var i=0,n=array.length;i<n;++i){if(func(array[i]))return array[i]}}function repeat_string(str,i){if(i<=0)return"";if(i==1)return str;var d=repeat_string(str,i>>1);d+=d;if(i&1)d+=str;return d}function DefaultsError(msg,defs){Error.call(this,msg);this.msg=msg;this.defs=defs}DefaultsError.prototype=Object.create(Error.prototype);DefaultsError.prototype.constructor=DefaultsError;DefaultsError.croak=function(msg,defs){throw new DefaultsError(msg,defs)};function defaults(args,defs,croak){if(args===true)args={};var ret=args||{};if(croak)for(var i in ret)if(ret.hasOwnProperty(i)&&!defs.hasOwnProperty(i))DefaultsError.croak("`"+i+"` is not a supported option",defs);for(var i in defs)if(defs.hasOwnProperty(i)){ret[i]=args&&args.hasOwnProperty(i)?args[i]:defs[i]}return ret}function merge(obj,ext){var count=0;for(var i in ext)if(ext.hasOwnProperty(i)){obj[i]=ext[i];count++}return count}function noop(){}var MAP=function(){function MAP(a,f,backwards){var ret=[],top=[],i;function doit(){var val=f(a[i],i);var is_last=val instanceof Last;if(is_last)val=val.v;if(val instanceof AtTop){val=val.v;if(val instanceof Splice){top.push.apply(top,backwards?val.v.slice().reverse():val.v)}else{top.push(val)}}else if(val!==skip){if(val instanceof Splice){ret.push.apply(ret,backwards?val.v.slice().reverse():val.v)}else{ret.push(val)}}return is_last}if(a instanceof Array){if(backwards){for(i=a.length;--i>=0;)if(doit())break;ret.reverse();top.reverse()}else{for(i=0;i<a.length;++i)if(doit())break}}else{for(i in a)if(a.hasOwnProperty(i))if(doit())break}return top.concat(ret)}MAP.at_top=function(val){return new AtTop(val)};MAP.splice=function(val){return new Splice(val)};MAP.last=function(val){return new Last(val)};var skip=MAP.skip={};function AtTop(val){this.v=val}function Splice(val){this.v=val}function Last(val){this.v=val}return MAP}();function push_uniq(array,el){if(array.indexOf(el)<0)array.push(el)}function string_template(text,props){return text.replace(/\{(.+?)\}/g,function(str,p){return props[p]})}function remove(array,el){for(var i=array.length;--i>=0;){if(array[i]===el)array.splice(i,1)}}function mergeSort(array,cmp){if(array.length<2)return array.slice();function merge(a,b){var r=[],ai=0,bi=0,i=0;while(ai<a.length&&bi<b.length){cmp(a[ai],b[bi])<=0?r[i++]=a[ai++]:r[i++]=b[bi++]}if(ai<a.length)r.push.apply(r,a.slice(ai));if(bi<b.length)r.push.apply(r,b.slice(bi));return r}function _ms(a){if(a.length<=1)return a;var m=Math.floor(a.length/2),left=a.slice(0,m),right=a.slice(m);left=_ms(left);right=_ms(right);return merge(left,right)}return _ms(array)}function set_difference(a,b){return a.filter(function(el){return b.indexOf(el)<0})}function set_intersection(a,b){return a.filter(function(el){return b.indexOf(el)>=0})}function makePredicate(words){if(!(words instanceof Array))words=words.split(" ");var f="",cats=[];out:for(var i=0;i<words.length;++i){for(var j=0;j<cats.length;++j)if(cats[j][0].length==words[i].length){cats[j].push(words[i]);continue out}cats.push([words[i]])}function compareTo(arr){if(arr.length==1)return f+="return str === "+JSON.stringify(arr[0])+";";f+="switch(str){";for(var i=0;i<arr.length;++i)f+="case "+JSON.stringify(arr[i])+":";f+="return true}return false;"}if(cats.length>3){cats.sort(function(a,b){return b.length-a.length});f+="switch(str.length){";for(var i=0;i<cats.length;++i){var cat=cats[i];f+="case "+cat[0].length+":";compareTo(cat)}f+="}"}else{compareTo(words)}return new Function("str",f)}function all(array,predicate){for(var i=array.length;--i>=0;)if(!predicate(array[i]))return false;return true}function Dictionary(){this._values=Object.create(null);this._size=0}Dictionary.prototype={set:function(key,val){if(!this.has(key))++this._size;this._values["$"+key]=val;return this},add:function(key,val){if(this.has(key)){this.get(key).push(val)}else{this.set(key,[val])}return this},get:function(key){return this._values["$"+key]},del:function(key){if(this.has(key)){--this._size;delete this._values["$"+key]}return this},has:function(key){return"$"+key in this._values},each:function(f){for(var i in this._values)f(this._values[i],i.substr(1))},size:function(){return this._size},map:function(f){var ret=[];for(var i in this._values)ret.push(f(this._values[i],i.substr(1)));return ret},toObject:function(){return this._values}};Dictionary.fromObject=function(obj){var dict=new Dictionary;dict._size=merge(dict._values,obj);return dict};/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function DEFNODE(type,props,methods,base){if(arguments.length<4)base=AST_Node;if(!props)props=[];else props=props.split(/\s+/);var self_props=props;if(base&&base.PROPS)props=props.concat(base.PROPS);var code="return function AST_"+type+"(props){ if (props) { ";for(var i=props.length;--i>=0;){code+="this."+props[i]+" = props."+props[i]+";"}var proto=base&&new base;if(proto&&proto.initialize||methods&&methods.initialize)code+="this.initialize();";code+="}}";var ctor=new Function(code)();if(proto){ctor.prototype=proto;ctor.BASE=base}if(base)base.SUBCLASSES.push(ctor);ctor.prototype.CTOR=ctor;ctor.PROPS=props||null;ctor.SELF_PROPS=self_props;ctor.SUBCLASSES=[];if(type){ctor.prototype.TYPE=ctor.TYPE=type}if(methods)for(i in methods)if(methods.hasOwnProperty(i)){if(/^\$/.test(i)){ctor[i.substr(1)]=methods[i]}else{ctor.prototype[i]=methods[i]}}ctor.DEFMETHOD=function(name,method){this.prototype[name]=method};return ctor}var AST_Token=DEFNODE("Token","type value line col pos endline endcol endpos nlb comments_before file",{},null);var AST_Node=DEFNODE("Node","start end",{clone:function(){return new this.CTOR(this)},$documentation:"Base class of all AST nodes",$propdoc:{start:"[AST_Token] The first token of this node",end:"[AST_Token] The last token of this node"},_walk:function(visitor){return visitor._visit(this)},walk:function(visitor){return this._walk(visitor)}},null);AST_Node.warn_function=null;AST_Node.warn=function(txt,props){if(AST_Node.warn_function)AST_Node.warn_function(string_template(txt,props))};var AST_Statement=DEFNODE("Statement",null,{$documentation:"Base class of all statements"});var AST_Debugger=DEFNODE("Debugger",null,{$documentation:"Represents a debugger statement"},AST_Statement);var AST_Directive=DEFNODE("Directive","value scope quote",{$documentation:'Represents a directive, like "use strict";',$propdoc:{value:"[string] The value of this directive as a plain string (it's not an AST_String!)",scope:"[AST_Scope/S] The scope that this directive affects",quote:"[string] the original quote character"}},AST_Statement);var AST_SimpleStatement=DEFNODE("SimpleStatement","body",{$documentation:"A statement consisting of an expression, i.e. a = 1 + 2",$propdoc:{body:"[AST_Node] an expression node (should not be instanceof AST_Statement)"},_walk:function(visitor){return visitor._visit(this,function(){this.body._walk(visitor)})}},AST_Statement);function walk_body(node,visitor){if(node.body instanceof AST_Statement){node.body._walk(visitor)}else node.body.forEach(function(stat){stat._walk(visitor)})}var AST_Block=DEFNODE("Block","body",{$documentation:"A body of statements (usually bracketed)",$propdoc:{body:"[AST_Statement*] an array of statements"},_walk:function(visitor){return visitor._visit(this,function(){walk_body(this,visitor)})}},AST_Statement);var AST_BlockStatement=DEFNODE("BlockStatement",null,{$documentation:"A block statement"},AST_Block);var AST_EmptyStatement=DEFNODE("EmptyStatement",null,{$documentation:"The empty statement (empty block or simply a semicolon)",_walk:function(visitor){return visitor._visit(this)}},AST_Statement);var AST_StatementWithBody=DEFNODE("StatementWithBody","body",{$documentation:"Base class for all statements that contain one nested body: `For`, `ForIn`, `Do`, `While`, `With`",$propdoc:{body:"[AST_Statement] the body; this should always be present, even if it's an AST_EmptyStatement"},_walk:function(visitor){return visitor._visit(this,function(){this.body._walk(visitor)})}},AST_Statement);var AST_LabeledStatement=DEFNODE("LabeledStatement","label",{$documentation:"Statement with a label",$propdoc:{label:"[AST_Label] a label definition"},_walk:function(visitor){return visitor._visit(this,function(){this.label._walk(visitor);this.body._walk(visitor)})}},AST_StatementWithBody);var AST_IterationStatement=DEFNODE("IterationStatement",null,{$documentation:"Internal class.  All loops inherit from it."},AST_StatementWithBody);var AST_DWLoop=DEFNODE("DWLoop","condition",{$documentation:"Base class for do/while statements",$propdoc:{condition:"[AST_Node] the loop condition.  Should not be instanceof AST_Statement"}},AST_IterationStatement);var AST_Do=DEFNODE("Do",null,{$documentation:"A `do` statement",_walk:function(visitor){return visitor._visit(this,function(){this.body._walk(visitor);this.condition._walk(visitor)})}},AST_DWLoop);var AST_While=DEFNODE("While",null,{$documentation:"A `while` statement",_walk:function(visitor){return visitor._visit(this,function(){this.condition._walk(visitor);this.body._walk(visitor)})}},AST_DWLoop);var AST_For=DEFNODE("For","init condition step",{$documentation:"A `for` statement",$propdoc:{init:"[AST_Node?] the `for` initialization code, or null if empty",condition:"[AST_Node?] the `for` termination clause, or null if empty",step:"[AST_Node?] the `for` update clause, or null if empty"},_walk:function(visitor){return visitor._visit(this,function(){if(this.init)this.init._walk(visitor);if(this.condition)this.condition._walk(visitor);if(this.step)this.step._walk(visitor);this.body._walk(visitor)})}},AST_IterationStatement);var AST_ForIn=DEFNODE("ForIn","init name object",{$documentation:"A `for ... in` statement",$propdoc:{init:"[AST_Node] the `for/in` initialization code",name:"[AST_SymbolRef?] the loop variable, only if `init` is AST_Var",object:"[AST_Node] the object that we're looping through"},_walk:function(visitor){return visitor._visit(this,function(){this.init._walk(visitor);this.object._walk(visitor);this.body._walk(visitor)})}},AST_IterationStatement);var AST_With=DEFNODE("With","expression",{$documentation:"A `with` statement",$propdoc:{expression:"[AST_Node] the `with` expression"},_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor);this.body._walk(visitor)})}},AST_StatementWithBody);var AST_Scope=DEFNODE("Scope","directives variables functions uses_with uses_eval parent_scope enclosed cname",{$documentation:"Base class for all statements introducing a lexical scope",$propdoc:{directives:"[string*/S] an array of directives declared in this scope",variables:"[Object/S] a map of name -> SymbolDef for all variables/functions defined in this scope",functions:"[Object/S] like `variables`, but only lists function declarations",uses_with:"[boolean/S] tells whether this scope uses the `with` statement",uses_eval:"[boolean/S] tells whether this scope contains a direct call to the global `eval`",parent_scope:"[AST_Scope?/S] link to the parent scope",enclosed:"[SymbolDef*/S] a list of all symbol definitions that are accessed from this scope or any subscopes",cname:"[integer/S] current index for mangling variables (used internally by the mangler)"}},AST_Block);var AST_Toplevel=DEFNODE("Toplevel","globals",{$documentation:"The toplevel scope",$propdoc:{globals:"[Object/S] a map of name -> SymbolDef for all undeclared names"},wrap_enclose:function(arg_parameter_pairs){var self=this;var args=[];var parameters=[];arg_parameter_pairs.forEach(function(pair){var splitAt=pair.lastIndexOf(":");args.push(pair.substr(0,splitAt));parameters.push(pair.substr(splitAt+1))});var wrapped_tl="(function("+parameters.join(",")+"){ '$ORIG'; })("+args.join(",")+")";wrapped_tl=parse(wrapped_tl);wrapped_tl=wrapped_tl.transform(new TreeTransformer(function before(node){if(node instanceof AST_Directive&&node.value=="$ORIG"){return MAP.splice(self.body)}}));return wrapped_tl},wrap_commonjs:function(name,export_all){var self=this;var to_export=[];if(export_all){self.figure_out_scope();self.walk(new TreeWalker(function(node){if(node instanceof AST_SymbolDeclaration&&node.definition().global){if(!find_if(function(n){return n.name==node.name},to_export))to_export.push(node)}}))}var wrapped_tl="(function(exports, global){ global['"+name+"'] = exports; '$ORIG'; '$EXPORTS'; }({}, (function(){return this}())))";wrapped_tl=parse(wrapped_tl);wrapped_tl=wrapped_tl.transform(new TreeTransformer(function before(node){if(node instanceof AST_SimpleStatement){node=node.body;if(node instanceof AST_String)switch(node.getValue()){case"$ORIG":return MAP.splice(self.body);case"$EXPORTS":var body=[];to_export.forEach(function(sym){body.push(new AST_SimpleStatement({body:new AST_Assign({left:new AST_Sub({expression:new AST_SymbolRef({name:"exports"}),property:new AST_String({value:sym.name})}),operator:"=",right:new AST_SymbolRef(sym)})}))});return MAP.splice(body)}}}));return wrapped_tl}},AST_Scope);var AST_Lambda=DEFNODE("Lambda","name argnames uses_arguments",{$documentation:"Base class for functions",$propdoc:{name:"[AST_SymbolDeclaration?] the name of this function",argnames:"[AST_SymbolFunarg*] array of function arguments",uses_arguments:"[boolean/S] tells whether this function accesses the arguments array"},_walk:function(visitor){return visitor._visit(this,function(){if(this.name)this.name._walk(visitor);this.argnames.forEach(function(arg){arg._walk(visitor)});walk_body(this,visitor)})}},AST_Scope);var AST_Accessor=DEFNODE("Accessor",null,{$documentation:"A setter/getter function.  The `name` property is always null."},AST_Lambda);var AST_Function=DEFNODE("Function",null,{$documentation:"A function expression"},AST_Lambda);var AST_Defun=DEFNODE("Defun",null,{$documentation:"A function definition"},AST_Lambda);var AST_Jump=DEFNODE("Jump",null,{$documentation:"Base class for “jumps” (for now that's `return`, `throw`, `break` and `continue`)"},AST_Statement);var AST_Exit=DEFNODE("Exit","value",{$documentation:"Base class for “exits” (`return` and `throw`)",$propdoc:{value:"[AST_Node?] the value returned or thrown by this statement; could be null for AST_Return"},_walk:function(visitor){return visitor._visit(this,this.value&&function(){this.value._walk(visitor)})}},AST_Jump);var AST_Return=DEFNODE("Return",null,{$documentation:"A `return` statement"},AST_Exit);var AST_Throw=DEFNODE("Throw",null,{$documentation:"A `throw` statement"},AST_Exit);var AST_LoopControl=DEFNODE("LoopControl","label",{$documentation:"Base class for loop control statements (`break` and `continue`)",$propdoc:{label:"[AST_LabelRef?] the label, or null if none"},_walk:function(visitor){return visitor._visit(this,this.label&&function(){this.label._walk(visitor)})}},AST_Jump);var AST_Break=DEFNODE("Break",null,{$documentation:"A `break` statement"},AST_LoopControl);var AST_Continue=DEFNODE("Continue",null,{$documentation:"A `continue` statement"},AST_LoopControl);var AST_If=DEFNODE("If","condition alternative",{$documentation:"A `if` statement",$propdoc:{condition:"[AST_Node] the `if` condition",alternative:"[AST_Statement?] the `else` part, or null if not present"},_walk:function(visitor){return visitor._visit(this,function(){this.condition._walk(visitor);this.body._walk(visitor);if(this.alternative)this.alternative._walk(visitor)})}},AST_StatementWithBody);var AST_Switch=DEFNODE("Switch","expression",{$documentation:"A `switch` statement",$propdoc:{expression:"[AST_Node] the `switch` “discriminant”"},_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor);walk_body(this,visitor)})}},AST_Block);var AST_SwitchBranch=DEFNODE("SwitchBranch",null,{$documentation:"Base class for `switch` branches"},AST_Block);var AST_Default=DEFNODE("Default",null,{$documentation:"A `default` switch branch"},AST_SwitchBranch);var AST_Case=DEFNODE("Case","expression",{$documentation:"A `case` switch branch",$propdoc:{expression:"[AST_Node] the `case` expression"},_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor);walk_body(this,visitor)})}},AST_SwitchBranch);var AST_Try=DEFNODE("Try","bcatch bfinally",{$documentation:"A `try` statement",$propdoc:{bcatch:"[AST_Catch?] the catch block, or null if not present",bfinally:"[AST_Finally?] the finally block, or null if not present"},_walk:function(visitor){return visitor._visit(this,function(){walk_body(this,visitor);if(this.bcatch)this.bcatch._walk(visitor);if(this.bfinally)this.bfinally._walk(visitor)})}},AST_Block);var AST_Catch=DEFNODE("Catch","argname",{$documentation:"A `catch` node; only makes sense as part of a `try` statement",$propdoc:{argname:"[AST_SymbolCatch] symbol for the exception"},_walk:function(visitor){return visitor._visit(this,function(){this.argname._walk(visitor);walk_body(this,visitor)})}},AST_Block);var AST_Finally=DEFNODE("Finally",null,{$documentation:"A `finally` node; only makes sense as part of a `try` statement"},AST_Block);var AST_Definitions=DEFNODE("Definitions","definitions",{$documentation:"Base class for `var` or `const` nodes (variable declarations/initializations)",$propdoc:{definitions:"[AST_VarDef*] array of variable definitions"},_walk:function(visitor){return visitor._visit(this,function(){this.definitions.forEach(function(def){def._walk(visitor)})})}},AST_Statement);var AST_Var=DEFNODE("Var",null,{$documentation:"A `var` statement"},AST_Definitions);var AST_Const=DEFNODE("Const",null,{$documentation:"A `const` statement"},AST_Definitions);var AST_VarDef=DEFNODE("VarDef","name value",{$documentation:"A variable declaration; only appears in a AST_Definitions node",$propdoc:{name:"[AST_SymbolVar|AST_SymbolConst] name of the variable",value:"[AST_Node?] initializer, or null of there's no initializer"},_walk:function(visitor){return visitor._visit(this,function(){this.name._walk(visitor);if(this.value)this.value._walk(visitor)})}});var AST_Call=DEFNODE("Call","expression args",{$documentation:"A function call expression",$propdoc:{expression:"[AST_Node] expression to invoke as function",args:"[AST_Node*] array of arguments"},_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor);this.args.forEach(function(arg){arg._walk(visitor)})})}});var AST_New=DEFNODE("New",null,{$documentation:"An object instantiation.  Derives from a function call since it has exactly the same properties"},AST_Call);var AST_Seq=DEFNODE("Seq","car cdr",{$documentation:"A sequence expression (two comma-separated expressions)",$propdoc:{car:"[AST_Node] first element in sequence",cdr:"[AST_Node] second element in sequence"},$cons:function(x,y){var seq=new AST_Seq(x);seq.car=x;seq.cdr=y;return seq},$from_array:function(array){if(array.length==0)return null;if(array.length==1)return array[0].clone();var list=null;for(var i=array.length;--i>=0;){list=AST_Seq.cons(array[i],list)}var p=list;while(p){if(p.cdr&&!p.cdr.cdr){p.cdr=p.cdr.car;break}p=p.cdr}return list},to_array:function(){var p=this,a=[];while(p){a.push(p.car);if(p.cdr&&!(p.cdr instanceof AST_Seq)){a.push(p.cdr);break}p=p.cdr}return a},add:function(node){var p=this;while(p){if(!(p.cdr instanceof AST_Seq)){var cell=AST_Seq.cons(p.cdr,node);return p.cdr=cell}p=p.cdr}},_walk:function(visitor){return visitor._visit(this,function(){this.car._walk(visitor);if(this.cdr)this.cdr._walk(visitor)})}});var AST_PropAccess=DEFNODE("PropAccess","expression property",{$documentation:'Base class for property access expressions, i.e. `a.foo` or `a["foo"]`',$propdoc:{expression:"[AST_Node] the “container” expression",property:"[AST_Node|string] the property to access.  For AST_Dot this is always a plain string, while for AST_Sub it's an arbitrary AST_Node"}});var AST_Dot=DEFNODE("Dot",null,{$documentation:"A dotted property access expression",_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor)})}},AST_PropAccess);var AST_Sub=DEFNODE("Sub",null,{$documentation:'Index-style property access, i.e. `a["foo"]`',_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor);this.property._walk(visitor)})}},AST_PropAccess);var AST_Unary=DEFNODE("Unary","operator expression",{$documentation:"Base class for unary expressions",$propdoc:{operator:"[string] the operator",expression:"[AST_Node] expression that this unary operator applies to"},_walk:function(visitor){return visitor._visit(this,function(){this.expression._walk(visitor)})}});var AST_UnaryPrefix=DEFNODE("UnaryPrefix",null,{$documentation:"Unary prefix expression, i.e. `typeof i` or `++i`"},AST_Unary);var AST_UnaryPostfix=DEFNODE("UnaryPostfix",null,{$documentation:"Unary postfix expression, i.e. `i++`"},AST_Unary);var AST_Binary=DEFNODE("Binary","left operator right",{$documentation:"Binary expression, i.e. `a + b`",$propdoc:{left:"[AST_Node] left-hand side expression",operator:"[string] the operator",right:"[AST_Node] right-hand side expression"},_walk:function(visitor){return visitor._visit(this,function(){this.left._walk(visitor);this.right._walk(visitor)})}});var AST_Conditional=DEFNODE("Conditional","condition consequent alternative",{$documentation:"Conditional expression using the ternary operator, i.e. `a ? b : c`",$propdoc:{condition:"[AST_Node]",consequent:"[AST_Node]",alternative:"[AST_Node]"},_walk:function(visitor){return visitor._visit(this,function(){this.condition._walk(visitor);this.consequent._walk(visitor);this.alternative._walk(visitor)})}});var AST_Assign=DEFNODE("Assign",null,{$documentation:"An assignment expression — `a = b + 5`"},AST_Binary);var AST_Array=DEFNODE("Array","elements",{$documentation:"An array literal",$propdoc:{elements:"[AST_Node*] array of elements"},_walk:function(visitor){return visitor._visit(this,function(){this.elements.forEach(function(el){el._walk(visitor)})})}});var AST_Object=DEFNODE("Object","properties",{$documentation:"An object literal",$propdoc:{properties:"[AST_ObjectProperty*] array of properties"},_walk:function(visitor){return visitor._visit(this,function(){this.properties.forEach(function(prop){prop._walk(visitor)})})}});var AST_ObjectProperty=DEFNODE("ObjectProperty","key value",{$documentation:"Base class for literal object properties",$propdoc:{key:"[string] the property name converted to a string for ObjectKeyVal.  For setters and getters this is an arbitrary AST_Node.",value:"[AST_Node] property value.  For setters and getters this is an AST_Function."},_walk:function(visitor){return visitor._visit(this,function(){this.value._walk(visitor)})}});var AST_ObjectKeyVal=DEFNODE("ObjectKeyVal","quote",{$documentation:"A key: value object property",$propdoc:{quote:"[string] the original quote character"}},AST_ObjectProperty);var AST_ObjectSetter=DEFNODE("ObjectSetter",null,{$documentation:"An object setter property"},AST_ObjectProperty);var AST_ObjectGetter=DEFNODE("ObjectGetter",null,{$documentation:"An object getter property"},AST_ObjectProperty);var AST_Symbol=DEFNODE("Symbol","scope name thedef",{$propdoc:{name:"[string] name of this symbol",scope:"[AST_Scope/S] the current scope (not necessarily the definition scope)",thedef:"[SymbolDef/S] the definition of this symbol"},$documentation:"Base class for all symbols"});var AST_SymbolAccessor=DEFNODE("SymbolAccessor",null,{$documentation:"The name of a property accessor (setter/getter function)"},AST_Symbol);var AST_SymbolDeclaration=DEFNODE("SymbolDeclaration","init",{$documentation:"A declaration symbol (symbol in var/const, function name or argument, symbol in catch)",$propdoc:{init:"[AST_Node*/S] array of initializers for this declaration."}},AST_Symbol);var AST_SymbolVar=DEFNODE("SymbolVar",null,{$documentation:"Symbol defining a variable"},AST_SymbolDeclaration);var AST_SymbolConst=DEFNODE("SymbolConst",null,{$documentation:"A constant declaration"},AST_SymbolDeclaration);var AST_SymbolFunarg=DEFNODE("SymbolFunarg",null,{$documentation:"Symbol naming a function argument"},AST_SymbolVar);var AST_SymbolDefun=DEFNODE("SymbolDefun",null,{$documentation:"Symbol defining a function"},AST_SymbolDeclaration);var AST_SymbolLambda=DEFNODE("SymbolLambda",null,{$documentation:"Symbol naming a function expression"},AST_SymbolDeclaration);var AST_SymbolCatch=DEFNODE("SymbolCatch",null,{$documentation:"Symbol naming the exception in catch"},AST_SymbolDeclaration);var AST_Label=DEFNODE("Label","references",{$documentation:"Symbol naming a label (declaration)",$propdoc:{references:"[AST_LoopControl*] a list of nodes referring to this label"},initialize:function(){this.references=[];this.thedef=this}},AST_Symbol);var AST_SymbolRef=DEFNODE("SymbolRef",null,{$documentation:"Reference to some symbol (not definition/declaration)"},AST_Symbol);var AST_LabelRef=DEFNODE("LabelRef",null,{$documentation:"Reference to a label symbol"},AST_Symbol);var AST_This=DEFNODE("This",null,{$documentation:"The `this` symbol"},AST_Symbol);var AST_Constant=DEFNODE("Constant",null,{$documentation:"Base class for all constants",getValue:function(){return this.value}});var AST_String=DEFNODE("String","value quote",{$documentation:"A string literal",$propdoc:{value:"[string] the contents of this string",quote:"[string] the original quote character"}},AST_Constant);var AST_Number=DEFNODE("Number","value",{$documentation:"A number literal",$propdoc:{value:"[number] the numeric value"}},AST_Constant);var AST_RegExp=DEFNODE("RegExp","value",{$documentation:"A regexp literal",$propdoc:{value:"[RegExp] the actual regexp"}},AST_Constant);var AST_Atom=DEFNODE("Atom",null,{$documentation:"Base class for atoms"},AST_Constant);var AST_Null=DEFNODE("Null",null,{$documentation:"The `null` atom",value:null},AST_Atom);var AST_NaN=DEFNODE("NaN",null,{$documentation:"The impossible value",value:0/0},AST_Atom);var AST_Undefined=DEFNODE("Undefined",null,{$documentation:"The `undefined` value",value:function(){}()},AST_Atom);var AST_Hole=DEFNODE("Hole",null,{$documentation:"A hole in an array",value:function(){}()},AST_Atom);var AST_Infinity=DEFNODE("Infinity",null,{$documentation:"The `Infinity` value",value:1/0},AST_Atom);var AST_Boolean=DEFNODE("Boolean",null,{$documentation:"Base class for booleans"},AST_Atom);var AST_False=DEFNODE("False",null,{$documentation:"The `false` atom",value:false},AST_Boolean);var AST_True=DEFNODE("True",null,{$documentation:"The `true` atom",value:true},AST_Boolean);function TreeWalker(callback){this.visit=callback;this.stack=[]}TreeWalker.prototype={_visit:function(node,descend){this.stack.push(node);var ret=this.visit(node,descend?function(){descend.call(node)}:noop);if(!ret&&descend){descend.call(node)}this.stack.pop();return ret},parent:function(n){return this.stack[this.stack.length-2-(n||0)]},push:function(node){this.stack.push(node)},pop:function(){return this.stack.pop()},self:function(){return this.stack[this.stack.length-1]},find_parent:function(type){var stack=this.stack;for(var i=stack.length;--i>=0;){var x=stack[i];if(x instanceof type)return x}},has_directive:function(type){return this.find_parent(AST_Scope).has_directive(type)},in_boolean_context:function(){var stack=this.stack;var i=stack.length,self=stack[--i];while(i>0){var p=stack[--i];if(p instanceof AST_If&&p.condition===self||p instanceof AST_Conditional&&p.condition===self||p instanceof AST_DWLoop&&p.condition===self||p instanceof AST_For&&p.condition===self||p instanceof AST_UnaryPrefix&&p.operator=="!"&&p.expression===self){return true}if(!(p instanceof AST_Binary&&(p.operator=="&&"||p.operator=="||")))return false;self=p}},loopcontrol_target:function(label){var stack=this.stack;if(label)for(var i=stack.length;--i>=0;){var x=stack[i];if(x instanceof AST_LabeledStatement&&x.label.name==label.name){return x.body}}else for(var i=stack.length;--i>=0;){var x=stack[i];if(x instanceof AST_Switch||x instanceof AST_IterationStatement)return x}}};/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>
    Parser based on parse-js (http://marijn.haverbeke.nl/parse-js/).

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";var KEYWORDS="break case catch const continue debugger default delete do else finally for function if in instanceof new return switch throw try typeof var void while with";var KEYWORDS_ATOM="false null true";var RESERVED_WORDS="abstract boolean byte char class double enum export extends final float goto implements import int interface long native package private protected public short static super synchronized this throws transient volatile yield"+" "+KEYWORDS_ATOM+" "+KEYWORDS;var KEYWORDS_BEFORE_EXPRESSION="return new delete throw else case";KEYWORDS=makePredicate(KEYWORDS);RESERVED_WORDS=makePredicate(RESERVED_WORDS);KEYWORDS_BEFORE_EXPRESSION=makePredicate(KEYWORDS_BEFORE_EXPRESSION);KEYWORDS_ATOM=makePredicate(KEYWORDS_ATOM);var OPERATOR_CHARS=makePredicate(characters("+-*&%=<>!?|~^"));var RE_HEX_NUMBER=/^0x[0-9a-f]+$/i;var RE_OCT_NUMBER=/^0[0-7]+$/;var RE_DEC_NUMBER=/^\d*\.?\d*(?:e[+-]?\d*(?:\d\.?|\.?\d)\d*)?$/i;var OPERATORS=makePredicate(["in","instanceof","typeof","new","void","delete","++","--","+","-","!","~","&","|","^","*","/","%",">>","<<",">>>","<",">","<=",">=","==","===","!=","!==","?","=","+=","-=","/=","*=","%=",">>=","<<=",">>>=","|=","^=","&=","&&","||"]);var WHITESPACE_CHARS=makePredicate(characters("  \n\r	\f​᠎             　\ufeff"));var PUNC_BEFORE_EXPRESSION=makePredicate(characters("[{(,.;:"));var PUNC_CHARS=makePredicate(characters("[]{}(),;:"));var REGEXP_MODIFIERS=makePredicate(characters("gmsiy"));var UNICODE={letter:new RegExp("[\\u0041-\\u005A\\u0061-\\u007A\\u00AA\\u00B5\\u00BA\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02C1\\u02C6-\\u02D1\\u02E0-\\u02E4\\u02EC\\u02EE\\u0370-\\u0374\\u0376\\u0377\\u037A-\\u037D\\u037F\\u0386\\u0388-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u03F5\\u03F7-\\u0481\\u048A-\\u052F\\u0531-\\u0556\\u0559\\u0561-\\u0587\\u05D0-\\u05EA\\u05F0-\\u05F2\\u0620-\\u064A\\u066E\\u066F\\u0671-\\u06D3\\u06D5\\u06E5\\u06E6\\u06EE\\u06EF\\u06FA-\\u06FC\\u06FF\\u0710\\u0712-\\u072F\\u074D-\\u07A5\\u07B1\\u07CA-\\u07EA\\u07F4\\u07F5\\u07FA\\u0800-\\u0815\\u081A\\u0824\\u0828\\u0840-\\u0858\\u08A0-\\u08B2\\u0904-\\u0939\\u093D\\u0950\\u0958-\\u0961\\u0971-\\u0980\\u0985-\\u098C\\u098F\\u0990\\u0993-\\u09A8\\u09AA-\\u09B0\\u09B2\\u09B6-\\u09B9\\u09BD\\u09CE\\u09DC\\u09DD\\u09DF-\\u09E1\\u09F0\\u09F1\\u0A05-\\u0A0A\\u0A0F\\u0A10\\u0A13-\\u0A28\\u0A2A-\\u0A30\\u0A32\\u0A33\\u0A35\\u0A36\\u0A38\\u0A39\\u0A59-\\u0A5C\\u0A5E\\u0A72-\\u0A74\\u0A85-\\u0A8D\\u0A8F-\\u0A91\\u0A93-\\u0AA8\\u0AAA-\\u0AB0\\u0AB2\\u0AB3\\u0AB5-\\u0AB9\\u0ABD\\u0AD0\\u0AE0\\u0AE1\\u0B05-\\u0B0C\\u0B0F\\u0B10\\u0B13-\\u0B28\\u0B2A-\\u0B30\\u0B32\\u0B33\\u0B35-\\u0B39\\u0B3D\\u0B5C\\u0B5D\\u0B5F-\\u0B61\\u0B71\\u0B83\\u0B85-\\u0B8A\\u0B8E-\\u0B90\\u0B92-\\u0B95\\u0B99\\u0B9A\\u0B9C\\u0B9E\\u0B9F\\u0BA3\\u0BA4\\u0BA8-\\u0BAA\\u0BAE-\\u0BB9\\u0BD0\\u0C05-\\u0C0C\\u0C0E-\\u0C10\\u0C12-\\u0C28\\u0C2A-\\u0C39\\u0C3D\\u0C58\\u0C59\\u0C60\\u0C61\\u0C85-\\u0C8C\\u0C8E-\\u0C90\\u0C92-\\u0CA8\\u0CAA-\\u0CB3\\u0CB5-\\u0CB9\\u0CBD\\u0CDE\\u0CE0\\u0CE1\\u0CF1\\u0CF2\\u0D05-\\u0D0C\\u0D0E-\\u0D10\\u0D12-\\u0D3A\\u0D3D\\u0D4E\\u0D60\\u0D61\\u0D7A-\\u0D7F\\u0D85-\\u0D96\\u0D9A-\\u0DB1\\u0DB3-\\u0DBB\\u0DBD\\u0DC0-\\u0DC6\\u0E01-\\u0E30\\u0E32\\u0E33\\u0E40-\\u0E46\\u0E81\\u0E82\\u0E84\\u0E87\\u0E88\\u0E8A\\u0E8D\\u0E94-\\u0E97\\u0E99-\\u0E9F\\u0EA1-\\u0EA3\\u0EA5\\u0EA7\\u0EAA\\u0EAB\\u0EAD-\\u0EB0\\u0EB2\\u0EB3\\u0EBD\\u0EC0-\\u0EC4\\u0EC6\\u0EDC-\\u0EDF\\u0F00\\u0F40-\\u0F47\\u0F49-\\u0F6C\\u0F88-\\u0F8C\\u1000-\\u102A\\u103F\\u1050-\\u1055\\u105A-\\u105D\\u1061\\u1065\\u1066\\u106E-\\u1070\\u1075-\\u1081\\u108E\\u10A0-\\u10C5\\u10C7\\u10CD\\u10D0-\\u10FA\\u10FC-\\u1248\\u124A-\\u124D\\u1250-\\u1256\\u1258\\u125A-\\u125D\\u1260-\\u1288\\u128A-\\u128D\\u1290-\\u12B0\\u12B2-\\u12B5\\u12B8-\\u12BE\\u12C0\\u12C2-\\u12C5\\u12C8-\\u12D6\\u12D8-\\u1310\\u1312-\\u1315\\u1318-\\u135A\\u1380-\\u138F\\u13A0-\\u13F4\\u1401-\\u166C\\u166F-\\u167F\\u1681-\\u169A\\u16A0-\\u16EA\\u16EE-\\u16F8\\u1700-\\u170C\\u170E-\\u1711\\u1720-\\u1731\\u1740-\\u1751\\u1760-\\u176C\\u176E-\\u1770\\u1780-\\u17B3\\u17D7\\u17DC\\u1820-\\u1877\\u1880-\\u18A8\\u18AA\\u18B0-\\u18F5\\u1900-\\u191E\\u1950-\\u196D\\u1970-\\u1974\\u1980-\\u19AB\\u19C1-\\u19C7\\u1A00-\\u1A16\\u1A20-\\u1A54\\u1AA7\\u1B05-\\u1B33\\u1B45-\\u1B4B\\u1B83-\\u1BA0\\u1BAE\\u1BAF\\u1BBA-\\u1BE5\\u1C00-\\u1C23\\u1C4D-\\u1C4F\\u1C5A-\\u1C7D\\u1CE9-\\u1CEC\\u1CEE-\\u1CF1\\u1CF5\\u1CF6\\u1D00-\\u1DBF\\u1E00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-\\u1FB4\\u1FB6-\\u1FBC\\u1FBE\\u1FC2-\\u1FC4\\u1FC6-\\u1FCC\\u1FD0-\\u1FD3\\u1FD6-\\u1FDB\\u1FE0-\\u1FEC\\u1FF2-\\u1FF4\\u1FF6-\\u1FFC\\u2071\\u207F\\u2090-\\u209C\\u2102\\u2107\\u210A-\\u2113\\u2115\\u2119-\\u211D\\u2124\\u2126\\u2128\\u212A-\\u212D\\u212F-\\u2139\\u213C-\\u213F\\u2145-\\u2149\\u214E\\u2160-\\u2188\\u2C00-\\u2C2E\\u2C30-\\u2C5E\\u2C60-\\u2CE4\\u2CEB-\\u2CEE\\u2CF2\\u2CF3\\u2D00-\\u2D25\\u2D27\\u2D2D\\u2D30-\\u2D67\\u2D6F\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE\\u2E2F\\u3005-\\u3007\\u3021-\\u3029\\u3031-\\u3035\\u3038-\\u303C\\u3041-\\u3096\\u309D-\\u309F\\u30A1-\\u30FA\\u30FC-\\u30FF\\u3105-\\u312D\\u3131-\\u318E\\u31A0-\\u31BA\\u31F0-\\u31FF\\u3400-\\u4DB5\\u4E00-\\u9FCC\\uA000-\\uA48C\\uA4D0-\\uA4FD\\uA500-\\uA60C\\uA610-\\uA61F\\uA62A\\uA62B\\uA640-\\uA66E\\uA67F-\\uA69D\\uA6A0-\\uA6EF\\uA717-\\uA71F\\uA722-\\uA788\\uA78B-\\uA78E\\uA790-\\uA7AD\\uA7B0\\uA7B1\\uA7F7-\\uA801\\uA803-\\uA805\\uA807-\\uA80A\\uA80C-\\uA822\\uA840-\\uA873\\uA882-\\uA8B3\\uA8F2-\\uA8F7\\uA8FB\\uA90A-\\uA925\\uA930-\\uA946\\uA960-\\uA97C\\uA984-\\uA9B2\\uA9CF\\uA9E0-\\uA9E4\\uA9E6-\\uA9EF\\uA9FA-\\uA9FE\\uAA00-\\uAA28\\uAA40-\\uAA42\\uAA44-\\uAA4B\\uAA60-\\uAA76\\uAA7A\\uAA7E-\\uAAAF\\uAAB1\\uAAB5\\uAAB6\\uAAB9-\\uAABD\\uAAC0\\uAAC2\\uAADB-\\uAADD\\uAAE0-\\uAAEA\\uAAF2-\\uAAF4\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uAB30-\\uAB5A\\uAB5C-\\uAB5F\\uAB64\\uAB65\\uABC0-\\uABE2\\uAC00-\\uD7A3\\uD7B0-\\uD7C6\\uD7CB-\\uD7FB\\uF900-\\uFA6D\\uFA70-\\uFAD9\\uFB00-\\uFB06\\uFB13-\\uFB17\\uFB1D\\uFB1F-\\uFB28\\uFB2A-\\uFB36\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-\\uFBB1\\uFBD3-\\uFD3D\\uFD50-\\uFD8F\\uFD92-\\uFDC7\\uFDF0-\\uFDFB\\uFE70-\\uFE74\\uFE76-\\uFEFC\\uFF21-\\uFF3A\\uFF41-\\uFF5A\\uFF66-\\uFFBE\\uFFC2-\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7\\uFFDA-\\uFFDC]"),digit:new RegExp("[\\u0030-\\u0039\\u0660-\\u0669\\u06F0-\\u06F9\\u07C0-\\u07C9\\u0966-\\u096F\\u09E6-\\u09EF\\u0A66-\\u0A6F\\u0AE6-\\u0AEF\\u0B66-\\u0B6F\\u0BE6-\\u0BEF\\u0C66-\\u0C6F\\u0CE6-\\u0CEF\\u0D66-\\u0D6F\\u0DE6-\\u0DEF\\u0E50-\\u0E59\\u0ED0-\\u0ED9\\u0F20-\\u0F29\\u1040-\\u1049\\u1090-\\u1099\\u17E0-\\u17E9\\u1810-\\u1819\\u1946-\\u194F\\u19D0-\\u19D9\\u1A80-\\u1A89\\u1A90-\\u1A99\\u1B50-\\u1B59\\u1BB0-\\u1BB9\\u1C40-\\u1C49\\u1C50-\\u1C59\\uA620-\\uA629\\uA8D0-\\uA8D9\\uA900-\\uA909\\uA9D0-\\uA9D9\\uA9F0-\\uA9F9\\uAA50-\\uAA59\\uABF0-\\uABF9\\uFF10-\\uFF19]"),non_spacing_mark:new RegExp("[\\u0300-\\u036F\\u0483-\\u0487\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7\\u0610-\\u061A\\u064B-\\u065E\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07A6-\\u07B0\\u07EB-\\u07F3\\u0816-\\u0819\\u081B-\\u0823\\u0825-\\u0827\\u0829-\\u082D\\u0900-\\u0902\\u093C\\u0941-\\u0948\\u094D\\u0951-\\u0955\\u0962\\u0963\\u0981\\u09BC\\u09C1-\\u09C4\\u09CD\\u09E2\\u09E3\\u0A01\\u0A02\\u0A3C\\u0A41\\u0A42\\u0A47\\u0A48\\u0A4B-\\u0A4D\\u0A51\\u0A70\\u0A71\\u0A75\\u0A81\\u0A82\\u0ABC\\u0AC1-\\u0AC5\\u0AC7\\u0AC8\\u0ACD\\u0AE2\\u0AE3\\u0B01\\u0B3C\\u0B3F\\u0B41-\\u0B44\\u0B4D\\u0B56\\u0B62\\u0B63\\u0B82\\u0BC0\\u0BCD\\u0C3E-\\u0C40\\u0C46-\\u0C48\\u0C4A-\\u0C4D\\u0C55\\u0C56\\u0C62\\u0C63\\u0CBC\\u0CBF\\u0CC6\\u0CCC\\u0CCD\\u0CE2\\u0CE3\\u0D41-\\u0D44\\u0D4D\\u0D62\\u0D63\\u0DCA\\u0DD2-\\u0DD4\\u0DD6\\u0E31\\u0E34-\\u0E3A\\u0E47-\\u0E4E\\u0EB1\\u0EB4-\\u0EB9\\u0EBB\\u0EBC\\u0EC8-\\u0ECD\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F71-\\u0F7E\\u0F80-\\u0F84\\u0F86\\u0F87\\u0F90-\\u0F97\\u0F99-\\u0FBC\\u0FC6\\u102D-\\u1030\\u1032-\\u1037\\u1039\\u103A\\u103D\\u103E\\u1058\\u1059\\u105E-\\u1060\\u1071-\\u1074\\u1082\\u1085\\u1086\\u108D\\u109D\\u135F\\u1712-\\u1714\\u1732-\\u1734\\u1752\\u1753\\u1772\\u1773\\u17B7-\\u17BD\\u17C6\\u17C9-\\u17D3\\u17DD\\u180B-\\u180D\\u18A9\\u1920-\\u1922\\u1927\\u1928\\u1932\\u1939-\\u193B\\u1A17\\u1A18\\u1A56\\u1A58-\\u1A5E\\u1A60\\u1A62\\u1A65-\\u1A6C\\u1A73-\\u1A7C\\u1A7F\\u1B00-\\u1B03\\u1B34\\u1B36-\\u1B3A\\u1B3C\\u1B42\\u1B6B-\\u1B73\\u1B80\\u1B81\\u1BA2-\\u1BA5\\u1BA8\\u1BA9\\u1C2C-\\u1C33\\u1C36\\u1C37\\u1CD0-\\u1CD2\\u1CD4-\\u1CE0\\u1CE2-\\u1CE8\\u1CED\\u1DC0-\\u1DE6\\u1DFD-\\u1DFF\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20F0\\u2CEF-\\u2CF1\\u2DE0-\\u2DFF\\u302A-\\u302F\\u3099\\u309A\\uA66F\\uA67C\\uA67D\\uA6F0\\uA6F1\\uA802\\uA806\\uA80B\\uA825\\uA826\\uA8C4\\uA8E0-\\uA8F1\\uA926-\\uA92D\\uA947-\\uA951\\uA980-\\uA982\\uA9B3\\uA9B6-\\uA9B9\\uA9BC\\uAA29-\\uAA2E\\uAA31\\uAA32\\uAA35\\uAA36\\uAA43\\uAA4C\\uAAB0\\uAAB2-\\uAAB4\\uAAB7\\uAAB8\\uAABE\\uAABF\\uAAC1\\uABE5\\uABE8\\uABED\\uFB1E\\uFE00-\\uFE0F\\uFE20-\\uFE26]"),space_combining_mark:new RegExp("[\\u0903\\u093E-\\u0940\\u0949-\\u094C\\u094E\\u0982\\u0983\\u09BE-\\u09C0\\u09C7\\u09C8\\u09CB\\u09CC\\u09D7\\u0A03\\u0A3E-\\u0A40\\u0A83\\u0ABE-\\u0AC0\\u0AC9\\u0ACB\\u0ACC\\u0B02\\u0B03\\u0B3E\\u0B40\\u0B47\\u0B48\\u0B4B\\u0B4C\\u0B57\\u0BBE\\u0BBF\\u0BC1\\u0BC2\\u0BC6-\\u0BC8\\u0BCA-\\u0BCC\\u0BD7\\u0C01-\\u0C03\\u0C41-\\u0C44\\u0C82\\u0C83\\u0CBE\\u0CC0-\\u0CC4\\u0CC7\\u0CC8\\u0CCA\\u0CCB\\u0CD5\\u0CD6\\u0D02\\u0D03\\u0D3E-\\u0D40\\u0D46-\\u0D48\\u0D4A-\\u0D4C\\u0D57\\u0D82\\u0D83\\u0DCF-\\u0DD1\\u0DD8-\\u0DDF\\u0DF2\\u0DF3\\u0F3E\\u0F3F\\u0F7F\\u102B\\u102C\\u1031\\u1038\\u103B\\u103C\\u1056\\u1057\\u1062-\\u1064\\u1067-\\u106D\\u1083\\u1084\\u1087-\\u108C\\u108F\\u109A-\\u109C\\u17B6\\u17BE-\\u17C5\\u17C7\\u17C8\\u1923-\\u1926\\u1929-\\u192B\\u1930\\u1931\\u1933-\\u1938\\u19B0-\\u19C0\\u19C8\\u19C9\\u1A19-\\u1A1B\\u1A55\\u1A57\\u1A61\\u1A63\\u1A64\\u1A6D-\\u1A72\\u1B04\\u1B35\\u1B3B\\u1B3D-\\u1B41\\u1B43\\u1B44\\u1B82\\u1BA1\\u1BA6\\u1BA7\\u1BAA\\u1C24-\\u1C2B\\u1C34\\u1C35\\u1CE1\\u1CF2\\uA823\\uA824\\uA827\\uA880\\uA881\\uA8B4-\\uA8C3\\uA952\\uA953\\uA983\\uA9B4\\uA9B5\\uA9BA\\uA9BB\\uA9BD-\\uA9C0\\uAA2F\\uAA30\\uAA33\\uAA34\\uAA4D\\uAA7B\\uABE3\\uABE4\\uABE6\\uABE7\\uABE9\\uABEA\\uABEC]"),connector_punctuation:new RegExp("[\\u005F\\u203F\\u2040\\u2054\\uFE33\\uFE34\\uFE4D-\\uFE4F\\uFF3F]")};function is_letter(code){return code>=97&&code<=122||code>=65&&code<=90||code>=170&&UNICODE.letter.test(String.fromCharCode(code))}function is_digit(code){return code>=48&&code<=57}function is_alphanumeric_char(code){return is_digit(code)||is_letter(code)}function is_unicode_digit(code){return UNICODE.digit.test(String.fromCharCode(code))}function is_unicode_combining_mark(ch){return UNICODE.non_spacing_mark.test(ch)||UNICODE.space_combining_mark.test(ch)}function is_unicode_connector_punctuation(ch){return UNICODE.connector_punctuation.test(ch)}function is_identifier(name){return!RESERVED_WORDS(name)&&/^[a-z_$][a-z0-9_$]*$/i.test(name)}function is_identifier_start(code){return code==36||code==95||is_letter(code)}function is_identifier_char(ch){var code=ch.charCodeAt(0);return is_identifier_start(code)||is_digit(code)||code==8204||code==8205||is_unicode_combining_mark(ch)||is_unicode_connector_punctuation(ch)||is_unicode_digit(code)}function is_identifier_string(str){return/^[a-z_$][a-z0-9_$]*$/i.test(str)}function parse_js_number(num){if(RE_HEX_NUMBER.test(num)){return parseInt(num.substr(2),16)}else if(RE_OCT_NUMBER.test(num)){return parseInt(num.substr(1),8)}else if(RE_DEC_NUMBER.test(num)){return parseFloat(num)}}function JS_Parse_Error(message,filename,line,col,pos){this.message=message;this.filename=filename;this.line=line;this.col=col;this.pos=pos;this.stack=(new Error).stack}JS_Parse_Error.prototype.toString=function(){return this.message+" (line: "+this.line+", col: "+this.col+", pos: "+this.pos+")"+"\n\n"+this.stack};function js_error(message,filename,line,col,pos){throw new JS_Parse_Error(message,filename,line,col,pos)}function is_token(token,type,val){return token.type==type&&(val==null||token.value==val)}var EX_EOF={};function tokenizer($TEXT,filename,html5_comments){var S={text:$TEXT,filename:filename,pos:0,tokpos:0,line:1,tokline:0,col:0,tokcol:0,newline_before:false,regex_allowed:false,comments_before:[]};function peek(){return S.text.charAt(S.pos)}function next(signal_eof,in_string){var ch=S.text.charAt(S.pos++);if(signal_eof&&!ch)throw EX_EOF;if("\r\n\u2028\u2029".indexOf(ch)>=0){S.newline_before=S.newline_before||!in_string;++S.line;S.col=0;if(!in_string&&ch=="\r"&&peek()=="\n"){++S.pos;ch="\n"}}else{++S.col}return ch}function forward(i){while(i-->0)next()}function looking_at(str){return S.text.substr(S.pos,str.length)==str}function find(what,signal_eof){var pos=S.text.indexOf(what,S.pos);if(signal_eof&&pos==-1)throw EX_EOF;return pos}function start_token(){S.tokline=S.line;S.tokcol=S.col;S.tokpos=S.pos}var prev_was_dot=false;function token(type,value,is_comment){S.regex_allowed=type=="operator"&&!UNARY_POSTFIX(value)||type=="keyword"&&KEYWORDS_BEFORE_EXPRESSION(value)||type=="punc"&&PUNC_BEFORE_EXPRESSION(value);prev_was_dot=type=="punc"&&value==".";var ret={type:type,value:value,line:S.tokline,col:S.tokcol,pos:S.tokpos,endline:S.line,endcol:S.col,endpos:S.pos,nlb:S.newline_before,file:filename};if(!is_comment){ret.comments_before=S.comments_before;S.comments_before=[];for(var i=0,len=ret.comments_before.length;i<len;i++){ret.nlb=ret.nlb||ret.comments_before[i].nlb}}S.newline_before=false;return new AST_Token(ret)}function skip_whitespace(){var ch;while(WHITESPACE_CHARS(ch=peek())||ch=="\u2028"||ch=="\u2029")next()}function read_while(pred){var ret="",ch,i=0;while((ch=peek())&&pred(ch,i++))ret+=next();return ret}function parse_error(err){js_error(err,filename,S.tokline,S.tokcol,S.tokpos)}function read_num(prefix){var has_e=false,after_e=false,has_x=false,has_dot=prefix==".";var num=read_while(function(ch,i){var code=ch.charCodeAt(0);switch(code){case 120:case 88:return has_x?false:has_x=true;case 101:case 69:return has_x?true:has_e?false:has_e=after_e=true;case 45:return after_e||i==0&&!prefix;case 43:return after_e;case after_e=false,46:return!has_dot&&!has_x&&!has_e?has_dot=true:false}return is_alphanumeric_char(code)});if(prefix)num=prefix+num;var valid=parse_js_number(num);if(!isNaN(valid)){return token("num",valid)}else{parse_error("Invalid syntax: "+num)}}function read_escaped_char(in_string){var ch=next(true,in_string);switch(ch.charCodeAt(0)){case 110:return"\n";case 114:return"\r";case 116:return"	";case 98:return"\b";case 118:return"";case 102:return"\f";case 48:return"\x00";case 120:return String.fromCharCode(hex_bytes(2));case 117:return String.fromCharCode(hex_bytes(4));case 10:return"";case 13:if(peek()=="\n"){next(true,in_string);return""}}return ch}function hex_bytes(n){var num=0;for(;n>0;--n){var digit=parseInt(next(true),16);if(isNaN(digit))parse_error("Invalid hex-character pattern in string");num=num<<4|digit}return num}var read_string=with_eof_error("Unterminated string constant",function(quote_char){var quote=next(),ret="";for(;;){var ch=next(true,true);if(ch=="\\"){var octal_len=0,first=null;ch=read_while(function(ch){if(ch>="0"&&ch<="7"){if(!first){first=ch;return++octal_len}else if(first<="3"&&octal_len<=2)return++octal_len;else if(first>="4"&&octal_len<=1)return++octal_len}return false});if(octal_len>0)ch=String.fromCharCode(parseInt(ch,8));else ch=read_escaped_char(true)}else if(ch==quote)break;ret+=ch}var tok=token("string",ret);tok.quote=quote_char;return tok});function skip_line_comment(type){var regex_allowed=S.regex_allowed;var i=find("\n"),ret;if(i==-1){ret=S.text.substr(S.pos);S.pos=S.text.length}else{ret=S.text.substring(S.pos,i);S.pos=i}S.col=S.tokcol+(S.pos-S.tokpos);S.comments_before.push(token(type,ret,true));S.regex_allowed=regex_allowed;return next_token()}var skip_multiline_comment=with_eof_error("Unterminated multiline comment",function(){var regex_allowed=S.regex_allowed;var i=find("*/",true);var text=S.text.substring(S.pos,i);var a=text.split("\n"),n=a.length;S.pos=i+2;S.line+=n-1;if(n>1)S.col=a[n-1].length;else S.col+=a[n-1].length;S.col+=2;var nlb=S.newline_before=S.newline_before||text.indexOf("\n")>=0;S.comments_before.push(token("comment2",text,true));S.regex_allowed=regex_allowed;S.newline_before=nlb;return next_token()});function read_name(){var backslash=false,name="",ch,escaped=false,hex;while((ch=peek())!=null){if(!backslash){if(ch=="\\")escaped=backslash=true,next();else if(is_identifier_char(ch))name+=next();else break}else{if(ch!="u")parse_error("Expecting UnicodeEscapeSequence -- uXXXX");ch=read_escaped_char();if(!is_identifier_char(ch))parse_error("Unicode char: "+ch.charCodeAt(0)+" is not valid in identifier");name+=ch;backslash=false}}if(KEYWORDS(name)&&escaped){hex=name.charCodeAt(0).toString(16).toUpperCase();name="\\u"+"0000".substr(hex.length)+hex+name.slice(1)}return name}var read_regexp=with_eof_error("Unterminated regular expression",function(regexp){var prev_backslash=false,ch,in_class=false;while(ch=next(true))if(prev_backslash){regexp+="\\"+ch;prev_backslash=false}else if(ch=="["){in_class=true;regexp+=ch}else if(ch=="]"&&in_class){in_class=false;regexp+=ch}else if(ch=="/"&&!in_class){break}else if(ch=="\\"){prev_backslash=true}else{regexp+=ch}var mods=read_name();return token("regexp",new RegExp(regexp,mods))});function read_operator(prefix){function grow(op){if(!peek())return op;var bigger=op+peek();if(OPERATORS(bigger)){next();return grow(bigger)}else{return op}}return token("operator",grow(prefix||next()))}function handle_slash(){next();switch(peek()){case"/":next();return skip_line_comment("comment1");case"*":next();return skip_multiline_comment()}return S.regex_allowed?read_regexp(""):read_operator("/")}function handle_dot(){next();return is_digit(peek().charCodeAt(0))?read_num("."):token("punc",".")}function read_word(){var word=read_name();if(prev_was_dot)return token("name",word);return KEYWORDS_ATOM(word)?token("atom",word):!KEYWORDS(word)?token("name",word):OPERATORS(word)?token("operator",word):token("keyword",word)}function with_eof_error(eof_error,cont){return function(x){try{return cont(x)}catch(ex){if(ex===EX_EOF)parse_error(eof_error);else throw ex}}}function next_token(force_regexp){if(force_regexp!=null)return read_regexp(force_regexp);skip_whitespace();start_token();if(html5_comments){if(looking_at("<!--")){forward(4);return skip_line_comment("comment3")}if(looking_at("-->")&&S.newline_before){forward(3);return skip_line_comment("comment4")}}var ch=peek();if(!ch)return token("eof");var code=ch.charCodeAt(0);switch(code){case 34:case 39:return read_string(ch);case 46:return handle_dot();case 47:return handle_slash()}if(is_digit(code))return read_num();if(PUNC_CHARS(ch))return token("punc",next());if(OPERATOR_CHARS(ch))return read_operator();if(code==92||is_identifier_start(code))return read_word();parse_error("Unexpected character '"+ch+"'")}next_token.context=function(nc){if(nc)S=nc;return S};return next_token}var UNARY_PREFIX=makePredicate(["typeof","void","delete","--","++","!","~","-","+"]);var UNARY_POSTFIX=makePredicate(["--","++"]);var ASSIGNMENT=makePredicate(["=","+=","-=","/=","*=","%=",">>=","<<=",">>>=","|=","^=","&="]);var PRECEDENCE=function(a,ret){for(var i=0;i<a.length;++i){var b=a[i];for(var j=0;j<b.length;++j){ret[b[j]]=i+1}}return ret}([["||"],["&&"],["|"],["^"],["&"],["==","===","!=","!=="],["<",">","<=",">=","in","instanceof"],[">>","<<",">>>"],["+","-"],["*","/","%"]],{});var STATEMENTS_WITH_LABELS=array_to_hash(["for","do","while","switch"]);var ATOMIC_START_TOKEN=array_to_hash(["atom","num","string","regexp","name"]);function parse($TEXT,options){options=defaults(options,{strict:false,filename:null,toplevel:null,expression:false,html5_comments:true,bare_returns:false});var S={input:typeof $TEXT=="string"?tokenizer($TEXT,options.filename,options.html5_comments):$TEXT,token:null,prev:null,peeked:null,in_function:0,in_directives:true,in_loop:0,labels:[]};S.token=next();function is(type,value){return is_token(S.token,type,value)}function peek(){return S.peeked||(S.peeked=S.input())}function next(){S.prev=S.token;if(S.peeked){S.token=S.peeked;S.peeked=null}else{S.token=S.input()}S.in_directives=S.in_directives&&(S.token.type=="string"||is("punc",";"));return S.token}function prev(){return S.prev}function croak(msg,line,col,pos){var ctx=S.input.context();js_error(msg,ctx.filename,line!=null?line:ctx.tokline,col!=null?col:ctx.tokcol,pos!=null?pos:ctx.tokpos)}function token_error(token,msg){croak(msg,token.line,token.col)}function unexpected(token){if(token==null)token=S.token;token_error(token,"Unexpected token: "+token.type+" ("+token.value+")")}function expect_token(type,val){if(is(type,val)){return next()}token_error(S.token,"Unexpected token "+S.token.type+" «"+S.token.value+"»"+", expected "+type+" «"+val+"»")}function expect(punc){return expect_token("punc",punc)}function can_insert_semicolon(){return!options.strict&&(S.token.nlb||is("eof")||is("punc","}"))}function semicolon(){if(is("punc",";"))next();else if(!can_insert_semicolon())unexpected()}function parenthesised(){expect("(");var exp=expression(true);expect(")");return exp}function embed_tokens(parser){return function(){var start=S.token;var expr=parser();var end=prev();expr.start=start;expr.end=end;return expr}}function handle_regexp(){if(is("operator","/")||is("operator","/=")){S.peeked=null;S.token=S.input(S.token.value.substr(1))}}var statement=embed_tokens(function(){var tmp;handle_regexp();switch(S.token.type){case"string":var dir=S.in_directives,stat=simple_statement();if(dir&&stat.body instanceof AST_String&&!is("punc",",")){return new AST_Directive({start:stat.body.start,end:stat.body.end,quote:stat.body.quote,value:stat.body.value})}return stat;case"num":case"regexp":case"operator":case"atom":return simple_statement();case"name":return is_token(peek(),"punc",":")?labeled_statement():simple_statement();case"punc":switch(S.token.value){case"{":return new AST_BlockStatement({start:S.token,body:block_(),end:prev()});case"[":case"(":return simple_statement();case";":next();return new AST_EmptyStatement;default:unexpected()}case"keyword":switch(tmp=S.token.value,next(),tmp){case"break":return break_cont(AST_Break);case"continue":return break_cont(AST_Continue);case"debugger":semicolon();return new AST_Debugger;case"do":return new AST_Do({body:in_loop(statement),condition:(expect_token("keyword","while"),tmp=parenthesised(),semicolon(),tmp)});case"while":return new AST_While({condition:parenthesised(),body:in_loop(statement)});case"for":return for_();case"function":return function_(AST_Defun);case"if":return if_();case"return":if(S.in_function==0&&!options.bare_returns)croak("'return' outside of function");return new AST_Return({value:is("punc",";")?(next(),null):can_insert_semicolon()?null:(tmp=expression(true),semicolon(),tmp)});case"switch":return new AST_Switch({expression:parenthesised(),body:in_loop(switch_body_)});case"throw":if(S.token.nlb)croak("Illegal newline after 'throw'");return new AST_Throw({value:(tmp=expression(true),semicolon(),tmp)});case"try":return try_();case"var":return tmp=var_(),semicolon(),tmp;case"const":return tmp=const_(),semicolon(),tmp;case"with":return new AST_With({expression:parenthesised(),body:statement()});default:unexpected()}}});function labeled_statement(){var label=as_symbol(AST_Label);if(find_if(function(l){return l.name==label.name},S.labels)){croak("Label "+label.name+" defined twice")}expect(":");S.labels.push(label);var stat=statement();S.labels.pop();if(!(stat instanceof AST_IterationStatement)){label.references.forEach(function(ref){if(ref instanceof AST_Continue){ref=ref.label.start;croak("Continue label `"+label.name+"` refers to non-IterationStatement.",ref.line,ref.col,ref.pos)}})}return new AST_LabeledStatement({body:stat,label:label})}function simple_statement(tmp){return new AST_SimpleStatement({body:(tmp=expression(true),semicolon(),tmp)})}function break_cont(type){var label=null,ldef;if(!can_insert_semicolon()){label=as_symbol(AST_LabelRef,true)}if(label!=null){ldef=find_if(function(l){return l.name==label.name},S.labels);if(!ldef)croak("Undefined label "+label.name);label.thedef=ldef}else if(S.in_loop==0)croak(type.TYPE+" not inside a loop or switch");semicolon();var stat=new type({label:label});if(ldef)ldef.references.push(stat);return stat}function for_(){expect("(");var init=null;if(!is("punc",";")){init=is("keyword","var")?(next(),var_(true)):expression(true,true);if(is("operator","in")){if(init instanceof AST_Var&&init.definitions.length>1)croak("Only one variable declaration allowed in for..in loop");next();return for_in(init)}}return regular_for(init)}function regular_for(init){expect(";");var test=is("punc",";")?null:expression(true);expect(";");var step=is("punc",")")?null:expression(true);expect(")");return new AST_For({init:init,condition:test,step:step,body:in_loop(statement)})}function for_in(init){var lhs=init instanceof AST_Var?init.definitions[0].name:null;var obj=expression(true);expect(")");return new AST_ForIn({init:init,name:lhs,object:obj,body:in_loop(statement)})}var function_=function(ctor){var in_statement=ctor===AST_Defun;var name=is("name")?as_symbol(in_statement?AST_SymbolDefun:AST_SymbolLambda):null;if(in_statement&&!name)unexpected();expect("(");return new ctor({name:name,argnames:function(first,a){while(!is("punc",")")){if(first)first=false;else expect(",");a.push(as_symbol(AST_SymbolFunarg))}next();return a}(true,[]),body:function(loop,labels){++S.in_function;S.in_directives=true;S.in_loop=0;S.labels=[];var a=block_();--S.in_function;S.in_loop=loop;S.labels=labels;return a}(S.in_loop,S.labels)})};function if_(){var cond=parenthesised(),body=statement(),belse=null;if(is("keyword","else")){next();belse=statement()}return new AST_If({condition:cond,body:body,alternative:belse})}function block_(){expect("{");var a=[];while(!is("punc","}")){if(is("eof"))unexpected();a.push(statement())}next();return a}function switch_body_(){expect("{");var a=[],cur=null,branch=null,tmp;while(!is("punc","}")){if(is("eof"))unexpected();if(is("keyword","case")){if(branch)branch.end=prev();cur=[];branch=new AST_Case({start:(tmp=S.token,next(),tmp),expression:expression(true),body:cur});a.push(branch);expect(":")}else if(is("keyword","default")){if(branch)branch.end=prev();cur=[];branch=new AST_Default({start:(tmp=S.token,next(),expect(":"),tmp),body:cur});a.push(branch)}else{if(!cur)unexpected();cur.push(statement())}}if(branch)branch.end=prev();next();return a}function try_(){var body=block_(),bcatch=null,bfinally=null;if(is("keyword","catch")){var start=S.token;next();expect("(");var name=as_symbol(AST_SymbolCatch);expect(")");bcatch=new AST_Catch({start:start,argname:name,body:block_(),end:prev()})}if(is("keyword","finally")){var start=S.token;next();bfinally=new AST_Finally({start:start,body:block_(),end:prev()})}if(!bcatch&&!bfinally)croak("Missing catch/finally blocks");return new AST_Try({body:body,bcatch:bcatch,bfinally:bfinally})}function vardefs(no_in,in_const){var a=[];for(;;){a.push(new AST_VarDef({start:S.token,name:as_symbol(in_const?AST_SymbolConst:AST_SymbolVar),value:is("operator","=")?(next(),expression(false,no_in)):null,end:prev()}));if(!is("punc",","))break;next()}return a}var var_=function(no_in){return new AST_Var({start:prev(),definitions:vardefs(no_in,false),end:prev()})};var const_=function(){return new AST_Const({start:prev(),definitions:vardefs(false,true),end:prev()})};var new_=function(){var start=S.token;expect_token("operator","new");var newexp=expr_atom(false),args;if(is("punc","(")){next();args=expr_list(")")}else{args=[]}return subscripts(new AST_New({start:start,expression:newexp,args:args,end:prev()}),true)};function as_atom_node(){var tok=S.token,ret;switch(tok.type){case"name":case"keyword":ret=_make_symbol(AST_SymbolRef);break;case"num":ret=new AST_Number({start:tok,end:tok,value:tok.value});break;case"string":ret=new AST_String({start:tok,end:tok,value:tok.value,quote:tok.quote});break;case"regexp":ret=new AST_RegExp({start:tok,end:tok,value:tok.value});break;case"atom":switch(tok.value){case"false":ret=new AST_False({start:tok,end:tok});break;case"true":ret=new AST_True({start:tok,end:tok});break;case"null":ret=new AST_Null({start:tok,end:tok});break}break}next();return ret}var expr_atom=function(allow_calls){if(is("operator","new")){return new_()}var start=S.token;if(is("punc")){switch(start.value){case"(":next();var ex=expression(true);ex.start=start;ex.end=S.token;expect(")");return subscripts(ex,allow_calls);case"[":return subscripts(array_(),allow_calls);case"{":return subscripts(object_(),allow_calls)}unexpected()}if(is("keyword","function")){next();var func=function_(AST_Function);func.start=start;func.end=prev();return subscripts(func,allow_calls)}if(ATOMIC_START_TOKEN[S.token.type]){return subscripts(as_atom_node(),allow_calls)}unexpected()};function expr_list(closing,allow_trailing_comma,allow_empty){var first=true,a=[];while(!is("punc",closing)){if(first)first=false;else expect(",");if(allow_trailing_comma&&is("punc",closing))break;if(is("punc",",")&&allow_empty){a.push(new AST_Hole({start:S.token,end:S.token}))}else{a.push(expression(false))}}next();return a}var array_=embed_tokens(function(){expect("[");return new AST_Array({elements:expr_list("]",!options.strict,true)})});var object_=embed_tokens(function(){expect("{");var first=true,a=[];while(!is("punc","}")){if(first)first=false;else expect(",");if(!options.strict&&is("punc","}"))break;var start=S.token;var type=start.type;var name=as_property_name();if(type=="name"&&!is("punc",":")){if(name=="get"){a.push(new AST_ObjectGetter({start:start,key:as_atom_node(),value:function_(AST_Accessor),end:prev()}));continue}if(name=="set"){a.push(new AST_ObjectSetter({start:start,key:as_atom_node(),value:function_(AST_Accessor),end:prev()}));continue}}expect(":");a.push(new AST_ObjectKeyVal({start:start,quote:start.quote,key:name,value:expression(false),end:prev()}))}next();return new AST_Object({properties:a})});function as_property_name(){var tmp=S.token;next();switch(tmp.type){case"num":case"string":case"name":case"operator":case"keyword":case"atom":return tmp.value;default:unexpected()}}function as_name(){var tmp=S.token;next();switch(tmp.type){case"name":case"operator":case"keyword":case"atom":return tmp.value;default:unexpected()}}function _make_symbol(type){var name=S.token.value;return new(name=="this"?AST_This:type)({name:String(name),start:S.token,end:S.token})}function as_symbol(type,noerror){if(!is("name")){if(!noerror)croak("Name expected");return null}var sym=_make_symbol(type);next();return sym}var subscripts=function(expr,allow_calls){var start=expr.start;if(is("punc",".")){next();return subscripts(new AST_Dot({start:start,expression:expr,property:as_name(),end:prev()}),allow_calls)}if(is("punc","[")){next();var prop=expression(true);expect("]");return subscripts(new AST_Sub({start:start,expression:expr,property:prop,end:prev()}),allow_calls)}if(allow_calls&&is("punc","(")){next();return subscripts(new AST_Call({start:start,expression:expr,args:expr_list(")"),end:prev()}),true)}return expr};var maybe_unary=function(allow_calls){var start=S.token;if(is("operator")&&UNARY_PREFIX(start.value)){next();handle_regexp();var ex=make_unary(AST_UnaryPrefix,start.value,maybe_unary(allow_calls));ex.start=start;ex.end=prev();return ex}var val=expr_atom(allow_calls);while(is("operator")&&UNARY_POSTFIX(S.token.value)&&!S.token.nlb){val=make_unary(AST_UnaryPostfix,S.token.value,val);val.start=start;val.end=S.token;next()}return val};function make_unary(ctor,op,expr){if((op=="++"||op=="--")&&!is_assignable(expr))croak("Invalid use of "+op+" operator");return new ctor({operator:op,expression:expr
})}var expr_op=function(left,min_prec,no_in){var op=is("operator")?S.token.value:null;if(op=="in"&&no_in)op=null;var prec=op!=null?PRECEDENCE[op]:null;if(prec!=null&&prec>min_prec){next();var right=expr_op(maybe_unary(true),prec,no_in);return expr_op(new AST_Binary({start:left.start,left:left,operator:op,right:right,end:right.end}),min_prec,no_in)}return left};function expr_ops(no_in){return expr_op(maybe_unary(true),0,no_in)}var maybe_conditional=function(no_in){var start=S.token;var expr=expr_ops(no_in);if(is("operator","?")){next();var yes=expression(false);expect(":");return new AST_Conditional({start:start,condition:expr,consequent:yes,alternative:expression(false,no_in),end:prev()})}return expr};function is_assignable(expr){if(!options.strict)return true;if(expr instanceof AST_This)return false;return expr instanceof AST_PropAccess||expr instanceof AST_Symbol}var maybe_assign=function(no_in){var start=S.token;var left=maybe_conditional(no_in),val=S.token.value;if(is("operator")&&ASSIGNMENT(val)){if(is_assignable(left)){next();return new AST_Assign({start:start,left:left,operator:val,right:maybe_assign(no_in),end:prev()})}croak("Invalid assignment")}return left};var expression=function(commas,no_in){var start=S.token;var expr=maybe_assign(no_in);if(commas&&is("punc",",")){next();return new AST_Seq({start:start,car:expr,cdr:expression(true,no_in),end:peek()})}return expr};function in_loop(cont){++S.in_loop;var ret=cont();--S.in_loop;return ret}if(options.expression){return expression(true)}return function(){var start=S.token;var body=[];while(!is("eof"))body.push(statement());var end=prev();var toplevel=options.toplevel;if(toplevel){toplevel.body=toplevel.body.concat(body);toplevel.end=end}else{toplevel=new AST_Toplevel({start:start,body:body,end:end})}return toplevel}()}/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function TreeTransformer(before,after){TreeWalker.call(this);this.before=before;this.after=after}TreeTransformer.prototype=new TreeWalker;(function(undefined){function _(node,descend){node.DEFMETHOD("transform",function(tw,in_list){var x,y;tw.push(this);if(tw.before)x=tw.before(this,descend,in_list);if(x===undefined){if(!tw.after){x=this;descend(x,tw)}else{tw.stack[tw.stack.length-1]=x=this.clone();descend(x,tw);y=tw.after(x,in_list);if(y!==undefined)x=y}}tw.pop();return x})}function do_list(list,tw){return MAP(list,function(node){return node.transform(tw,true)})}_(AST_Node,noop);_(AST_LabeledStatement,function(self,tw){self.label=self.label.transform(tw);self.body=self.body.transform(tw)});_(AST_SimpleStatement,function(self,tw){self.body=self.body.transform(tw)});_(AST_Block,function(self,tw){self.body=do_list(self.body,tw)});_(AST_DWLoop,function(self,tw){self.condition=self.condition.transform(tw);self.body=self.body.transform(tw)});_(AST_For,function(self,tw){if(self.init)self.init=self.init.transform(tw);if(self.condition)self.condition=self.condition.transform(tw);if(self.step)self.step=self.step.transform(tw);self.body=self.body.transform(tw)});_(AST_ForIn,function(self,tw){self.init=self.init.transform(tw);self.object=self.object.transform(tw);self.body=self.body.transform(tw)});_(AST_With,function(self,tw){self.expression=self.expression.transform(tw);self.body=self.body.transform(tw)});_(AST_Exit,function(self,tw){if(self.value)self.value=self.value.transform(tw)});_(AST_LoopControl,function(self,tw){if(self.label)self.label=self.label.transform(tw)});_(AST_If,function(self,tw){self.condition=self.condition.transform(tw);self.body=self.body.transform(tw);if(self.alternative)self.alternative=self.alternative.transform(tw)});_(AST_Switch,function(self,tw){self.expression=self.expression.transform(tw);self.body=do_list(self.body,tw)});_(AST_Case,function(self,tw){self.expression=self.expression.transform(tw);self.body=do_list(self.body,tw)});_(AST_Try,function(self,tw){self.body=do_list(self.body,tw);if(self.bcatch)self.bcatch=self.bcatch.transform(tw);if(self.bfinally)self.bfinally=self.bfinally.transform(tw)});_(AST_Catch,function(self,tw){self.argname=self.argname.transform(tw);self.body=do_list(self.body,tw)});_(AST_Definitions,function(self,tw){self.definitions=do_list(self.definitions,tw)});_(AST_VarDef,function(self,tw){self.name=self.name.transform(tw);if(self.value)self.value=self.value.transform(tw)});_(AST_Lambda,function(self,tw){if(self.name)self.name=self.name.transform(tw);self.argnames=do_list(self.argnames,tw);self.body=do_list(self.body,tw)});_(AST_Call,function(self,tw){self.expression=self.expression.transform(tw);self.args=do_list(self.args,tw)});_(AST_Seq,function(self,tw){self.car=self.car.transform(tw);self.cdr=self.cdr.transform(tw)});_(AST_Dot,function(self,tw){self.expression=self.expression.transform(tw)});_(AST_Sub,function(self,tw){self.expression=self.expression.transform(tw);self.property=self.property.transform(tw)});_(AST_Unary,function(self,tw){self.expression=self.expression.transform(tw)});_(AST_Binary,function(self,tw){self.left=self.left.transform(tw);self.right=self.right.transform(tw)});_(AST_Conditional,function(self,tw){self.condition=self.condition.transform(tw);self.consequent=self.consequent.transform(tw);self.alternative=self.alternative.transform(tw)});_(AST_Array,function(self,tw){self.elements=do_list(self.elements,tw)});_(AST_Object,function(self,tw){self.properties=do_list(self.properties,tw)});_(AST_ObjectProperty,function(self,tw){self.value=self.value.transform(tw)})})();/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function SymbolDef(scope,index,orig){this.name=orig.name;this.orig=[orig];this.scope=scope;this.references=[];this.global=false;this.mangled_name=null;this.undeclared=false;this.constant=false;this.index=index}SymbolDef.prototype={unmangleable:function(options){if(!options)options={};return this.global&&!options.toplevel||this.undeclared||!options.eval&&(this.scope.uses_eval||this.scope.uses_with)||options.keep_fnames&&(this.orig[0]instanceof AST_SymbolLambda||this.orig[0]instanceof AST_SymbolDefun)},mangle:function(options){var cache=options.cache&&options.cache.props;if(this.global&&cache&&cache.has(this.name)){this.mangled_name=cache.get(this.name)}else if(!this.mangled_name&&!this.unmangleable(options)){var s=this.scope;if(!options.screw_ie8&&this.orig[0]instanceof AST_SymbolLambda)s=s.parent_scope;this.mangled_name=s.next_mangled(options,this);if(this.global&&cache){cache.set(this.name,this.mangled_name)}}}};AST_Toplevel.DEFMETHOD("figure_out_scope",function(options){options=defaults(options,{screw_ie8:false,cache:null});var self=this;var scope=self.parent_scope=null;var defun=null;var nesting=0;var tw=new TreeWalker(function(node,descend){if(options.screw_ie8&&node instanceof AST_Catch){var save_scope=scope;scope=new AST_Scope(node);scope.init_scope_vars(nesting);scope.parent_scope=save_scope;descend();scope=save_scope;return true}if(node instanceof AST_Scope){node.init_scope_vars(nesting);var save_scope=node.parent_scope=scope;var save_defun=defun;defun=scope=node;++nesting;descend();--nesting;scope=save_scope;defun=save_defun;return true}if(node instanceof AST_Directive){node.scope=scope;push_uniq(scope.directives,node.value);return true}if(node instanceof AST_With){for(var s=scope;s;s=s.parent_scope)s.uses_with=true;return}if(node instanceof AST_Symbol){node.scope=scope}if(node instanceof AST_SymbolLambda){defun.def_function(node)}else if(node instanceof AST_SymbolDefun){(node.scope=defun.parent_scope).def_function(node)}else if(node instanceof AST_SymbolVar||node instanceof AST_SymbolConst){var def=defun.def_variable(node);def.constant=node instanceof AST_SymbolConst;def.init=tw.parent().value}else if(node instanceof AST_SymbolCatch){(options.screw_ie8?scope:defun).def_variable(node)}});self.walk(tw);var func=null;var globals=self.globals=new Dictionary;var tw=new TreeWalker(function(node,descend){if(node instanceof AST_Lambda){var prev_func=func;func=node;descend();func=prev_func;return true}if(node instanceof AST_SymbolRef){var name=node.name;var sym=node.scope.find_variable(name);if(!sym){var g;if(globals.has(name)){g=globals.get(name)}else{g=new SymbolDef(self,globals.size(),node);g.undeclared=true;g.global=true;globals.set(name,g)}node.thedef=g;if(name=="eval"&&tw.parent()instanceof AST_Call){for(var s=node.scope;s&&!s.uses_eval;s=s.parent_scope)s.uses_eval=true}if(func&&name=="arguments"){func.uses_arguments=true}}else{node.thedef=sym}node.reference();return true}});self.walk(tw);if(options.cache){this.cname=options.cache.cname}});AST_Scope.DEFMETHOD("init_scope_vars",function(nesting){this.directives=[];this.variables=new Dictionary;this.functions=new Dictionary;this.uses_with=false;this.uses_eval=false;this.parent_scope=null;this.enclosed=[];this.cname=-1;this.nesting=nesting});AST_Scope.DEFMETHOD("strict",function(){return this.has_directive("use strict")});AST_Lambda.DEFMETHOD("init_scope_vars",function(){AST_Scope.prototype.init_scope_vars.apply(this,arguments);this.uses_arguments=false});AST_SymbolRef.DEFMETHOD("reference",function(){var def=this.definition();def.references.push(this);var s=this.scope;while(s){push_uniq(s.enclosed,def);if(s===def.scope)break;s=s.parent_scope}this.frame=this.scope.nesting-def.scope.nesting});AST_Scope.DEFMETHOD("find_variable",function(name){if(name instanceof AST_Symbol)name=name.name;return this.variables.get(name)||this.parent_scope&&this.parent_scope.find_variable(name)});AST_Scope.DEFMETHOD("has_directive",function(value){return this.parent_scope&&this.parent_scope.has_directive(value)||(this.directives.indexOf(value)>=0?this:null)});AST_Scope.DEFMETHOD("def_function",function(symbol){this.functions.set(symbol.name,this.def_variable(symbol))});AST_Scope.DEFMETHOD("def_variable",function(symbol){var def;if(!this.variables.has(symbol.name)){def=new SymbolDef(this,this.variables.size(),symbol);this.variables.set(symbol.name,def);def.global=!this.parent_scope}else{def=this.variables.get(symbol.name);def.orig.push(symbol)}return symbol.thedef=def});AST_Scope.DEFMETHOD("next_mangled",function(options){var ext=this.enclosed;out:while(true){var m=base54(++this.cname);if(!is_identifier(m))continue;if(options.except.indexOf(m)>=0)continue;for(var i=ext.length;--i>=0;){var sym=ext[i];var name=sym.mangled_name||sym.unmangleable(options)&&sym.name;if(m==name)continue out}return m}});AST_Function.DEFMETHOD("next_mangled",function(options,def){var tricky_def=def.orig[0]instanceof AST_SymbolFunarg&&this.name&&this.name.definition();while(true){var name=AST_Lambda.prototype.next_mangled.call(this,options,def);if(!(tricky_def&&tricky_def.mangled_name==name))return name}});AST_Scope.DEFMETHOD("references",function(sym){if(sym instanceof AST_Symbol)sym=sym.definition();return this.enclosed.indexOf(sym)<0?null:sym});AST_Symbol.DEFMETHOD("unmangleable",function(options){return this.definition().unmangleable(options)});AST_SymbolAccessor.DEFMETHOD("unmangleable",function(){return true});AST_Label.DEFMETHOD("unmangleable",function(){return false});AST_Symbol.DEFMETHOD("unreferenced",function(){return this.definition().references.length==0&&!(this.scope.uses_eval||this.scope.uses_with)});AST_Symbol.DEFMETHOD("undeclared",function(){return this.definition().undeclared});AST_LabelRef.DEFMETHOD("undeclared",function(){return false});AST_Label.DEFMETHOD("undeclared",function(){return false});AST_Symbol.DEFMETHOD("definition",function(){return this.thedef});AST_Symbol.DEFMETHOD("global",function(){return this.definition().global});AST_Toplevel.DEFMETHOD("_default_mangler_options",function(options){return defaults(options,{except:[],eval:false,sort:false,toplevel:false,screw_ie8:false,keep_fnames:false})});AST_Toplevel.DEFMETHOD("mangle_names",function(options){options=this._default_mangler_options(options);var lname=-1;var to_mangle=[];if(options.cache){this.globals.each(function(symbol){if(options.except.indexOf(symbol.name)<0){to_mangle.push(symbol)}})}var tw=new TreeWalker(function(node,descend){if(node instanceof AST_LabeledStatement){var save_nesting=lname;descend();lname=save_nesting;return true}if(node instanceof AST_Scope){var p=tw.parent(),a=[];node.variables.each(function(symbol){if(options.except.indexOf(symbol.name)<0){a.push(symbol)}});if(options.sort)a.sort(function(a,b){return b.references.length-a.references.length});to_mangle.push.apply(to_mangle,a);return}if(node instanceof AST_Label){var name;do name=base54(++lname);while(!is_identifier(name));node.mangled_name=name;return true}if(options.screw_ie8&&node instanceof AST_SymbolCatch){to_mangle.push(node.definition());return}});this.walk(tw);to_mangle.forEach(function(def){def.mangle(options)});if(options.cache){options.cache.cname=this.cname}});AST_Toplevel.DEFMETHOD("compute_char_frequency",function(options){options=this._default_mangler_options(options);var tw=new TreeWalker(function(node){if(node instanceof AST_Constant)base54.consider(node.print_to_string());else if(node instanceof AST_Return)base54.consider("return");else if(node instanceof AST_Throw)base54.consider("throw");else if(node instanceof AST_Continue)base54.consider("continue");else if(node instanceof AST_Break)base54.consider("break");else if(node instanceof AST_Debugger)base54.consider("debugger");else if(node instanceof AST_Directive)base54.consider(node.value);else if(node instanceof AST_While)base54.consider("while");else if(node instanceof AST_Do)base54.consider("do while");else if(node instanceof AST_If){base54.consider("if");if(node.alternative)base54.consider("else")}else if(node instanceof AST_Var)base54.consider("var");else if(node instanceof AST_Const)base54.consider("const");else if(node instanceof AST_Lambda)base54.consider("function");else if(node instanceof AST_For)base54.consider("for");else if(node instanceof AST_ForIn)base54.consider("for in");else if(node instanceof AST_Switch)base54.consider("switch");else if(node instanceof AST_Case)base54.consider("case");else if(node instanceof AST_Default)base54.consider("default");else if(node instanceof AST_With)base54.consider("with");else if(node instanceof AST_ObjectSetter)base54.consider("set"+node.key);else if(node instanceof AST_ObjectGetter)base54.consider("get"+node.key);else if(node instanceof AST_ObjectKeyVal)base54.consider(node.key);else if(node instanceof AST_New)base54.consider("new");else if(node instanceof AST_This)base54.consider("this");else if(node instanceof AST_Try)base54.consider("try");else if(node instanceof AST_Catch)base54.consider("catch");else if(node instanceof AST_Finally)base54.consider("finally");else if(node instanceof AST_Symbol&&node.unmangleable(options))base54.consider(node.name);else if(node instanceof AST_Unary||node instanceof AST_Binary)base54.consider(node.operator);else if(node instanceof AST_Dot)base54.consider(node.property)});this.walk(tw);base54.sort()});var base54=function(){var string="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ$_0123456789";var chars,frequency;function reset(){frequency=Object.create(null);chars=string.split("").map(function(ch){return ch.charCodeAt(0)});chars.forEach(function(ch){frequency[ch]=0})}base54.consider=function(str){for(var i=str.length;--i>=0;){var code=str.charCodeAt(i);if(code in frequency)++frequency[code]}};base54.sort=function(){chars=mergeSort(chars,function(a,b){if(is_digit(a)&&!is_digit(b))return 1;if(is_digit(b)&&!is_digit(a))return-1;return frequency[b]-frequency[a]})};base54.reset=reset;reset();base54.get=function(){return chars};base54.freq=function(){return frequency};function base54(num){var ret="",base=54;num++;do{num--;ret+=String.fromCharCode(chars[num%base]);num=Math.floor(num/base);base=64}while(num>0);return ret}return base54}();AST_Toplevel.DEFMETHOD("scope_warnings",function(options){options=defaults(options,{undeclared:false,unreferenced:true,assign_to_global:true,func_arguments:true,nested_defuns:true,eval:true});var tw=new TreeWalker(function(node){if(options.undeclared&&node instanceof AST_SymbolRef&&node.undeclared()){AST_Node.warn("Undeclared symbol: {name} [{file}:{line},{col}]",{name:node.name,file:node.start.file,line:node.start.line,col:node.start.col})}if(options.assign_to_global){var sym=null;if(node instanceof AST_Assign&&node.left instanceof AST_SymbolRef)sym=node.left;else if(node instanceof AST_ForIn&&node.init instanceof AST_SymbolRef)sym=node.init;if(sym&&(sym.undeclared()||sym.global()&&sym.scope!==sym.definition().scope)){AST_Node.warn("{msg}: {name} [{file}:{line},{col}]",{msg:sym.undeclared()?"Accidental global?":"Assignment to global",name:sym.name,file:sym.start.file,line:sym.start.line,col:sym.start.col})}}if(options.eval&&node instanceof AST_SymbolRef&&node.undeclared()&&node.name=="eval"){AST_Node.warn("Eval is used [{file}:{line},{col}]",node.start)}if(options.unreferenced&&(node instanceof AST_SymbolDeclaration||node instanceof AST_Label)&&!(node instanceof AST_SymbolCatch)&&node.unreferenced()){AST_Node.warn("{type} {name} is declared but not referenced [{file}:{line},{col}]",{type:node instanceof AST_Label?"Label":"Symbol",name:node.name,file:node.start.file,line:node.start.line,col:node.start.col})}if(options.func_arguments&&node instanceof AST_Lambda&&node.uses_arguments){AST_Node.warn("arguments used in function {name} [{file}:{line},{col}]",{name:node.name?node.name.name:"anonymous",file:node.start.file,line:node.start.line,col:node.start.col})}if(options.nested_defuns&&node instanceof AST_Defun&&!(tw.parent()instanceof AST_Scope)){AST_Node.warn('Function {name} declared in nested statement "{type}" [{file}:{line},{col}]',{name:node.name.name,type:tw.parent().TYPE,file:node.start.file,line:node.start.line,col:node.start.col})}});this.walk(tw)});/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function OutputStream(options){options=defaults(options,{indent_start:0,indent_level:4,quote_keys:false,space_colon:true,ascii_only:false,unescape_regexps:false,inline_script:false,width:80,max_line_len:32e3,beautify:false,source_map:null,bracketize:false,semicolons:true,comments:false,preserve_line:false,screw_ie8:false,preamble:null,quote_style:0},true);var indentation=0;var current_col=0;var current_line=1;var current_pos=0;var OUTPUT="";function to_ascii(str,identifier){return str.replace(/[\u0080-\uffff]/g,function(ch){var code=ch.charCodeAt(0).toString(16);if(code.length<=2&&!identifier){while(code.length<2)code="0"+code;return"\\x"+code}else{while(code.length<4)code="0"+code;return"\\u"+code}})}function make_string(str,quote){var dq=0,sq=0;str=str.replace(/[\\\b\f\n\r\t\x22\x27\u2028\u2029\0\ufeff]/g,function(s){switch(s){case"\\":return"\\\\";case"\b":return"\\b";case"\f":return"\\f";case"\n":return"\\n";case"\r":return"\\r";case"\u2028":return"\\u2028";case"\u2029":return"\\u2029";case'"':++dq;return'"';case"'":++sq;return"'";case"\x00":return"\\x00";case"\ufeff":return"\\ufeff"}return s});function quote_single(){return"'"+str.replace(/\x27/g,"\\'")+"'"}function quote_double(){return'"'+str.replace(/\x22/g,'\\"')+'"'}if(options.ascii_only)str=to_ascii(str);switch(options.quote_style){case 1:return quote_single();case 2:return quote_double();case 3:return quote=="'"?quote_single():quote_double();default:return dq>sq?quote_single():quote_double()}}function encode_string(str,quote){var ret=make_string(str,quote);if(options.inline_script)ret=ret.replace(/<\x2fscript([>\/\t\n\f\r ])/gi,"<\\/script$1");return ret}function make_name(name){name=name.toString();if(options.ascii_only)name=to_ascii(name,true);return name}function make_indent(back){return repeat_string(" ",options.indent_start+indentation-back*options.indent_level)}var might_need_space=false;var might_need_semicolon=false;var last=null;function last_char(){return last.charAt(last.length-1)}function maybe_newline(){if(options.max_line_len&&current_col>options.max_line_len)print("\n")}var requireSemicolonChars=makePredicate("( [ + * / - , .");function print(str){str=String(str);var ch=str.charAt(0);if(might_need_semicolon){if((!ch||";}".indexOf(ch)<0)&&!/[;]$/.test(last)){if(options.semicolons||requireSemicolonChars(ch)){OUTPUT+=";";current_col++;current_pos++}else{OUTPUT+="\n";current_pos++;current_line++;current_col=0}if(!options.beautify)might_need_space=false}might_need_semicolon=false}if(!options.beautify&&options.preserve_line&&stack[stack.length-1]){var target_line=stack[stack.length-1].start.line;while(current_line<target_line){OUTPUT+="\n";current_pos++;current_line++;current_col=0;might_need_space=false}}if(might_need_space){var prev=last_char();if(is_identifier_char(prev)&&(is_identifier_char(ch)||ch=="\\")||/^[\+\-\/]$/.test(ch)&&ch==prev){OUTPUT+=" ";current_col++;current_pos++}might_need_space=false}var a=str.split(/\r?\n/),n=a.length-1;current_line+=n;if(n==0){current_col+=a[n].length}else{current_col=a[n].length}current_pos+=str.length;last=str;OUTPUT+=str}var space=options.beautify?function(){print(" ")}:function(){might_need_space=true};var indent=options.beautify?function(half){if(options.beautify){print(make_indent(half?.5:0))}}:noop;var with_indent=options.beautify?function(col,cont){if(col===true)col=next_indent();var save_indentation=indentation;indentation=col;var ret=cont();indentation=save_indentation;return ret}:function(col,cont){return cont()};var newline=options.beautify?function(){print("\n")}:maybe_newline;var semicolon=options.beautify?function(){print(";")}:function(){might_need_semicolon=true};function force_semicolon(){might_need_semicolon=false;print(";")}function next_indent(){return indentation+options.indent_level}function with_block(cont){var ret;print("{");newline();with_indent(next_indent(),function(){ret=cont()});indent();print("}");return ret}function with_parens(cont){print("(");var ret=cont();print(")");return ret}function with_square(cont){print("[");var ret=cont();print("]");return ret}function comma(){print(",");space()}function colon(){print(":");if(options.space_colon)space()}var add_mapping=options.source_map?function(token,name){try{if(token)options.source_map.add(token.file||"?",current_line,current_col,token.line,token.col,!name&&token.type=="name"?token.value:name)}catch(ex){AST_Node.warn("Couldn't figure out mapping for {file}:{line},{col} → {cline},{ccol} [{name}]",{file:token.file,line:token.line,col:token.col,cline:current_line,ccol:current_col,name:name||""})}}:noop;function get(){return OUTPUT}if(options.preamble){print(options.preamble.replace(/\r\n?|[\n\u2028\u2029]|\s*$/g,"\n"))}var stack=[];return{get:get,toString:get,indent:indent,indentation:function(){return indentation},current_width:function(){return current_col-indentation},should_break:function(){return options.width&&this.current_width()>=options.width},newline:newline,print:print,space:space,comma:comma,colon:colon,last:function(){return last},semicolon:semicolon,force_semicolon:force_semicolon,to_ascii:to_ascii,print_name:function(name){print(make_name(name))},print_string:function(str,quote){print(encode_string(str,quote))},next_indent:next_indent,with_indent:with_indent,with_block:with_block,with_parens:with_parens,with_square:with_square,add_mapping:add_mapping,option:function(opt){return options[opt]},line:function(){return current_line},col:function(){return current_col},pos:function(){return current_pos},push_node:function(node){stack.push(node)},pop_node:function(){return stack.pop()},stack:function(){return stack},parent:function(n){return stack[stack.length-2-(n||0)]}}}(function(){function DEFPRINT(nodetype,generator){nodetype.DEFMETHOD("_codegen",generator)}AST_Node.DEFMETHOD("print",function(stream,force_parens){var self=this,generator=self._codegen;function doit(){self.add_comments(stream);self.add_source_map(stream);generator(self,stream)}stream.push_node(self);if(force_parens||self.needs_parens(stream)){stream.with_parens(doit)}else{doit()}stream.pop_node()});AST_Node.DEFMETHOD("print_to_string",function(options){var s=OutputStream(options);this.print(s);return s.get()});AST_Node.DEFMETHOD("add_comments",function(output){var c=output.option("comments"),self=this;if(c){var start=self.start;if(start&&!start._comments_dumped){start._comments_dumped=true;var comments=start.comments_before||[];if(self instanceof AST_Exit&&self.value){self.value.walk(new TreeWalker(function(node){if(node.start&&node.start.comments_before){comments=comments.concat(node.start.comments_before);node.start.comments_before=[]}if(node instanceof AST_Function||node instanceof AST_Array||node instanceof AST_Object){return true}}))}if(c.test){comments=comments.filter(function(comment){return c.test(comment.value)})}else if(typeof c=="function"){comments=comments.filter(function(comment){return c(self,comment)})}if(!output.option("beautify")&&comments.length>0&&/comment[134]/.test(comments[0].type)&&output.col()!==0&&comments[0].nlb){output.print("\n")}comments.forEach(function(c){if(/comment[134]/.test(c.type)){output.print("//"+c.value+"\n");output.indent()}else if(c.type=="comment2"){output.print("/*"+c.value+"*/");if(start.nlb){output.print("\n");output.indent()}else{output.space()}}})}}});function PARENS(nodetype,func){if(Array.isArray(nodetype)){nodetype.forEach(function(nodetype){PARENS(nodetype,func)})}else{nodetype.DEFMETHOD("needs_parens",func)}}PARENS(AST_Node,function(){return false});PARENS(AST_Function,function(output){return first_in_statement(output)});PARENS(AST_Object,function(output){return first_in_statement(output)});PARENS([AST_Unary,AST_Undefined],function(output){var p=output.parent();return p instanceof AST_PropAccess&&p.expression===this});PARENS(AST_Seq,function(output){var p=output.parent();return p instanceof AST_Call||p instanceof AST_Unary||p instanceof AST_Binary||p instanceof AST_VarDef||p instanceof AST_PropAccess||p instanceof AST_Array||p instanceof AST_ObjectProperty||p instanceof AST_Conditional});PARENS(AST_Binary,function(output){var p=output.parent();if(p instanceof AST_Call&&p.expression===this)return true;if(p instanceof AST_Unary)return true;if(p instanceof AST_PropAccess&&p.expression===this)return true;if(p instanceof AST_Binary){var po=p.operator,pp=PRECEDENCE[po];var so=this.operator,sp=PRECEDENCE[so];if(pp>sp||pp==sp&&this===p.right){return true}}});PARENS(AST_PropAccess,function(output){var p=output.parent();if(p instanceof AST_New&&p.expression===this){try{this.walk(new TreeWalker(function(node){if(node instanceof AST_Call)throw p}))}catch(ex){if(ex!==p)throw ex;return true}}});PARENS(AST_Call,function(output){var p=output.parent(),p1;if(p instanceof AST_New&&p.expression===this)return true;return this.expression instanceof AST_Function&&p instanceof AST_PropAccess&&p.expression===this&&(p1=output.parent(1))instanceof AST_Assign&&p1.left===p});PARENS(AST_New,function(output){var p=output.parent();if(no_constructor_parens(this,output)&&(p instanceof AST_PropAccess||p instanceof AST_Call&&p.expression===this))return true});PARENS(AST_Number,function(output){var p=output.parent();if(this.getValue()<0&&p instanceof AST_PropAccess&&p.expression===this)return true});PARENS([AST_Assign,AST_Conditional],function(output){var p=output.parent();if(p instanceof AST_Unary)return true;if(p instanceof AST_Binary&&!(p instanceof AST_Assign))return true;if(p instanceof AST_Call&&p.expression===this)return true;if(p instanceof AST_Conditional&&p.condition===this)return true;if(p instanceof AST_PropAccess&&p.expression===this)return true});DEFPRINT(AST_Directive,function(self,output){output.print_string(self.value,self.quote);output.semicolon()});DEFPRINT(AST_Debugger,function(self,output){output.print("debugger");output.semicolon()});function display_body(body,is_toplevel,output){var last=body.length-1;body.forEach(function(stmt,i){if(!(stmt instanceof AST_EmptyStatement)){output.indent();stmt.print(output);if(!(i==last&&is_toplevel)){output.newline();if(is_toplevel)output.newline()}}})}AST_StatementWithBody.DEFMETHOD("_do_print_body",function(output){force_statement(this.body,output)});DEFPRINT(AST_Statement,function(self,output){self.body.print(output);output.semicolon()});DEFPRINT(AST_Toplevel,function(self,output){display_body(self.body,true,output);output.print("")});DEFPRINT(AST_LabeledStatement,function(self,output){self.label.print(output);output.colon();self.body.print(output)});DEFPRINT(AST_SimpleStatement,function(self,output){self.body.print(output);output.semicolon()});function print_bracketed(body,output){if(body.length>0)output.with_block(function(){display_body(body,false,output)});else output.print("{}")}DEFPRINT(AST_BlockStatement,function(self,output){print_bracketed(self.body,output)});DEFPRINT(AST_EmptyStatement,function(self,output){output.semicolon()});DEFPRINT(AST_Do,function(self,output){output.print("do");output.space();self._do_print_body(output);output.space();output.print("while");output.space();output.with_parens(function(){self.condition.print(output)});output.semicolon()});DEFPRINT(AST_While,function(self,output){output.print("while");output.space();output.with_parens(function(){self.condition.print(output)});output.space();self._do_print_body(output)});DEFPRINT(AST_For,function(self,output){output.print("for");output.space();output.with_parens(function(){if(self.init&&!(self.init instanceof AST_EmptyStatement)){if(self.init instanceof AST_Definitions){self.init.print(output)}else{parenthesize_for_noin(self.init,output,true)}output.print(";");output.space()}else{output.print(";")}if(self.condition){self.condition.print(output);output.print(";");output.space()}else{output.print(";")}if(self.step){self.step.print(output)}});output.space();self._do_print_body(output)});DEFPRINT(AST_ForIn,function(self,output){output.print("for");output.space();output.with_parens(function(){self.init.print(output);output.space();output.print("in");output.space();self.object.print(output)});output.space();self._do_print_body(output)});DEFPRINT(AST_With,function(self,output){output.print("with");output.space();output.with_parens(function(){self.expression.print(output)});output.space();self._do_print_body(output)});AST_Lambda.DEFMETHOD("_do_print",function(output,nokeyword){var self=this;if(!nokeyword){output.print("function")}if(self.name){output.space();self.name.print(output)}output.with_parens(function(){self.argnames.forEach(function(arg,i){if(i)output.comma();arg.print(output)})});output.space();print_bracketed(self.body,output)});DEFPRINT(AST_Lambda,function(self,output){self._do_print(output)});AST_Exit.DEFMETHOD("_do_print",function(output,kind){output.print(kind);if(this.value){output.space();this.value.print(output)}output.semicolon()});DEFPRINT(AST_Return,function(self,output){self._do_print(output,"return")});DEFPRINT(AST_Throw,function(self,output){self._do_print(output,"throw")});AST_LoopControl.DEFMETHOD("_do_print",function(output,kind){output.print(kind);if(this.label){output.space();this.label.print(output)}output.semicolon()});DEFPRINT(AST_Break,function(self,output){self._do_print(output,"break")});DEFPRINT(AST_Continue,function(self,output){self._do_print(output,"continue")});function make_then(self,output){if(output.option("bracketize")){make_block(self.body,output);return}if(!self.body)return output.force_semicolon();if(self.body instanceof AST_Do&&!output.option("screw_ie8")){make_block(self.body,output);return}var b=self.body;while(true){if(b instanceof AST_If){if(!b.alternative){make_block(self.body,output);return}b=b.alternative}else if(b instanceof AST_StatementWithBody){b=b.body}else break}force_statement(self.body,output)}DEFPRINT(AST_If,function(self,output){output.print("if");output.space();output.with_parens(function(){self.condition.print(output)});output.space();if(self.alternative){make_then(self,output);output.space();output.print("else");output.space();force_statement(self.alternative,output)}else{self._do_print_body(output)}});DEFPRINT(AST_Switch,function(self,output){output.print("switch");output.space();output.with_parens(function(){self.expression.print(output)});output.space();if(self.body.length>0)output.with_block(function(){self.body.forEach(function(stmt,i){if(i)output.newline();output.indent(true);stmt.print(output)})});else output.print("{}")});AST_SwitchBranch.DEFMETHOD("_do_print_body",function(output){if(this.body.length>0){output.newline();this.body.forEach(function(stmt){output.indent();stmt.print(output);output.newline()})}});DEFPRINT(AST_Default,function(self,output){output.print("default:");self._do_print_body(output)});DEFPRINT(AST_Case,function(self,output){output.print("case");output.space();self.expression.print(output);output.print(":");self._do_print_body(output)});DEFPRINT(AST_Try,function(self,output){output.print("try");output.space();print_bracketed(self.body,output);if(self.bcatch){output.space();self.bcatch.print(output)}if(self.bfinally){output.space();self.bfinally.print(output)}});DEFPRINT(AST_Catch,function(self,output){output.print("catch");output.space();output.with_parens(function(){self.argname.print(output)});output.space();print_bracketed(self.body,output)});DEFPRINT(AST_Finally,function(self,output){output.print("finally");output.space();print_bracketed(self.body,output)});AST_Definitions.DEFMETHOD("_do_print",function(output,kind){output.print(kind);output.space();this.definitions.forEach(function(def,i){if(i)output.comma();def.print(output)});var p=output.parent();var in_for=p instanceof AST_For||p instanceof AST_ForIn;var avoid_semicolon=in_for&&p.init===this;if(!avoid_semicolon)output.semicolon()});DEFPRINT(AST_Var,function(self,output){self._do_print(output,"var")});DEFPRINT(AST_Const,function(self,output){self._do_print(output,"const")});function parenthesize_for_noin(node,output,noin){if(!noin)node.print(output);else try{node.walk(new TreeWalker(function(node){if(node instanceof AST_Binary&&node.operator=="in")throw output}));node.print(output)}catch(ex){if(ex!==output)throw ex;node.print(output,true)}}DEFPRINT(AST_VarDef,function(self,output){self.name.print(output);if(self.value){output.space();output.print("=");output.space();var p=output.parent(1);var noin=p instanceof AST_For||p instanceof AST_ForIn;parenthesize_for_noin(self.value,output,noin)}});DEFPRINT(AST_Call,function(self,output){self.expression.print(output);if(self instanceof AST_New&&no_constructor_parens(self,output))return;output.with_parens(function(){self.args.forEach(function(expr,i){if(i)output.comma();expr.print(output)})})});DEFPRINT(AST_New,function(self,output){output.print("new");output.space();AST_Call.prototype._codegen(self,output)});AST_Seq.DEFMETHOD("_do_print",function(output){this.car.print(output);if(this.cdr){output.comma();if(output.should_break()){output.newline();output.indent()}this.cdr.print(output)}});DEFPRINT(AST_Seq,function(self,output){self._do_print(output)});DEFPRINT(AST_Dot,function(self,output){var expr=self.expression;expr.print(output);if(expr instanceof AST_Number&&expr.getValue()>=0){if(!/[xa-f.]/i.test(output.last())){output.print(".")}}output.print(".");output.add_mapping(self.end);output.print_name(self.property)});DEFPRINT(AST_Sub,function(self,output){self.expression.print(output);output.print("[");self.property.print(output);output.print("]")});DEFPRINT(AST_UnaryPrefix,function(self,output){var op=self.operator;output.print(op);if(/^[a-z]/i.test(op)||/[+-]$/.test(op)&&self.expression instanceof AST_UnaryPrefix&&/^[+-]/.test(self.expression.operator)){output.space()}self.expression.print(output)});DEFPRINT(AST_UnaryPostfix,function(self,output){self.expression.print(output);output.print(self.operator)});DEFPRINT(AST_Binary,function(self,output){self.left.print(output);output.space();output.print(self.operator);if(self.operator=="<"&&self.right instanceof AST_UnaryPrefix&&self.right.operator=="!"&&self.right.expression instanceof AST_UnaryPrefix&&self.right.expression.operator=="--"){output.print(" ")}else{output.space()}self.right.print(output)});DEFPRINT(AST_Conditional,function(self,output){self.condition.print(output);output.space();output.print("?");output.space();self.consequent.print(output);output.space();output.colon();self.alternative.print(output)});DEFPRINT(AST_Array,function(self,output){output.with_square(function(){var a=self.elements,len=a.length;if(len>0)output.space();a.forEach(function(exp,i){if(i)output.comma();exp.print(output);if(i===len-1&&exp instanceof AST_Hole)output.comma()});if(len>0)output.space()})});DEFPRINT(AST_Object,function(self,output){if(self.properties.length>0)output.with_block(function(){self.properties.forEach(function(prop,i){if(i){output.print(",");output.newline()}output.indent();prop.print(output)});output.newline()});else output.print("{}")});DEFPRINT(AST_ObjectKeyVal,function(self,output){var key=self.key;var quote=self.quote;if(output.option("quote_keys")){output.print_string(key+"")}else if((typeof key=="number"||!output.option("beautify")&&+key+""==key)&&parseFloat(key)>=0){output.print(make_num(key))}else if(RESERVED_WORDS(key)?output.option("screw_ie8"):is_identifier_string(key)){output.print_name(key)}else{output.print_string(key,quote)}output.colon();self.value.print(output)});DEFPRINT(AST_ObjectSetter,function(self,output){output.print("set");output.space();self.key.print(output);self.value._do_print(output,true)});DEFPRINT(AST_ObjectGetter,function(self,output){output.print("get");output.space();self.key.print(output);self.value._do_print(output,true)});DEFPRINT(AST_Symbol,function(self,output){var def=self.definition();output.print_name(def?def.mangled_name||def.name:self.name)});DEFPRINT(AST_Undefined,function(self,output){output.print("void 0")});DEFPRINT(AST_Hole,noop);DEFPRINT(AST_Infinity,function(self,output){output.print("Infinity")});DEFPRINT(AST_NaN,function(self,output){output.print("NaN")});DEFPRINT(AST_This,function(self,output){output.print("this")});DEFPRINT(AST_Constant,function(self,output){output.print(self.getValue())});DEFPRINT(AST_String,function(self,output){output.print_string(self.getValue(),self.quote)});DEFPRINT(AST_Number,function(self,output){output.print(make_num(self.getValue()))});function regexp_safe_literal(code){return[92,47,46,43,42,63,40,41,91,93,123,125,36,94,58,124,33,10,13,0,65279,8232,8233].indexOf(code)<0}DEFPRINT(AST_RegExp,function(self,output){var str=self.getValue().toString();if(output.option("ascii_only")){str=output.to_ascii(str)}else if(output.option("unescape_regexps")){str=str.split("\\\\").map(function(str){return str.replace(/\\u[0-9a-fA-F]{4}|\\x[0-9a-fA-F]{2}/g,function(s){var code=parseInt(s.substr(2),16);return regexp_safe_literal(code)?String.fromCharCode(code):s})}).join("\\\\")}output.print(str);var p=output.parent();if(p instanceof AST_Binary&&/^in/.test(p.operator)&&p.left===self)output.print(" ")});function force_statement(stat,output){if(output.option("bracketize")){if(!stat||stat instanceof AST_EmptyStatement)output.print("{}");else if(stat instanceof AST_BlockStatement)stat.print(output);else output.with_block(function(){output.indent();stat.print(output);output.newline()})}else{if(!stat||stat instanceof AST_EmptyStatement)output.force_semicolon();else stat.print(output)}}function first_in_statement(output){var a=output.stack(),i=a.length,node=a[--i],p=a[--i];while(i>0){if(p instanceof AST_Statement&&p.body===node)return true;if(p instanceof AST_Seq&&p.car===node||p instanceof AST_Call&&p.expression===node&&!(p instanceof AST_New)||p instanceof AST_Dot&&p.expression===node||p instanceof AST_Sub&&p.expression===node||p instanceof AST_Conditional&&p.condition===node||p instanceof AST_Binary&&p.left===node||p instanceof AST_UnaryPostfix&&p.expression===node){node=p;p=a[--i]}else{return false}}}function no_constructor_parens(self,output){return self.args.length==0&&!output.option("beautify")}function best_of(a){var best=a[0],len=best.length;for(var i=1;i<a.length;++i){if(a[i].length<len){best=a[i];len=best.length}}return best}function make_num(num){var str=num.toString(10),a=[str.replace(/^0\./,".").replace("e+","e")],m;if(Math.floor(num)===num){if(num>=0){a.push("0x"+num.toString(16).toLowerCase(),"0"+num.toString(8))}else{a.push("-0x"+(-num).toString(16).toLowerCase(),"-0"+(-num).toString(8))}if(m=/^(.*?)(0+)$/.exec(num)){a.push(m[1]+"e"+m[2].length)}}else if(m=/^0?\.(0+)(.*)$/.exec(num)){a.push(m[2]+"e-"+(m[1].length+m[2].length),str.substr(str.indexOf(".")))}return best_of(a)}function make_block(stmt,output){if(stmt instanceof AST_BlockStatement){stmt.print(output);return}output.with_block(function(){output.indent();stmt.print(output);output.newline()})}function DEFMAP(nodetype,generator){nodetype.DEFMETHOD("add_source_map",function(stream){generator(this,stream)})}DEFMAP(AST_Node,noop);function basic_sourcemap_gen(self,output){output.add_mapping(self.start)}DEFMAP(AST_Directive,basic_sourcemap_gen);DEFMAP(AST_Debugger,basic_sourcemap_gen);DEFMAP(AST_Symbol,basic_sourcemap_gen);DEFMAP(AST_Jump,basic_sourcemap_gen);DEFMAP(AST_StatementWithBody,basic_sourcemap_gen);DEFMAP(AST_LabeledStatement,noop);DEFMAP(AST_Lambda,basic_sourcemap_gen);DEFMAP(AST_Switch,basic_sourcemap_gen);DEFMAP(AST_SwitchBranch,basic_sourcemap_gen);DEFMAP(AST_BlockStatement,basic_sourcemap_gen);DEFMAP(AST_Toplevel,noop);DEFMAP(AST_New,basic_sourcemap_gen);DEFMAP(AST_Try,basic_sourcemap_gen);DEFMAP(AST_Catch,basic_sourcemap_gen);DEFMAP(AST_Finally,basic_sourcemap_gen);DEFMAP(AST_Definitions,basic_sourcemap_gen);DEFMAP(AST_Constant,basic_sourcemap_gen);DEFMAP(AST_ObjectProperty,function(self,output){output.add_mapping(self.start,self.key)})})();/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function Compressor(options,false_by_default){if(!(this instanceof Compressor))return new Compressor(options,false_by_default);TreeTransformer.call(this,this.before,this.after);this.options=defaults(options,{sequences:!false_by_default,properties:!false_by_default,dead_code:!false_by_default,drop_debugger:!false_by_default,unsafe:false,unsafe_comps:false,conditionals:!false_by_default,comparisons:!false_by_default,evaluate:!false_by_default,booleans:!false_by_default,loops:!false_by_default,unused:!false_by_default,hoist_funs:!false_by_default,keep_fargs:false,keep_fnames:false,hoist_vars:false,if_return:!false_by_default,join_vars:!false_by_default,cascade:!false_by_default,side_effects:!false_by_default,pure_getters:false,pure_funcs:null,negate_iife:!false_by_default,screw_ie8:false,drop_console:false,angular:false,warnings:true,global_defs:{}},true)}Compressor.prototype=new TreeTransformer;merge(Compressor.prototype,{option:function(key){return this.options[key]},warn:function(){if(this.options.warnings)AST_Node.warn.apply(AST_Node,arguments)},before:function(node,descend,in_list){if(node._squeezed)return node;var was_scope=false;if(node instanceof AST_Scope){node=node.hoist_declarations(this);was_scope=true}descend(node,this);node=node.optimize(this);if(was_scope&&node instanceof AST_Scope){node.drop_unused(this);descend(node,this)}node._squeezed=true;return node}});(function(){function OPT(node,optimizer){node.DEFMETHOD("optimize",function(compressor){var self=this;if(self._optimized)return self;var opt=optimizer(self,compressor);opt._optimized=true;if(opt===self)return opt;return opt.transform(compressor)})}OPT(AST_Node,function(self,compressor){return self});AST_Node.DEFMETHOD("equivalent_to",function(node){return this.print_to_string()==node.print_to_string()});function make_node(ctor,orig,props){if(!props)props={};if(orig){if(!props.start)props.start=orig.start;if(!props.end)props.end=orig.end}return new ctor(props)}function make_node_from_constant(compressor,val,orig){if(val instanceof AST_Node)return val.transform(compressor);switch(typeof val){case"string":return make_node(AST_String,orig,{value:val}).optimize(compressor);case"number":return make_node(isNaN(val)?AST_NaN:AST_Number,orig,{value:val}).optimize(compressor);case"boolean":return make_node(val?AST_True:AST_False,orig).optimize(compressor);case"undefined":return make_node(AST_Undefined,orig).optimize(compressor);default:if(val===null){return make_node(AST_Null,orig,{value:null}).optimize(compressor)}if(val instanceof RegExp){return make_node(AST_RegExp,orig,{value:val}).optimize(compressor)}throw new Error(string_template("Can't handle constant of type: {type}",{type:typeof val}))}}function as_statement_array(thing){if(thing===null)return[];if(thing instanceof AST_BlockStatement)return thing.body;if(thing instanceof AST_EmptyStatement)return[];if(thing instanceof AST_Statement)return[thing];throw new Error("Can't convert thing to statement array")}function is_empty(thing){if(thing===null)return true;if(thing instanceof AST_EmptyStatement)return true;if(thing instanceof AST_BlockStatement)return thing.body.length==0;return false}function loop_body(x){if(x instanceof AST_Switch)return x;if(x instanceof AST_For||x instanceof AST_ForIn||x instanceof AST_DWLoop){return x.body instanceof AST_BlockStatement?x.body:x}return x}function tighten_body(statements,compressor){var CHANGED;do{CHANGED=false;if(compressor.option("angular")){statements=process_for_angular(statements)}statements=eliminate_spurious_blocks(statements);if(compressor.option("dead_code")){statements=eliminate_dead_code(statements,compressor)}if(compressor.option("if_return")){statements=handle_if_return(statements,compressor)}if(compressor.option("sequences")){statements=sequencesize(statements,compressor)}if(compressor.option("join_vars")){statements=join_consecutive_vars(statements,compressor)}}while(CHANGED);if(compressor.option("negate_iife")){negate_iifes(statements,compressor)}return statements;function process_for_angular(statements){function has_inject(comment){return/@ngInject/.test(comment.value)}function make_arguments_names_list(func){return func.argnames.map(function(sym){return make_node(AST_String,sym,{value:sym.name})})}function make_array(orig,elements){return make_node(AST_Array,orig,{elements:elements})}function make_injector(func,name){return make_node(AST_SimpleStatement,func,{body:make_node(AST_Assign,func,{operator:"=",left:make_node(AST_Dot,name,{expression:make_node(AST_SymbolRef,name,name),property:"$inject"}),right:make_array(func,make_arguments_names_list(func))})})}function check_expression(body){if(body&&body.args){body.args.forEach(function(argument,index,array){var comments=argument.start.comments_before;if(argument instanceof AST_Lambda&&comments.length&&has_inject(comments[0])){array[index]=make_array(argument,make_arguments_names_list(argument).concat(argument))}});if(body.expression&&body.expression.expression){check_expression(body.expression.expression)}}}return statements.reduce(function(a,stat){a.push(stat);if(stat.body&&stat.body.args){check_expression(stat.body)}else{var token=stat.start;var comments=token.comments_before;if(comments&&comments.length>0){var last=comments.pop();if(has_inject(last)){if(stat instanceof AST_Defun){a.push(make_injector(stat,stat.name))}else if(stat instanceof AST_Definitions){stat.definitions.forEach(function(def){if(def.value&&def.value instanceof AST_Lambda){a.push(make_injector(def.value,def.name))}})}else{compressor.warn("Unknown statement marked with @ngInject [{file}:{line},{col}]",token)}}}}return a},[])}function eliminate_spurious_blocks(statements){var seen_dirs=[];return statements.reduce(function(a,stat){if(stat instanceof AST_BlockStatement){CHANGED=true;a.push.apply(a,eliminate_spurious_blocks(stat.body))}else if(stat instanceof AST_EmptyStatement){CHANGED=true}else if(stat instanceof AST_Directive){if(seen_dirs.indexOf(stat.value)<0){a.push(stat);seen_dirs.push(stat.value)}else{CHANGED=true}}else{a.push(stat)}return a},[])}function handle_if_return(statements,compressor){var self=compressor.self();var in_lambda=self instanceof AST_Lambda;var ret=[];loop:for(var i=statements.length;--i>=0;){var stat=statements[i];switch(true){case in_lambda&&stat instanceof AST_Return&&!stat.value&&ret.length==0:CHANGED=true;continue loop;case stat instanceof AST_If:if(stat.body instanceof AST_Return){if((in_lambda&&ret.length==0||ret[0]instanceof AST_Return&&!ret[0].value)&&!stat.body.value&&!stat.alternative){CHANGED=true;var cond=make_node(AST_SimpleStatement,stat.condition,{body:stat.condition});ret.unshift(cond);continue loop}if(ret[0]instanceof AST_Return&&stat.body.value&&ret[0].value&&!stat.alternative){CHANGED=true;stat=stat.clone();stat.alternative=ret[0];ret[0]=stat.transform(compressor);continue loop}if((ret.length==0||ret[0]instanceof AST_Return)&&stat.body.value&&!stat.alternative&&in_lambda){CHANGED=true;stat=stat.clone();stat.alternative=ret[0]||make_node(AST_Return,stat,{value:make_node(AST_Undefined,stat)});ret[0]=stat.transform(compressor);continue loop}if(!stat.body.value&&in_lambda){CHANGED=true;stat=stat.clone();stat.condition=stat.condition.negate(compressor);stat.body=make_node(AST_BlockStatement,stat,{body:as_statement_array(stat.alternative).concat(ret)});stat.alternative=null;ret=[stat.transform(compressor)];continue loop}if(ret.length==1&&in_lambda&&ret[0]instanceof AST_SimpleStatement&&(!stat.alternative||stat.alternative instanceof AST_SimpleStatement)){CHANGED=true;ret.push(make_node(AST_Return,ret[0],{value:make_node(AST_Undefined,ret[0])}).transform(compressor));ret=as_statement_array(stat.alternative).concat(ret);ret.unshift(stat);continue loop}}var ab=aborts(stat.body);var lct=ab instanceof AST_LoopControl?compressor.loopcontrol_target(ab.label):null;if(ab&&(ab instanceof AST_Return&&!ab.value&&in_lambda||ab instanceof AST_Continue&&self===loop_body(lct)||ab instanceof AST_Break&&lct instanceof AST_BlockStatement&&self===lct)){if(ab.label){remove(ab.label.thedef.references,ab)}CHANGED=true;var body=as_statement_array(stat.body).slice(0,-1);stat=stat.clone();stat.condition=stat.condition.negate(compressor);stat.body=make_node(AST_BlockStatement,stat,{body:as_statement_array(stat.alternative).concat(ret)});stat.alternative=make_node(AST_BlockStatement,stat,{body:body});ret=[stat.transform(compressor)];continue loop}var ab=aborts(stat.alternative);var lct=ab instanceof AST_LoopControl?compressor.loopcontrol_target(ab.label):null;if(ab&&(ab instanceof AST_Return&&!ab.value&&in_lambda||ab instanceof AST_Continue&&self===loop_body(lct)||ab instanceof AST_Break&&lct instanceof AST_BlockStatement&&self===lct)){if(ab.label){remove(ab.label.thedef.references,ab)}CHANGED=true;stat=stat.clone();stat.body=make_node(AST_BlockStatement,stat.body,{body:as_statement_array(stat.body).concat(ret)});stat.alternative=make_node(AST_BlockStatement,stat.alternative,{body:as_statement_array(stat.alternative).slice(0,-1)});ret=[stat.transform(compressor)];continue loop}ret.unshift(stat);break;default:ret.unshift(stat);break}}return ret}function eliminate_dead_code(statements,compressor){var has_quit=false;var orig=statements.length;var self=compressor.self();statements=statements.reduce(function(a,stat){if(has_quit){extract_declarations_from_unreachable_code(compressor,stat,a)}else{if(stat instanceof AST_LoopControl){var lct=compressor.loopcontrol_target(stat.label);if(stat instanceof AST_Break&&lct instanceof AST_BlockStatement&&loop_body(lct)===self||stat instanceof AST_Continue&&loop_body(lct)===self){if(stat.label){remove(stat.label.thedef.references,stat)}}else{a.push(stat)}}else{a.push(stat)}if(aborts(stat))has_quit=true}return a},[]);CHANGED=statements.length!=orig;return statements}function sequencesize(statements,compressor){if(statements.length<2)return statements;var seq=[],ret=[];function push_seq(){seq=AST_Seq.from_array(seq);if(seq)ret.push(make_node(AST_SimpleStatement,seq,{body:seq}));seq=[]}statements.forEach(function(stat){if(stat instanceof AST_SimpleStatement&&seq.length<2e3)seq.push(stat.body);else push_seq(),ret.push(stat)});push_seq();ret=sequencesize_2(ret,compressor);CHANGED=ret.length!=statements.length;return ret}function sequencesize_2(statements,compressor){function cons_seq(right){ret.pop();var left=prev.body;if(left instanceof AST_Seq){left.add(right)}else{left=AST_Seq.cons(left,right)}return left.transform(compressor)}var ret=[],prev=null;statements.forEach(function(stat){if(prev){if(stat instanceof AST_For){var opera={};try{prev.body.walk(new TreeWalker(function(node){if(node instanceof AST_Binary&&node.operator=="in")throw opera}));if(stat.init&&!(stat.init instanceof AST_Definitions)){stat.init=cons_seq(stat.init)}else if(!stat.init){stat.init=prev.body;ret.pop()}}catch(ex){if(ex!==opera)throw ex}}else if(stat instanceof AST_If){stat.condition=cons_seq(stat.condition)}else if(stat instanceof AST_With){stat.expression=cons_seq(stat.expression)}else if(stat instanceof AST_Exit&&stat.value){stat.value=cons_seq(stat.value)}else if(stat instanceof AST_Exit){stat.value=cons_seq(make_node(AST_Undefined,stat))}else if(stat instanceof AST_Switch){stat.expression=cons_seq(stat.expression)}}ret.push(stat);prev=stat instanceof AST_SimpleStatement?stat:null});return ret}function join_consecutive_vars(statements,compressor){var prev=null;return statements.reduce(function(a,stat){if(stat instanceof AST_Definitions&&prev&&prev.TYPE==stat.TYPE){prev.definitions=prev.definitions.concat(stat.definitions);CHANGED=true}else if(stat instanceof AST_For&&prev instanceof AST_Definitions&&(!stat.init||stat.init.TYPE==prev.TYPE)){CHANGED=true;a.pop();if(stat.init){stat.init.definitions=prev.definitions.concat(stat.init.definitions)}else{stat.init=prev}a.push(stat);prev=stat}else{prev=stat;a.push(stat)}return a},[])}function negate_iifes(statements,compressor){statements.forEach(function(stat){if(stat instanceof AST_SimpleStatement){stat.body=function transform(thing){return thing.transform(new TreeTransformer(function(node){if(node instanceof AST_Call&&node.expression instanceof AST_Function){return make_node(AST_UnaryPrefix,node,{operator:"!",expression:node})}else if(node instanceof AST_Call){node.expression=transform(node.expression)}else if(node instanceof AST_Seq){node.car=transform(node.car)}else if(node instanceof AST_Conditional){var expr=transform(node.condition);if(expr!==node.condition){node.condition=expr;var tmp=node.consequent;node.consequent=node.alternative;node.alternative=tmp}}return node}))}(stat.body)}})}}function extract_declarations_from_unreachable_code(compressor,stat,target){compressor.warn("Dropping unreachable code [{file}:{line},{col}]",stat.start);stat.walk(new TreeWalker(function(node){if(node instanceof AST_Definitions){compressor.warn("Declarations in unreachable code! [{file}:{line},{col}]",node.start);node.remove_initializers();target.push(node);return true}if(node instanceof AST_Defun){target.push(node);return true}if(node instanceof AST_Scope){return true}}))}(function(def){var unary_bool=["!","delete"];var binary_bool=["in","instanceof","==","!=","===","!==","<","<=",">=",">"];def(AST_Node,function(){return false});def(AST_UnaryPrefix,function(){return member(this.operator,unary_bool)});def(AST_Binary,function(){return member(this.operator,binary_bool)||(this.operator=="&&"||this.operator=="||")&&this.left.is_boolean()&&this.right.is_boolean()});def(AST_Conditional,function(){return this.consequent.is_boolean()&&this.alternative.is_boolean()});def(AST_Assign,function(){return this.operator=="="&&this.right.is_boolean()});def(AST_Seq,function(){return this.cdr.is_boolean()});def(AST_True,function(){return true});def(AST_False,function(){return true})})(function(node,func){node.DEFMETHOD("is_boolean",func)});(function(def){def(AST_Node,function(){return false});def(AST_String,function(){return true});def(AST_UnaryPrefix,function(){return this.operator=="typeof"});def(AST_Binary,function(compressor){return this.operator=="+"&&(this.left.is_string(compressor)||this.right.is_string(compressor))});def(AST_Assign,function(compressor){return(this.operator=="="||this.operator=="+=")&&this.right.is_string(compressor)});def(AST_Seq,function(compressor){return this.cdr.is_string(compressor)});def(AST_Conditional,function(compressor){return this.consequent.is_string(compressor)&&this.alternative.is_string(compressor)});def(AST_Call,function(compressor){return compressor.option("unsafe")&&this.expression instanceof AST_SymbolRef&&this.expression.name=="String"&&this.expression.undeclared()})})(function(node,func){node.DEFMETHOD("is_string",func)});function best_of(ast1,ast2){return ast1.print_to_string().length>ast2.print_to_string().length?ast2:ast1}(function(def){AST_Node.DEFMETHOD("evaluate",function(compressor){if(!compressor.option("evaluate"))return[this];try{var val=this._eval(compressor);return[best_of(make_node_from_constant(compressor,val,this),this),val]}catch(ex){if(ex!==def)throw ex;return[this]}});def(AST_Statement,function(){throw new Error(string_template("Cannot evaluate a statement [{file}:{line},{col}]",this.start))});def(AST_Function,function(){throw def});function ev(node,compressor){if(!compressor)throw new Error("Compressor must be passed");return node._eval(compressor)}def(AST_Node,function(){throw def});def(AST_Constant,function(){return this.getValue()});def(AST_UnaryPrefix,function(compressor){var e=this.expression;switch(this.operator){case"!":return!ev(e,compressor);case"typeof":if(e instanceof AST_Function)return typeof function(){};e=ev(e,compressor);if(e instanceof RegExp)throw def;return typeof e;case"void":return void ev(e,compressor);case"~":return~ev(e,compressor);case"-":e=ev(e,compressor);if(e===0)throw def;return-e;case"+":return+ev(e,compressor)}throw def});def(AST_Binary,function(c){var left=this.left,right=this.right;switch(this.operator){case"&&":return ev(left,c)&&ev(right,c);case"||":return ev(left,c)||ev(right,c);case"|":return ev(left,c)|ev(right,c);case"&":return ev(left,c)&ev(right,c);case"^":return ev(left,c)^ev(right,c);case"+":return ev(left,c)+ev(right,c);case"*":return ev(left,c)*ev(right,c);case"/":return ev(left,c)/ev(right,c);case"%":return ev(left,c)%ev(right,c);case"-":return ev(left,c)-ev(right,c);case"<<":return ev(left,c)<<ev(right,c);case">>":return ev(left,c)>>ev(right,c);case">>>":return ev(left,c)>>>ev(right,c);case"==":return ev(left,c)==ev(right,c);case"===":return ev(left,c)===ev(right,c);case"!=":return ev(left,c)!=ev(right,c);case"!==":return ev(left,c)!==ev(right,c);case"<":return ev(left,c)<ev(right,c);case"<=":return ev(left,c)<=ev(right,c);case">":return ev(left,c)>ev(right,c);case">=":return ev(left,c)>=ev(right,c);case"in":return ev(left,c)in ev(right,c);case"instanceof":return ev(left,c)instanceof ev(right,c)}throw def});def(AST_Conditional,function(compressor){return ev(this.condition,compressor)?ev(this.consequent,compressor):ev(this.alternative,compressor)});def(AST_SymbolRef,function(compressor){var d=this.definition();if(d&&d.constant&&d.init)return ev(d.init,compressor);throw def});def(AST_Dot,function(compressor){if(compressor.option("unsafe")&&this.property=="length"){var str=ev(this.expression,compressor);if(typeof str=="string")return str.length}throw def})})(function(node,func){node.DEFMETHOD("_eval",func)});(function(def){function basic_negation(exp){return make_node(AST_UnaryPrefix,exp,{operator:"!",expression:exp})}def(AST_Node,function(){return basic_negation(this)});def(AST_Statement,function(){throw new Error("Cannot negate a statement")});def(AST_Function,function(){return basic_negation(this)});def(AST_UnaryPrefix,function(){if(this.operator=="!")return this.expression;return basic_negation(this)});def(AST_Seq,function(compressor){var self=this.clone();self.cdr=self.cdr.negate(compressor);return self});def(AST_Conditional,function(compressor){var self=this.clone();self.consequent=self.consequent.negate(compressor);self.alternative=self.alternative.negate(compressor);return best_of(basic_negation(this),self)});def(AST_Binary,function(compressor){var self=this.clone(),op=this.operator;if(compressor.option("unsafe_comps")){switch(op){case"<=":self.operator=">";return self;case"<":self.operator=">=";return self;case">=":self.operator="<";return self;case">":self.operator="<=";return self}}switch(op){case"==":self.operator="!=";return self;case"!=":self.operator="==";return self;case"===":self.operator="!==";return self;case"!==":self.operator="===";return self;case"&&":self.operator="||";self.left=self.left.negate(compressor);self.right=self.right.negate(compressor);return best_of(basic_negation(this),self);case"||":self.operator="&&";self.left=self.left.negate(compressor);self.right=self.right.negate(compressor);return best_of(basic_negation(this),self)}return basic_negation(this)})})(function(node,func){node.DEFMETHOD("negate",function(compressor){return func.call(this,compressor)})});(function(def){def(AST_Node,function(compressor){return true});def(AST_EmptyStatement,function(compressor){return false});def(AST_Constant,function(compressor){return false});def(AST_This,function(compressor){return false});def(AST_Call,function(compressor){var pure=compressor.option("pure_funcs");if(!pure)return true;return pure.indexOf(this.expression.print_to_string())<0});def(AST_Block,function(compressor){for(var i=this.body.length;--i>=0;){if(this.body[i].has_side_effects(compressor))return true}return false});def(AST_SimpleStatement,function(compressor){return this.body.has_side_effects(compressor)});def(AST_Defun,function(compressor){return true});def(AST_Function,function(compressor){return false});def(AST_Binary,function(compressor){return this.left.has_side_effects(compressor)||this.right.has_side_effects(compressor)});def(AST_Assign,function(compressor){return true});def(AST_Conditional,function(compressor){return this.condition.has_side_effects(compressor)||this.consequent.has_side_effects(compressor)||this.alternative.has_side_effects(compressor)});def(AST_Unary,function(compressor){return this.operator=="delete"||this.operator=="++"||this.operator=="--"||this.expression.has_side_effects(compressor)});def(AST_SymbolRef,function(compressor){return this.global()&&this.undeclared()});def(AST_Object,function(compressor){for(var i=this.properties.length;--i>=0;)if(this.properties[i].has_side_effects(compressor))return true;return false});def(AST_ObjectProperty,function(compressor){return this.value.has_side_effects(compressor)});def(AST_Array,function(compressor){for(var i=this.elements.length;--i>=0;)if(this.elements[i].has_side_effects(compressor))return true;return false});def(AST_Dot,function(compressor){if(!compressor.option("pure_getters"))return true;return this.expression.has_side_effects(compressor)});def(AST_Sub,function(compressor){if(!compressor.option("pure_getters"))return true;return this.expression.has_side_effects(compressor)||this.property.has_side_effects(compressor)});def(AST_PropAccess,function(compressor){return!compressor.option("pure_getters")});def(AST_Seq,function(compressor){return this.car.has_side_effects(compressor)||this.cdr.has_side_effects(compressor)})})(function(node,func){node.DEFMETHOD("has_side_effects",func)});function aborts(thing){return thing&&thing.aborts()}(function(def){def(AST_Statement,function(){return null});def(AST_Jump,function(){return this});function block_aborts(){var n=this.body.length;return n>0&&aborts(this.body[n-1])}def(AST_BlockStatement,block_aborts);def(AST_SwitchBranch,block_aborts);def(AST_If,function(){return this.alternative&&aborts(this.body)&&aborts(this.alternative)&&this})})(function(node,func){node.DEFMETHOD("aborts",func)});OPT(AST_Directive,function(self,compressor){if(self.scope.has_directive(self.value)!==self.scope){return make_node(AST_EmptyStatement,self)}return self});OPT(AST_Debugger,function(self,compressor){if(compressor.option("drop_debugger"))return make_node(AST_EmptyStatement,self);return self});OPT(AST_LabeledStatement,function(self,compressor){if(self.body instanceof AST_Break&&compressor.loopcontrol_target(self.body.label)===self.body){return make_node(AST_EmptyStatement,self)}return self.label.references.length==0?self.body:self});OPT(AST_Block,function(self,compressor){self.body=tighten_body(self.body,compressor);return self});OPT(AST_BlockStatement,function(self,compressor){self.body=tighten_body(self.body,compressor);switch(self.body.length){case 1:return self.body[0];case 0:return make_node(AST_EmptyStatement,self)}return self});AST_Scope.DEFMETHOD("drop_unused",function(compressor){var self=this;if(compressor.option("unused")&&!(self instanceof AST_Toplevel)&&!self.uses_eval){var in_use=[];var initializations=new Dictionary;var scope=this;var tw=new TreeWalker(function(node,descend){if(node!==self){if(node instanceof AST_Defun){initializations.add(node.name.name,node);return true}if(node instanceof AST_Definitions&&scope===self){node.definitions.forEach(function(def){if(def.value){initializations.add(def.name.name,def.value);if(def.value.has_side_effects(compressor)){def.value.walk(tw)}}});return true}if(node instanceof AST_SymbolRef){push_uniq(in_use,node.definition());return true}if(node instanceof AST_Scope){var save_scope=scope;scope=node;descend();scope=save_scope;return true}}});self.walk(tw);for(var i=0;i<in_use.length;++i){in_use[i].orig.forEach(function(decl){var init=initializations.get(decl.name);if(init)init.forEach(function(init){var tw=new TreeWalker(function(node){if(node instanceof AST_SymbolRef){push_uniq(in_use,node.definition())}});init.walk(tw)})})}var tt=new TreeTransformer(function before(node,descend,in_list){if(node instanceof AST_Lambda&&!(node instanceof AST_Accessor)){if(compressor.option("unsafe")&&!compressor.option("keep_fargs")){for(var a=node.argnames,i=a.length;--i>=0;){var sym=a[i];if(sym.unreferenced()){a.pop();compressor.warn("Dropping unused function argument {name} [{file}:{line},{col}]",{name:sym.name,file:sym.start.file,line:sym.start.line,col:sym.start.col})}else break}}}if(node instanceof AST_Defun&&node!==self){if(!member(node.name.definition(),in_use)){compressor.warn("Dropping unused function {name} [{file}:{line},{col}]",{name:node.name.name,file:node.name.start.file,line:node.name.start.line,col:node.name.start.col});return make_node(AST_EmptyStatement,node)}return node}if(node instanceof AST_Definitions&&!(tt.parent()instanceof AST_ForIn)){var def=node.definitions.filter(function(def){if(member(def.name.definition(),in_use))return true;var w={name:def.name.name,file:def.name.start.file,line:def.name.start.line,col:def.name.start.col};if(def.value&&def.value.has_side_effects(compressor)){def._unused_side_effects=true;compressor.warn("Side effects in initialization of unused variable {name} [{file}:{line},{col}]",w);return true}compressor.warn("Dropping unused variable {name} [{file}:{line},{col}]",w);return false});def=mergeSort(def,function(a,b){if(!a.value&&b.value)return-1;if(!b.value&&a.value)return 1;return 0});var side_effects=[];for(var i=0;i<def.length;){var x=def[i];if(x._unused_side_effects){side_effects.push(x.value);def.splice(i,1)}else{if(side_effects.length>0){side_effects.push(x.value);x.value=AST_Seq.from_array(side_effects);side_effects=[]}++i}}if(side_effects.length>0){side_effects=make_node(AST_BlockStatement,node,{body:[make_node(AST_SimpleStatement,node,{body:AST_Seq.from_array(side_effects)})]})}else{side_effects=null}if(def.length==0&&!side_effects){return make_node(AST_EmptyStatement,node)}if(def.length==0){return in_list?MAP.splice(side_effects.body):side_effects}node.definitions=def;if(side_effects){side_effects.body.unshift(node);return in_list?MAP.splice(side_effects.body):side_effects}return node}if(node instanceof AST_For){descend(node,this);if(node.init instanceof AST_BlockStatement){var body=node.init.body.slice(0,-1);node.init=node.init.body.slice(-1)[0].body;body.push(node);return in_list?MAP.splice(body):make_node(AST_BlockStatement,node,{body:body})}}if(node instanceof AST_Scope&&node!==self)return node});self.transform(tt)}});AST_Scope.DEFMETHOD("hoist_declarations",function(compressor){var hoist_funs=compressor.option("hoist_funs");var hoist_vars=compressor.option("hoist_vars");var self=this;if(hoist_funs||hoist_vars){var dirs=[];var hoisted=[];var vars=new Dictionary,vars_found=0,var_decl=0;self.walk(new TreeWalker(function(node){if(node instanceof AST_Scope&&node!==self)return true;if(node instanceof AST_Var){++var_decl;return true}}));hoist_vars=hoist_vars&&var_decl>1;var tt=new TreeTransformer(function before(node){if(node!==self){if(node instanceof AST_Directive){dirs.push(node);return make_node(AST_EmptyStatement,node)}if(node instanceof AST_Defun&&hoist_funs){hoisted.push(node);return make_node(AST_EmptyStatement,node)}if(node instanceof AST_Var&&hoist_vars){node.definitions.forEach(function(def){vars.set(def.name.name,def);++vars_found});var seq=node.to_assignments();var p=tt.parent();if(p instanceof AST_ForIn&&p.init===node){if(seq==null)return node.definitions[0].name;return seq}if(p instanceof AST_For&&p.init===node){return seq}if(!seq)return make_node(AST_EmptyStatement,node);return make_node(AST_SimpleStatement,node,{body:seq})}if(node instanceof AST_Scope)return node}});self=self.transform(tt);if(vars_found>0){var defs=[];vars.each(function(def,name){if(self instanceof AST_Lambda&&find_if(function(x){return x.name==def.name.name},self.argnames)){vars.del(name)}else{def=def.clone();def.value=null;defs.push(def);vars.set(name,def)}});if(defs.length>0){for(var i=0;i<self.body.length;){if(self.body[i]instanceof AST_SimpleStatement){var expr=self.body[i].body,sym,assign;if(expr instanceof AST_Assign&&expr.operator=="="&&(sym=expr.left)instanceof AST_Symbol&&vars.has(sym.name)){var def=vars.get(sym.name);if(def.value)break;def.value=expr.right;remove(defs,def);defs.push(def);self.body.splice(i,1);continue}if(expr instanceof AST_Seq&&(assign=expr.car)instanceof AST_Assign&&assign.operator=="="&&(sym=assign.left)instanceof AST_Symbol&&vars.has(sym.name)){var def=vars.get(sym.name);if(def.value)break;def.value=assign.right;remove(defs,def);defs.push(def);self.body[i].body=expr.cdr;continue}}if(self.body[i]instanceof AST_EmptyStatement){self.body.splice(i,1);continue}if(self.body[i]instanceof AST_BlockStatement){var tmp=[i,1].concat(self.body[i].body);self.body.splice.apply(self.body,tmp);continue}break}defs=make_node(AST_Var,self,{definitions:defs});hoisted.push(defs)}}self.body=dirs.concat(hoisted,self.body)}return self});OPT(AST_SimpleStatement,function(self,compressor){if(compressor.option("side_effects")){if(!self.body.has_side_effects(compressor)){compressor.warn("Dropping side-effect-free statement [{file}:{line},{col}]",self.start);return make_node(AST_EmptyStatement,self)}}return self});OPT(AST_DWLoop,function(self,compressor){var cond=self.condition.evaluate(compressor);self.condition=cond[0];if(!compressor.option("loops"))return self;if(cond.length>1){if(cond[1]){return make_node(AST_For,self,{body:self.body})}else if(self instanceof AST_While){if(compressor.option("dead_code")){var a=[];extract_declarations_from_unreachable_code(compressor,self.body,a);return make_node(AST_BlockStatement,self,{body:a})}}}return self});function if_break_in_loop(self,compressor){function drop_it(rest){rest=as_statement_array(rest);if(self.body instanceof AST_BlockStatement){self.body=self.body.clone();self.body.body=rest.concat(self.body.body.slice(1));self.body=self.body.transform(compressor)}else{self.body=make_node(AST_BlockStatement,self.body,{body:rest}).transform(compressor)}if_break_in_loop(self,compressor)}var first=self.body instanceof AST_BlockStatement?self.body.body[0]:self.body;if(first instanceof AST_If){if(first.body instanceof AST_Break&&compressor.loopcontrol_target(first.body.label)===self){if(self.condition){self.condition=make_node(AST_Binary,self.condition,{left:self.condition,operator:"&&",right:first.condition.negate(compressor)})}else{self.condition=first.condition.negate(compressor)}drop_it(first.alternative)}else if(first.alternative instanceof AST_Break&&compressor.loopcontrol_target(first.alternative.label)===self){if(self.condition){self.condition=make_node(AST_Binary,self.condition,{left:self.condition,operator:"&&",right:first.condition})}else{self.condition=first.condition}drop_it(first.body)}}}OPT(AST_While,function(self,compressor){if(!compressor.option("loops"))return self;self=AST_DWLoop.prototype.optimize.call(self,compressor);if(self instanceof AST_While){if_break_in_loop(self,compressor);self=make_node(AST_For,self,self).transform(compressor)}return self});OPT(AST_For,function(self,compressor){var cond=self.condition;if(cond){cond=cond.evaluate(compressor);self.condition=cond[0]}if(!compressor.option("loops"))return self;if(cond){if(cond.length>1&&!cond[1]){if(compressor.option("dead_code")){var a=[];if(self.init instanceof AST_Statement){a.push(self.init)}else if(self.init){a.push(make_node(AST_SimpleStatement,self.init,{body:self.init}))}extract_declarations_from_unreachable_code(compressor,self.body,a);return make_node(AST_BlockStatement,self,{body:a})}}}if_break_in_loop(self,compressor);return self});OPT(AST_If,function(self,compressor){if(!compressor.option("conditionals"))return self;var cond=self.condition.evaluate(compressor);self.condition=cond[0];if(cond.length>1){if(cond[1]){compressor.warn("Condition always true [{file}:{line},{col}]",self.condition.start);if(compressor.option("dead_code")){var a=[];if(self.alternative){extract_declarations_from_unreachable_code(compressor,self.alternative,a)}a.push(self.body);return make_node(AST_BlockStatement,self,{body:a}).transform(compressor)}}else{compressor.warn("Condition always false [{file}:{line},{col}]",self.condition.start);if(compressor.option("dead_code")){var a=[];extract_declarations_from_unreachable_code(compressor,self.body,a);if(self.alternative)a.push(self.alternative);return make_node(AST_BlockStatement,self,{
body:a}).transform(compressor)}}}if(is_empty(self.alternative))self.alternative=null;var negated=self.condition.negate(compressor);var negated_is_best=best_of(self.condition,negated)===negated;if(self.alternative&&negated_is_best){negated_is_best=false;self.condition=negated;var tmp=self.body;self.body=self.alternative||make_node(AST_EmptyStatement);self.alternative=tmp}if(is_empty(self.body)&&is_empty(self.alternative)){return make_node(AST_SimpleStatement,self.condition,{body:self.condition}).transform(compressor)}if(self.body instanceof AST_SimpleStatement&&self.alternative instanceof AST_SimpleStatement){return make_node(AST_SimpleStatement,self,{body:make_node(AST_Conditional,self,{condition:self.condition,consequent:self.body.body,alternative:self.alternative.body})}).transform(compressor)}if(is_empty(self.alternative)&&self.body instanceof AST_SimpleStatement){if(negated_is_best)return make_node(AST_SimpleStatement,self,{body:make_node(AST_Binary,self,{operator:"||",left:negated,right:self.body.body})}).transform(compressor);return make_node(AST_SimpleStatement,self,{body:make_node(AST_Binary,self,{operator:"&&",left:self.condition,right:self.body.body})}).transform(compressor)}if(self.body instanceof AST_EmptyStatement&&self.alternative&&self.alternative instanceof AST_SimpleStatement){return make_node(AST_SimpleStatement,self,{body:make_node(AST_Binary,self,{operator:"||",left:self.condition,right:self.alternative.body})}).transform(compressor)}if(self.body instanceof AST_Exit&&self.alternative instanceof AST_Exit&&self.body.TYPE==self.alternative.TYPE){return make_node(self.body.CTOR,self,{value:make_node(AST_Conditional,self,{condition:self.condition,consequent:self.body.value||make_node(AST_Undefined,self.body).optimize(compressor),alternative:self.alternative.value||make_node(AST_Undefined,self.alternative).optimize(compressor)})}).transform(compressor)}if(self.body instanceof AST_If&&!self.body.alternative&&!self.alternative){self.condition=make_node(AST_Binary,self.condition,{operator:"&&",left:self.condition,right:self.body.condition}).transform(compressor);self.body=self.body.body}if(aborts(self.body)){if(self.alternative){var alt=self.alternative;self.alternative=null;return make_node(AST_BlockStatement,self,{body:[self,alt]}).transform(compressor)}}if(aborts(self.alternative)){var body=self.body;self.body=self.alternative;self.condition=negated_is_best?negated:self.condition.negate(compressor);self.alternative=null;return make_node(AST_BlockStatement,self,{body:[self,body]}).transform(compressor)}return self});OPT(AST_Switch,function(self,compressor){if(self.body.length==0&&compressor.option("conditionals")){return make_node(AST_SimpleStatement,self,{body:self.expression}).transform(compressor)}for(;;){var last_branch=self.body[self.body.length-1];if(last_branch){var stat=last_branch.body[last_branch.body.length-1];if(stat instanceof AST_Break&&loop_body(compressor.loopcontrol_target(stat.label))===self)last_branch.body.pop();if(last_branch instanceof AST_Default&&last_branch.body.length==0){self.body.pop();continue}}break}var exp=self.expression.evaluate(compressor);out:if(exp.length==2)try{self.expression=exp[0];if(!compressor.option("dead_code"))break out;var value=exp[1];var in_if=false;var in_block=false;var started=false;var stopped=false;var ruined=false;var tt=new TreeTransformer(function(node,descend,in_list){if(node instanceof AST_Lambda||node instanceof AST_SimpleStatement){return node}else if(node instanceof AST_Switch&&node===self){node=node.clone();descend(node,this);return ruined?node:make_node(AST_BlockStatement,node,{body:node.body.reduce(function(a,branch){return a.concat(branch.body)},[])}).transform(compressor)}else if(node instanceof AST_If||node instanceof AST_Try){var save=in_if;in_if=!in_block;descend(node,this);in_if=save;return node}else if(node instanceof AST_StatementWithBody||node instanceof AST_Switch){var save=in_block;in_block=true;descend(node,this);in_block=save;return node}else if(node instanceof AST_Break&&this.loopcontrol_target(node.label)===self){if(in_if){ruined=true;return node}if(in_block)return node;stopped=true;return in_list?MAP.skip:make_node(AST_EmptyStatement,node)}else if(node instanceof AST_SwitchBranch&&this.parent()===self){if(stopped)return MAP.skip;if(node instanceof AST_Case){var exp=node.expression.evaluate(compressor);if(exp.length<2){throw self}if(exp[1]===value||started){started=true;if(aborts(node))stopped=true;descend(node,this);return node}return MAP.skip}descend(node,this);return node}});tt.stack=compressor.stack.slice();self=self.transform(tt)}catch(ex){if(ex!==self)throw ex}return self});OPT(AST_Case,function(self,compressor){self.body=tighten_body(self.body,compressor);return self});OPT(AST_Try,function(self,compressor){self.body=tighten_body(self.body,compressor);return self});AST_Definitions.DEFMETHOD("remove_initializers",function(){this.definitions.forEach(function(def){def.value=null})});AST_Definitions.DEFMETHOD("to_assignments",function(){var assignments=this.definitions.reduce(function(a,def){if(def.value){var name=make_node(AST_SymbolRef,def.name,def.name);a.push(make_node(AST_Assign,def,{operator:"=",left:name,right:def.value}))}return a},[]);if(assignments.length==0)return null;return AST_Seq.from_array(assignments)});OPT(AST_Definitions,function(self,compressor){if(self.definitions.length==0)return make_node(AST_EmptyStatement,self);return self});OPT(AST_Function,function(self,compressor){self=AST_Lambda.prototype.optimize.call(self,compressor);if(compressor.option("unused")&&!compressor.option("keep_fnames")){if(self.name&&self.name.unreferenced()){self.name=null}}return self});OPT(AST_Call,function(self,compressor){if(compressor.option("unsafe")){var exp=self.expression;if(exp instanceof AST_SymbolRef&&exp.undeclared()){switch(exp.name){case"Array":if(self.args.length!=1){return make_node(AST_Array,self,{elements:self.args}).transform(compressor)}break;case"Object":if(self.args.length==0){return make_node(AST_Object,self,{properties:[]})}break;case"String":if(self.args.length==0)return make_node(AST_String,self,{value:""});if(self.args.length<=1)return make_node(AST_Binary,self,{left:self.args[0],operator:"+",right:make_node(AST_String,self,{value:""})}).transform(compressor);break;case"Number":if(self.args.length==0)return make_node(AST_Number,self,{value:0});if(self.args.length==1)return make_node(AST_UnaryPrefix,self,{expression:self.args[0],operator:"+"}).transform(compressor);case"Boolean":if(self.args.length==0)return make_node(AST_False,self);if(self.args.length==1)return make_node(AST_UnaryPrefix,self,{expression:make_node(AST_UnaryPrefix,null,{expression:self.args[0],operator:"!"}),operator:"!"}).transform(compressor);break;case"Function":if(self.args.length==0)return make_node(AST_Function,self,{argnames:[],body:[]});if(all(self.args,function(x){return x instanceof AST_String})){try{var code="(function("+self.args.slice(0,-1).map(function(arg){return arg.value}).join(",")+"){"+self.args[self.args.length-1].value+"})()";var ast=parse(code);ast.figure_out_scope({screw_ie8:compressor.option("screw_ie8")});var comp=new Compressor(compressor.options);ast=ast.transform(comp);ast.figure_out_scope({screw_ie8:compressor.option("screw_ie8")});ast.mangle_names();var fun;try{ast.walk(new TreeWalker(function(node){if(node instanceof AST_Lambda){fun=node;throw ast}}))}catch(ex){if(ex!==ast)throw ex}if(!fun)return self;var args=fun.argnames.map(function(arg,i){return make_node(AST_String,self.args[i],{value:arg.print_to_string()})});var code=OutputStream();AST_BlockStatement.prototype._codegen.call(fun,fun,code);code=code.toString().replace(/^\{|\}$/g,"");args.push(make_node(AST_String,self.args[self.args.length-1],{value:code}));self.args=args;return self}catch(ex){if(ex instanceof JS_Parse_Error){compressor.warn("Error parsing code passed to new Function [{file}:{line},{col}]",self.args[self.args.length-1].start);compressor.warn(ex.toString())}else{console.log(ex);throw ex}}}break}}else if(exp instanceof AST_Dot&&exp.property=="toString"&&self.args.length==0){return make_node(AST_Binary,self,{left:make_node(AST_String,self,{value:""}),operator:"+",right:exp.expression}).transform(compressor)}else if(exp instanceof AST_Dot&&exp.expression instanceof AST_Array&&exp.property=="join")EXIT:{var separator=self.args.length==0?",":self.args[0].evaluate(compressor)[1];if(separator==null)break EXIT;var elements=exp.expression.elements.reduce(function(a,el){el=el.evaluate(compressor);if(a.length==0||el.length==1){a.push(el)}else{var last=a[a.length-1];if(last.length==2){var val=""+last[1]+separator+el[1];a[a.length-1]=[make_node_from_constant(compressor,val,last[0]),val]}else{a.push(el)}}return a},[]);if(elements.length==0)return make_node(AST_String,self,{value:""});if(elements.length==1)return elements[0][0];if(separator==""){var first;if(elements[0][0]instanceof AST_String||elements[1][0]instanceof AST_String){first=elements.shift()[0]}else{first=make_node(AST_String,self,{value:""})}return elements.reduce(function(prev,el){return make_node(AST_Binary,el[0],{operator:"+",left:prev,right:el[0]})},first).transform(compressor)}var node=self.clone();node.expression=node.expression.clone();node.expression.expression=node.expression.expression.clone();node.expression.expression.elements=elements.map(function(el){return el[0]});return best_of(self,node)}}if(compressor.option("side_effects")){if(self.expression instanceof AST_Function&&self.args.length==0&&!AST_Block.prototype.has_side_effects.call(self.expression,compressor)){return make_node(AST_Undefined,self).transform(compressor)}}if(compressor.option("drop_console")){if(self.expression instanceof AST_PropAccess){var name=self.expression.expression;while(name.expression){name=name.expression}if(name instanceof AST_SymbolRef&&name.name=="console"&&name.undeclared()){return make_node(AST_Undefined,self).transform(compressor)}}}return self.evaluate(compressor)[0]});OPT(AST_New,function(self,compressor){if(compressor.option("unsafe")){var exp=self.expression;if(exp instanceof AST_SymbolRef&&exp.undeclared()){switch(exp.name){case"Object":case"RegExp":case"Function":case"Error":case"Array":return make_node(AST_Call,self,self).transform(compressor)}}}return self});OPT(AST_Seq,function(self,compressor){if(!compressor.option("side_effects"))return self;if(!self.car.has_side_effects(compressor)){var p;if(!(self.cdr instanceof AST_SymbolRef&&self.cdr.name=="eval"&&self.cdr.undeclared()&&(p=compressor.parent())instanceof AST_Call&&p.expression===self)){return self.cdr}}if(compressor.option("cascade")){if(self.car instanceof AST_Assign&&!self.car.left.has_side_effects(compressor)){if(self.car.left.equivalent_to(self.cdr)){return self.car}if(self.cdr instanceof AST_Call&&self.cdr.expression.equivalent_to(self.car.left)){self.cdr.expression=self.car;return self.cdr}}if(!self.car.has_side_effects(compressor)&&!self.cdr.has_side_effects(compressor)&&self.car.equivalent_to(self.cdr)){return self.car}}if(self.cdr instanceof AST_UnaryPrefix&&self.cdr.operator=="void"&&!self.cdr.expression.has_side_effects(compressor)){self.cdr.expression=self.car;return self.cdr}if(self.cdr instanceof AST_Undefined){return make_node(AST_UnaryPrefix,self,{operator:"void",expression:self.car})}return self});AST_Unary.DEFMETHOD("lift_sequences",function(compressor){if(compressor.option("sequences")){if(this.expression instanceof AST_Seq){var seq=this.expression;var x=seq.to_array();this.expression=x.pop();x.push(this);seq=AST_Seq.from_array(x).transform(compressor);return seq}}return this});OPT(AST_UnaryPostfix,function(self,compressor){return self.lift_sequences(compressor)});OPT(AST_UnaryPrefix,function(self,compressor){self=self.lift_sequences(compressor);var e=self.expression;if(compressor.option("booleans")&&compressor.in_boolean_context()){switch(self.operator){case"!":if(e instanceof AST_UnaryPrefix&&e.operator=="!"){return e.expression}break;case"typeof":compressor.warn("Boolean expression always true [{file}:{line},{col}]",self.start);return make_node(AST_True,self)}if(e instanceof AST_Binary&&self.operator=="!"){self=best_of(self,e.negate(compressor))}}return self.evaluate(compressor)[0]});function has_side_effects_or_prop_access(node,compressor){var save_pure_getters=compressor.option("pure_getters");compressor.options.pure_getters=false;var ret=node.has_side_effects(compressor);compressor.options.pure_getters=save_pure_getters;return ret}AST_Binary.DEFMETHOD("lift_sequences",function(compressor){if(compressor.option("sequences")){if(this.left instanceof AST_Seq){var seq=this.left;var x=seq.to_array();this.left=x.pop();x.push(this);seq=AST_Seq.from_array(x).transform(compressor);return seq}if(this.right instanceof AST_Seq&&this instanceof AST_Assign&&!has_side_effects_or_prop_access(this.left,compressor)){var seq=this.right;var x=seq.to_array();this.right=x.pop();x.push(this);seq=AST_Seq.from_array(x).transform(compressor);return seq}}return this});var commutativeOperators=makePredicate("== === != !== * & | ^");OPT(AST_Binary,function(self,compressor){var reverse=compressor.has_directive("use asm")?noop:function(op,force){if(force||!(self.left.has_side_effects(compressor)||self.right.has_side_effects(compressor))){if(op)self.operator=op;var tmp=self.left;self.left=self.right;self.right=tmp}};if(commutativeOperators(self.operator)){if(self.right instanceof AST_Constant&&!(self.left instanceof AST_Constant)){if(!(self.left instanceof AST_Binary&&PRECEDENCE[self.left.operator]>=PRECEDENCE[self.operator])){reverse(null,true)}}if(/^[!=]==?$/.test(self.operator)){if(self.left instanceof AST_SymbolRef&&self.right instanceof AST_Conditional){if(self.right.consequent instanceof AST_SymbolRef&&self.right.consequent.definition()===self.left.definition()){if(/^==/.test(self.operator))return self.right.condition;if(/^!=/.test(self.operator))return self.right.condition.negate(compressor)}if(self.right.alternative instanceof AST_SymbolRef&&self.right.alternative.definition()===self.left.definition()){if(/^==/.test(self.operator))return self.right.condition.negate(compressor);if(/^!=/.test(self.operator))return self.right.condition}}if(self.right instanceof AST_SymbolRef&&self.left instanceof AST_Conditional){if(self.left.consequent instanceof AST_SymbolRef&&self.left.consequent.definition()===self.right.definition()){if(/^==/.test(self.operator))return self.left.condition;if(/^!=/.test(self.operator))return self.left.condition.negate(compressor)}if(self.left.alternative instanceof AST_SymbolRef&&self.left.alternative.definition()===self.right.definition()){if(/^==/.test(self.operator))return self.left.condition.negate(compressor);if(/^!=/.test(self.operator))return self.left.condition}}}}self=self.lift_sequences(compressor);if(compressor.option("comparisons"))switch(self.operator){case"===":case"!==":if(self.left.is_string(compressor)&&self.right.is_string(compressor)||self.left.is_boolean()&&self.right.is_boolean()){self.operator=self.operator.substr(0,2)}case"==":case"!=":if(self.left instanceof AST_String&&self.left.value=="undefined"&&self.right instanceof AST_UnaryPrefix&&self.right.operator=="typeof"&&compressor.option("unsafe")){if(!(self.right.expression instanceof AST_SymbolRef)||!self.right.expression.undeclared()){self.right=self.right.expression;self.left=make_node(AST_Undefined,self.left).optimize(compressor);if(self.operator.length==2)self.operator+="="}}break}if(compressor.option("conditionals")){if(self.operator=="&&"){var ll=self.left.evaluate(compressor);var rr=self.right.evaluate(compressor);if(ll.length>1){if(ll[1]){compressor.warn("Condition left of && always true [{file}:{line},{col}]",self.start);return rr[0]}else{compressor.warn("Condition left of && always false [{file}:{line},{col}]",self.start);return ll[0]}}}else if(self.operator=="||"){var ll=self.left.evaluate(compressor);var rr=self.right.evaluate(compressor);if(ll.length>1){if(ll[1]){compressor.warn("Condition left of || always true [{file}:{line},{col}]",self.start);return ll[0]}else{compressor.warn("Condition left of || always false [{file}:{line},{col}]",self.start);return rr[0]}}}}if(compressor.option("booleans")&&compressor.in_boolean_context())switch(self.operator){case"&&":var ll=self.left.evaluate(compressor);var rr=self.right.evaluate(compressor);if(ll.length>1&&!ll[1]||rr.length>1&&!rr[1]){compressor.warn("Boolean && always false [{file}:{line},{col}]",self.start);if(self.left.has_side_effects(compressor)){return make_node(AST_Seq,self,{car:self.left,cdr:make_node(AST_False)}).optimize(compressor)}return make_node(AST_False,self)}if(ll.length>1&&ll[1]){return rr[0]}if(rr.length>1&&rr[1]){return ll[0]}break;case"||":var ll=self.left.evaluate(compressor);var rr=self.right.evaluate(compressor);if(ll.length>1&&ll[1]||rr.length>1&&rr[1]){compressor.warn("Boolean || always true [{file}:{line},{col}]",self.start);if(self.left.has_side_effects(compressor)){return make_node(AST_Seq,self,{car:self.left,cdr:make_node(AST_True)}).optimize(compressor)}return make_node(AST_True,self)}if(ll.length>1&&!ll[1]){return rr[0]}if(rr.length>1&&!rr[1]){return ll[0]}break;case"+":var ll=self.left.evaluate(compressor);var rr=self.right.evaluate(compressor);if(ll.length>1&&ll[0]instanceof AST_String&&ll[1]||rr.length>1&&rr[0]instanceof AST_String&&rr[1]){compressor.warn("+ in boolean context always true [{file}:{line},{col}]",self.start);return make_node(AST_True,self)}break}if(compressor.option("comparisons")&&self.is_boolean()){if(!(compressor.parent()instanceof AST_Binary)||compressor.parent()instanceof AST_Assign){var negated=make_node(AST_UnaryPrefix,self,{operator:"!",expression:self.negate(compressor)});self=best_of(self,negated)}switch(self.operator){case"<":reverse(">");break;case"<=":reverse(">=");break}}if(self.operator=="+"&&self.right instanceof AST_String&&self.right.getValue()===""&&self.left instanceof AST_Binary&&self.left.operator=="+"&&self.left.is_string(compressor)){return self.left}if(compressor.option("evaluate")){if(self.operator=="+"){if(self.left instanceof AST_Constant&&self.right instanceof AST_Binary&&self.right.operator=="+"&&self.right.left instanceof AST_Constant&&self.right.is_string(compressor)){self=make_node(AST_Binary,self,{operator:"+",left:make_node(AST_String,null,{value:""+self.left.getValue()+self.right.left.getValue(),start:self.left.start,end:self.right.left.end}),right:self.right.right})}if(self.right instanceof AST_Constant&&self.left instanceof AST_Binary&&self.left.operator=="+"&&self.left.right instanceof AST_Constant&&self.left.is_string(compressor)){self=make_node(AST_Binary,self,{operator:"+",left:self.left.left,right:make_node(AST_String,null,{value:""+self.left.right.getValue()+self.right.getValue(),start:self.left.right.start,end:self.right.end})})}if(self.left instanceof AST_Binary&&self.left.operator=="+"&&self.left.is_string(compressor)&&self.left.right instanceof AST_Constant&&self.right instanceof AST_Binary&&self.right.operator=="+"&&self.right.left instanceof AST_Constant&&self.right.is_string(compressor)){self=make_node(AST_Binary,self,{operator:"+",left:make_node(AST_Binary,self.left,{operator:"+",left:self.left.left,right:make_node(AST_String,null,{value:""+self.left.right.getValue()+self.right.left.getValue(),start:self.left.right.start,end:self.right.left.end})}),right:self.right.right})}}}if(self.right instanceof AST_Binary&&self.right.operator==self.operator&&(self.operator=="&&"||self.operator=="||")){self.left=make_node(AST_Binary,self.left,{operator:self.operator,left:self.left,right:self.right.left});self.right=self.right.right;return self.transform(compressor)}return self.evaluate(compressor)[0]});OPT(AST_SymbolRef,function(self,compressor){if(self.undeclared()){var defines=compressor.option("global_defs");if(defines&&defines.hasOwnProperty(self.name)){return make_node_from_constant(compressor,defines[self.name],self)}switch(self.name){case"undefined":return make_node(AST_Undefined,self);case"NaN":return make_node(AST_NaN,self).transform(compressor);case"Infinity":return make_node(AST_Infinity,self).transform(compressor)}}return self});OPT(AST_Infinity,function(self,compressor){return make_node(AST_Binary,self,{operator:"/",left:make_node(AST_Number,self,{value:1}),right:make_node(AST_Number,self,{value:0})})});OPT(AST_Undefined,function(self,compressor){if(compressor.option("unsafe")){var scope=compressor.find_parent(AST_Scope);var undef=scope.find_variable("undefined");if(undef){var ref=make_node(AST_SymbolRef,self,{name:"undefined",scope:scope,thedef:undef});ref.reference();return ref}}return self});var ASSIGN_OPS=["+","-","/","*","%",">>","<<",">>>","|","^","&"];OPT(AST_Assign,function(self,compressor){self=self.lift_sequences(compressor);if(self.operator=="="&&self.left instanceof AST_SymbolRef&&self.right instanceof AST_Binary&&self.right.left instanceof AST_SymbolRef&&self.right.left.name==self.left.name&&member(self.right.operator,ASSIGN_OPS)){self.operator=self.right.operator+"=";self.right=self.right.right}return self});OPT(AST_Conditional,function(self,compressor){if(!compressor.option("conditionals"))return self;if(self.condition instanceof AST_Seq){var car=self.condition.car;self.condition=self.condition.cdr;return AST_Seq.cons(car,self)}var cond=self.condition.evaluate(compressor);if(cond.length>1){if(cond[1]){compressor.warn("Condition always true [{file}:{line},{col}]",self.start);return self.consequent}else{compressor.warn("Condition always false [{file}:{line},{col}]",self.start);return self.alternative}}var negated=cond[0].negate(compressor);if(best_of(cond[0],negated)===negated){self=make_node(AST_Conditional,self,{condition:negated,consequent:self.alternative,alternative:self.consequent})}var consequent=self.consequent;var alternative=self.alternative;if(consequent instanceof AST_Assign&&alternative instanceof AST_Assign&&consequent.operator==alternative.operator&&consequent.left.equivalent_to(alternative.left)&&!consequent.left.has_side_effects(compressor)){return make_node(AST_Assign,self,{operator:consequent.operator,left:consequent.left,right:make_node(AST_Conditional,self,{condition:self.condition,consequent:consequent.right,alternative:alternative.right})})}if(consequent instanceof AST_Call&&alternative.TYPE===consequent.TYPE&&consequent.args.length==alternative.args.length&&!consequent.expression.has_side_effects(compressor)&&consequent.expression.equivalent_to(alternative.expression)){if(consequent.args.length==0){return make_node(AST_Seq,self,{car:self.condition,cdr:consequent})}if(consequent.args.length==1){consequent.args[0]=make_node(AST_Conditional,self,{condition:self.condition,consequent:consequent.args[0],alternative:alternative.args[0]});return consequent}}if(consequent instanceof AST_Conditional&&consequent.alternative.equivalent_to(alternative)){return make_node(AST_Conditional,self,{condition:make_node(AST_Binary,self,{left:self.condition,operator:"&&",right:consequent.condition}),consequent:consequent.consequent,alternative:alternative})}if(consequent instanceof AST_Constant&&alternative instanceof AST_Constant&&consequent.equivalent_to(alternative)){if(self.condition.has_side_effects(compressor)){return AST_Seq.from_array([self.condition,make_node_from_constant(compressor,consequent.value,self)])}else{return make_node_from_constant(compressor,consequent.value,self)}}if(consequent instanceof AST_True&&alternative instanceof AST_False){self.condition=self.condition.negate(compressor);return make_node(AST_UnaryPrefix,self.condition,{operator:"!",expression:self.condition})}if(consequent instanceof AST_False&&alternative instanceof AST_True){return self.condition.negate(compressor)}return self});OPT(AST_Boolean,function(self,compressor){if(compressor.option("booleans")){var p=compressor.parent();if(p instanceof AST_Binary&&(p.operator=="=="||p.operator=="!=")){compressor.warn("Non-strict equality against boolean: {operator} {value} [{file}:{line},{col}]",{operator:p.operator,value:self.value,file:p.start.file,line:p.start.line,col:p.start.col});return make_node(AST_Number,self,{value:+self.value})}return make_node(AST_UnaryPrefix,self,{operator:"!",expression:make_node(AST_Number,self,{value:1-self.value})})}return self});OPT(AST_Sub,function(self,compressor){var prop=self.property;if(prop instanceof AST_String&&compressor.option("properties")){prop=prop.getValue();if(RESERVED_WORDS(prop)?compressor.option("screw_ie8"):is_identifier_string(prop)){return make_node(AST_Dot,self,{expression:self.expression,property:prop}).optimize(compressor)}var v=parseFloat(prop);if(!isNaN(v)&&v.toString()==prop){self.property=make_node(AST_Number,self.property,{value:v})}}return self});OPT(AST_Dot,function(self,compressor){var prop=self.property;if(RESERVED_WORDS(prop)&&!compressor.option("screw_ie8")){return make_node(AST_Sub,self,{expression:self.expression,property:make_node(AST_String,self,{value:prop})}).optimize(compressor)}return self.evaluate(compressor)[0]});function literals_in_boolean_context(self,compressor){if(compressor.option("booleans")&&compressor.in_boolean_context()&&!self.has_side_effects(compressor)){return make_node(AST_True,self)}return self}OPT(AST_Array,literals_in_boolean_context);OPT(AST_Object,literals_in_boolean_context);OPT(AST_RegExp,literals_in_boolean_context)})();/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function SourceMap(options){options=defaults(options,{file:null,root:null,orig:null,orig_line_diff:0,dest_line_diff:0});var orig_map=options.orig&&new MOZ_SourceMap.SourceMapConsumer(options.orig);var generator;if(orig_map){generator=MOZ_SourceMap.SourceMapGenerator.fromSourceMap(orig_map)}else{generator=new MOZ_SourceMap.SourceMapGenerator({file:options.file,sourceRoot:options.root})}function add(source,gen_line,gen_col,orig_line,orig_col,name){if(orig_map){var info=orig_map.originalPositionFor({line:orig_line,column:orig_col});if(info.source===null){return}source=info.source;orig_line=info.line;orig_col=info.column;name=info.name||name}generator.addMapping({generated:{line:gen_line+options.dest_line_diff,column:gen_col},original:{line:orig_line+options.orig_line_diff,column:orig_col},source:source,name:name})}return{add:add,get:function(){return generator},toString:function(){return JSON.stringify(generator.toJSON())}}}/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";(function(){var MOZ_TO_ME={ExpressionStatement:function(M){var expr=M.expression;if(expr.type==="Literal"&&typeof expr.value==="string"){return new AST_Directive({start:my_start_token(M),end:my_end_token(M),value:expr.value})}return new AST_SimpleStatement({start:my_start_token(M),end:my_end_token(M),body:from_moz(expr)})},TryStatement:function(M){var handlers=M.handlers||[M.handler];if(handlers.length>1||M.guardedHandlers&&M.guardedHandlers.length){throw new Error("Multiple catch clauses are not supported.")}return new AST_Try({start:my_start_token(M),end:my_end_token(M),body:from_moz(M.block).body,bcatch:from_moz(handlers[0]),bfinally:M.finalizer?new AST_Finally(from_moz(M.finalizer)):null})},Property:function(M){var key=M.key;var name=key.type=="Identifier"?key.name:key.value;var args={start:my_start_token(key),end:my_end_token(M.value),key:name,value:from_moz(M.value)};switch(M.kind){case"init":return new AST_ObjectKeyVal(args);case"set":args.value.name=from_moz(key);return new AST_ObjectSetter(args);case"get":args.value.name=from_moz(key);return new AST_ObjectGetter(args)}},ObjectExpression:function(M){return new AST_Object({start:my_start_token(M),end:my_end_token(M),properties:M.properties.map(function(prop){prop.type="Property";return from_moz(prop)})})},SequenceExpression:function(M){return AST_Seq.from_array(M.expressions.map(from_moz))},MemberExpression:function(M){return new(M.computed?AST_Sub:AST_Dot)({start:my_start_token(M),end:my_end_token(M),property:M.computed?from_moz(M.property):M.property.name,expression:from_moz(M.object)})},SwitchCase:function(M){return new(M.test?AST_Case:AST_Default)({start:my_start_token(M),end:my_end_token(M),expression:from_moz(M.test),body:M.consequent.map(from_moz)})},VariableDeclaration:function(M){return new(M.kind==="const"?AST_Const:AST_Var)({start:my_start_token(M),end:my_end_token(M),definitions:M.declarations.map(from_moz)})},Literal:function(M){var val=M.value,args={start:my_start_token(M),end:my_end_token(M)};if(val===null)return new AST_Null(args);switch(typeof val){case"string":args.value=val;return new AST_String(args);case"number":args.value=val;return new AST_Number(args);case"boolean":return new(val?AST_True:AST_False)(args);default:args.value=val;return new AST_RegExp(args)}},Identifier:function(M){var p=FROM_MOZ_STACK[FROM_MOZ_STACK.length-2];return new(p.type=="LabeledStatement"?AST_Label:p.type=="VariableDeclarator"&&p.id===M?p.kind=="const"?AST_SymbolConst:AST_SymbolVar:p.type=="FunctionExpression"?p.id===M?AST_SymbolLambda:AST_SymbolFunarg:p.type=="FunctionDeclaration"?p.id===M?AST_SymbolDefun:AST_SymbolFunarg:p.type=="CatchClause"?AST_SymbolCatch:p.type=="BreakStatement"||p.type=="ContinueStatement"?AST_LabelRef:AST_SymbolRef)({start:my_start_token(M),end:my_end_token(M),name:M.name})}};MOZ_TO_ME.UpdateExpression=MOZ_TO_ME.UnaryExpression=function To_Moz_Unary(M){var prefix="prefix"in M?M.prefix:M.type=="UnaryExpression"?true:false;return new(prefix?AST_UnaryPrefix:AST_UnaryPostfix)({start:my_start_token(M),end:my_end_token(M),operator:M.operator,expression:from_moz(M.argument)})};map("Program",AST_Toplevel,"body@body");map("EmptyStatement",AST_EmptyStatement);map("BlockStatement",AST_BlockStatement,"body@body");map("IfStatement",AST_If,"test>condition, consequent>body, alternate>alternative");map("LabeledStatement",AST_LabeledStatement,"label>label, body>body");map("BreakStatement",AST_Break,"label>label");map("ContinueStatement",AST_Continue,"label>label");map("WithStatement",AST_With,"object>expression, body>body");map("SwitchStatement",AST_Switch,"discriminant>expression, cases@body");map("ReturnStatement",AST_Return,"argument>value");map("ThrowStatement",AST_Throw,"argument>value");map("WhileStatement",AST_While,"test>condition, body>body");map("DoWhileStatement",AST_Do,"test>condition, body>body");map("ForStatement",AST_For,"init>init, test>condition, update>step, body>body");map("ForInStatement",AST_ForIn,"left>init, right>object, body>body");map("DebuggerStatement",AST_Debugger);map("FunctionDeclaration",AST_Defun,"id>name, params@argnames, body%body");map("VariableDeclarator",AST_VarDef,"id>name, init>value");map("CatchClause",AST_Catch,"param>argname, body%body");map("ThisExpression",AST_This);map("ArrayExpression",AST_Array,"elements@elements");map("FunctionExpression",AST_Function,"id>name, params@argnames, body%body");map("BinaryExpression",AST_Binary,"operator=operator, left>left, right>right");map("LogicalExpression",AST_Binary,"operator=operator, left>left, right>right");map("AssignmentExpression",AST_Assign,"operator=operator, left>left, right>right");map("ConditionalExpression",AST_Conditional,"test>condition, consequent>consequent, alternate>alternative");map("NewExpression",AST_New,"callee>expression, arguments@args");map("CallExpression",AST_Call,"callee>expression, arguments@args");def_to_moz(AST_Directive,function To_Moz_Directive(M){return{type:"ExpressionStatement",expression:{type:"Literal",value:M.value}}});def_to_moz(AST_SimpleStatement,function To_Moz_ExpressionStatement(M){return{type:"ExpressionStatement",expression:to_moz(M.body)}});def_to_moz(AST_SwitchBranch,function To_Moz_SwitchCase(M){return{type:"SwitchCase",test:to_moz(M.expression),consequent:M.body.map(to_moz)}});def_to_moz(AST_Try,function To_Moz_TryStatement(M){return{type:"TryStatement",block:to_moz_block(M),handler:to_moz(M.bcatch),guardedHandlers:[],finalizer:to_moz(M.bfinally)}});def_to_moz(AST_Catch,function To_Moz_CatchClause(M){return{type:"CatchClause",param:to_moz(M.argname),guard:null,body:to_moz_block(M)}});def_to_moz(AST_Definitions,function To_Moz_VariableDeclaration(M){return{type:"VariableDeclaration",kind:M instanceof AST_Const?"const":"var",declarations:M.definitions.map(to_moz)}});def_to_moz(AST_Seq,function To_Moz_SequenceExpression(M){return{type:"SequenceExpression",expressions:M.to_array().map(to_moz)}});def_to_moz(AST_PropAccess,function To_Moz_MemberExpression(M){var isComputed=M instanceof AST_Sub;return{type:"MemberExpression",object:to_moz(M.expression),computed:isComputed,property:isComputed?to_moz(M.property):{type:"Identifier",name:M.property}}});def_to_moz(AST_Unary,function To_Moz_Unary(M){return{type:M.operator=="++"||M.operator=="--"?"UpdateExpression":"UnaryExpression",operator:M.operator,prefix:M instanceof AST_UnaryPrefix,argument:to_moz(M.expression)}});def_to_moz(AST_Binary,function To_Moz_BinaryExpression(M){return{type:M.operator=="&&"||M.operator=="||"?"LogicalExpression":"BinaryExpression",left:to_moz(M.left),operator:M.operator,right:to_moz(M.right)}});def_to_moz(AST_Object,function To_Moz_ObjectExpression(M){return{type:"ObjectExpression",properties:M.properties.map(to_moz)}});def_to_moz(AST_ObjectProperty,function To_Moz_Property(M){var key=is_identifier(M.key)?{type:"Identifier",name:M.key}:{type:"Literal",value:M.key};var kind;if(M instanceof AST_ObjectKeyVal){kind="init"}else if(M instanceof AST_ObjectGetter){kind="get"}else if(M instanceof AST_ObjectSetter){kind="set"}return{type:"Property",kind:kind,key:key,value:to_moz(M.value)}});def_to_moz(AST_Symbol,function To_Moz_Identifier(M){var def=M.definition();return{type:"Identifier",name:def?def.mangled_name||def.name:M.name}});def_to_moz(AST_Constant,function To_Moz_Literal(M){var value=M.value;if(typeof value==="number"&&(value<0||value===0&&1/value<0)){return{type:"UnaryExpression",operator:"-",prefix:true,argument:{type:"Literal",value:-value}}}return{type:"Literal",value:value}});def_to_moz(AST_Atom,function To_Moz_Atom(M){return{type:"Identifier",name:String(M.value)}});AST_Boolean.DEFMETHOD("to_mozilla_ast",AST_Constant.prototype.to_mozilla_ast);AST_Null.DEFMETHOD("to_mozilla_ast",AST_Constant.prototype.to_mozilla_ast);AST_Hole.DEFMETHOD("to_mozilla_ast",function To_Moz_ArrayHole(){return null});AST_Block.DEFMETHOD("to_mozilla_ast",AST_BlockStatement.prototype.to_mozilla_ast);AST_Lambda.DEFMETHOD("to_mozilla_ast",AST_Function.prototype.to_mozilla_ast);function my_start_token(moznode){var loc=moznode.loc,start=loc&&loc.start;var range=moznode.range;return new AST_Token({file:loc&&loc.source,line:start&&start.line,col:start&&start.column,pos:range?range[0]:moznode.start,endline:start&&start.line,endcol:start&&start.column,endpos:range?range[0]:moznode.start})}function my_end_token(moznode){var loc=moznode.loc,end=loc&&loc.end;var range=moznode.range;return new AST_Token({file:loc&&loc.source,line:end&&end.line,col:end&&end.column,pos:range?range[1]:moznode.end,endline:end&&end.line,endcol:end&&end.column,endpos:range?range[1]:moznode.end})}function map(moztype,mytype,propmap){var moz_to_me="function From_Moz_"+moztype+"(M){\n";moz_to_me+="return new "+mytype.name+"({\n"+"start: my_start_token(M),\n"+"end: my_end_token(M)";var me_to_moz="function To_Moz_"+moztype+"(M){\n";me_to_moz+="return {\n"+"type: "+JSON.stringify(moztype);if(propmap)propmap.split(/\s*,\s*/).forEach(function(prop){var m=/([a-z0-9$_]+)(=|@|>|%)([a-z0-9$_]+)/i.exec(prop);if(!m)throw new Error("Can't understand property map: "+prop);var moz=m[1],how=m[2],my=m[3];moz_to_me+=",\n"+my+": ";me_to_moz+=",\n"+moz+": ";switch(how){case"@":moz_to_me+="M."+moz+".map(from_moz)";me_to_moz+="M."+my+".map(to_moz)";break;case">":moz_to_me+="from_moz(M."+moz+")";me_to_moz+="to_moz(M."+my+")";break;case"=":moz_to_me+="M."+moz;me_to_moz+="M."+my;break;case"%":moz_to_me+="from_moz(M."+moz+").body";me_to_moz+="to_moz_block(M)";break;default:throw new Error("Can't understand operator in propmap: "+prop)}});moz_to_me+="\n})\n}";me_to_moz+="\n}\n}";moz_to_me=new Function("my_start_token","my_end_token","from_moz","return("+moz_to_me+")")(my_start_token,my_end_token,from_moz);me_to_moz=new Function("to_moz","to_moz_block","return("+me_to_moz+")")(to_moz,to_moz_block);MOZ_TO_ME[moztype]=moz_to_me;def_to_moz(mytype,me_to_moz)}var FROM_MOZ_STACK=null;function from_moz(node){FROM_MOZ_STACK.push(node);var ret=node!=null?MOZ_TO_ME[node.type](node):null;FROM_MOZ_STACK.pop();return ret}AST_Node.from_mozilla_ast=function(node){var save_stack=FROM_MOZ_STACK;FROM_MOZ_STACK=[];var ast=from_moz(node);FROM_MOZ_STACK=save_stack;return ast};function set_moz_loc(mynode,moznode,myparent){var start=mynode.start;var end=mynode.end;if(start.pos!=null&&end.endpos!=null){moznode.range=[start.pos,end.endpos]}if(start.line){moznode.loc={start:{line:start.line,column:start.col},end:end.endline?{line:end.endline,column:end.endcol}:null};if(start.file){moznode.loc.source=start.file}}return moznode}function def_to_moz(mytype,handler){mytype.DEFMETHOD("to_mozilla_ast",function(){return set_moz_loc(this,handler(this))})}function to_moz(node){return node!=null?node.to_mozilla_ast():null}function to_moz_block(node){return{type:"BlockStatement",body:node.body.map(to_moz)}}})();/***********************************************************************

  A JavaScript tokenizer / parser / beautifier / compressor.
  https://github.com/mishoo/UglifyJS2

  -------------------------------- (C) ---------------------------------

                           Author: Mihai Bazon
                         <mihai.bazon@gmail.com>
                       http://mihai.bazon.net/blog

  Distributed under the BSD license:

    Copyright 2012 (c) Mihai Bazon <mihai.bazon@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        * Redistributions of source code must retain the above
          copyright notice, this list of conditions and the following
          disclaimer.

        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials
          provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER “AS IS” AND ANY
    EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
    OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.

 ***********************************************************************/
"use strict";function find_builtins(){var a=[];[Object,Array,Function,Number,String,Boolean,Error,Math,Date,RegExp].forEach(function(ctor){Object.getOwnPropertyNames(ctor).map(add);if(ctor.prototype){Object.getOwnPropertyNames(ctor.prototype).map(add)}});function add(name){push_uniq(a,name)}return a}function mangle_properties(ast,options){options=defaults(options,{reserved:null,cache:null,only_cache:false,regex:null});var reserved=options.reserved;if(reserved==null)reserved=find_builtins();var cache=options.cache;if(cache==null){cache={cname:-1,props:new Dictionary}}var regex=options.regex;var names_to_mangle=[];ast.walk(new TreeWalker(function(node){if(node instanceof AST_ObjectKeyVal){add(node.key)}else if(node instanceof AST_ObjectProperty){add(node.key.name)}else if(node instanceof AST_Dot){if(this.parent()instanceof AST_Assign){add(node.property)}}else if(node instanceof AST_Sub){if(this.parent()instanceof AST_Assign){addStrings(node.property)}}}));return ast.transform(new TreeTransformer(function(node){if(node instanceof AST_ObjectKeyVal){if(should_mangle(node.key)){node.key=mangle(node.key)}}else if(node instanceof AST_ObjectProperty){if(should_mangle(node.key.name)){node.key.name=mangle(node.key.name)}}else if(node instanceof AST_Dot){if(should_mangle(node.property)){node.property=mangle(node.property)}}else if(node instanceof AST_Sub){node.property=mangleStrings(node.property)}}));function can_mangle(name){if(reserved.indexOf(name)>=0)return false;if(options.only_cache){return cache.props.has(name)}if(/^[0-9.]+$/.test(name))return false;return true}function should_mangle(name){if(regex&&!regex.test(name))return false;if(reserved.indexOf(name)>=0)return false;return cache.props.has(name)||names_to_mangle.indexOf(name)>=0}function add(name){if(can_mangle(name))push_uniq(names_to_mangle,name)}function mangle(name){var mangled=cache.props.get(name);if(!mangled){do{mangled=base54(++cache.cname)}while(!can_mangle(mangled));cache.props.set(name,mangled)}return mangled}function addStrings(node){var out={};try{(function walk(node){node.walk(new TreeWalker(function(node){if(node instanceof AST_Seq){walk(node.cdr);return true}if(node instanceof AST_String){add(node.value);return true}if(node instanceof AST_Conditional){walk(node.consequent);walk(node.alternative);return true}throw out}))})(node)}catch(ex){if(ex!==out)throw ex}}function mangleStrings(node){return node.transform(new TreeTransformer(function(node){if(node instanceof AST_Seq){node.cdr=mangleStrings(node.cdr)}else if(node instanceof AST_String){if(should_mangle(node.value)){node.value=mangle(node.value)}}else if(node instanceof AST_Conditional){node.consequent=mangleStrings(node.consequent);node.alternative=mangleStrings(node.alternative)}return node}))}}exports["array_to_hash"]=array_to_hash;exports["slice"]=slice;exports["characters"]=characters;exports["member"]=member;exports["find_if"]=find_if;exports["repeat_string"]=repeat_string;exports["DefaultsError"]=DefaultsError;exports["defaults"]=defaults;exports["merge"]=merge;exports["noop"]=noop;exports["MAP"]=MAP;exports["push_uniq"]=push_uniq;exports["string_template"]=string_template;exports["remove"]=remove;exports["mergeSort"]=mergeSort;exports["set_difference"]=set_difference;exports["set_intersection"]=set_intersection;exports["makePredicate"]=makePredicate;exports["all"]=all;exports["Dictionary"]=Dictionary;exports["DEFNODE"]=DEFNODE;exports["AST_Token"]=AST_Token;exports["AST_Node"]=AST_Node;exports["AST_Statement"]=AST_Statement;exports["AST_Debugger"]=AST_Debugger;exports["AST_Directive"]=AST_Directive;exports["AST_SimpleStatement"]=AST_SimpleStatement;exports["walk_body"]=walk_body;exports["AST_Block"]=AST_Block;exports["AST_BlockStatement"]=AST_BlockStatement;exports["AST_EmptyStatement"]=AST_EmptyStatement;exports["AST_StatementWithBody"]=AST_StatementWithBody;exports["AST_LabeledStatement"]=AST_LabeledStatement;exports["AST_IterationStatement"]=AST_IterationStatement;exports["AST_DWLoop"]=AST_DWLoop;exports["AST_Do"]=AST_Do;exports["AST_While"]=AST_While;exports["AST_For"]=AST_For;exports["AST_ForIn"]=AST_ForIn;exports["AST_With"]=AST_With;exports["AST_Scope"]=AST_Scope;exports["AST_Toplevel"]=AST_Toplevel;exports["AST_Lambda"]=AST_Lambda;exports["AST_Accessor"]=AST_Accessor;exports["AST_Function"]=AST_Function;exports["AST_Defun"]=AST_Defun;exports["AST_Jump"]=AST_Jump;exports["AST_Exit"]=AST_Exit;exports["AST_Return"]=AST_Return;exports["AST_Throw"]=AST_Throw;exports["AST_LoopControl"]=AST_LoopControl;exports["AST_Break"]=AST_Break;exports["AST_Continue"]=AST_Continue;exports["AST_If"]=AST_If;exports["AST_Switch"]=AST_Switch;exports["AST_SwitchBranch"]=AST_SwitchBranch;exports["AST_Default"]=AST_Default;exports["AST_Case"]=AST_Case;exports["AST_Try"]=AST_Try;exports["AST_Catch"]=AST_Catch;exports["AST_Finally"]=AST_Finally;exports["AST_Definitions"]=AST_Definitions;exports["AST_Var"]=AST_Var;exports["AST_Const"]=AST_Const;exports["AST_VarDef"]=AST_VarDef;exports["AST_Call"]=AST_Call;exports["AST_New"]=AST_New;exports["AST_Seq"]=AST_Seq;exports["AST_PropAccess"]=AST_PropAccess;exports["AST_Dot"]=AST_Dot;exports["AST_Sub"]=AST_Sub;exports["AST_Unary"]=AST_Unary;exports["AST_UnaryPrefix"]=AST_UnaryPrefix;exports["AST_UnaryPostfix"]=AST_UnaryPostfix;exports["AST_Binary"]=AST_Binary;exports["AST_Conditional"]=AST_Conditional;exports["AST_Assign"]=AST_Assign;exports["AST_Array"]=AST_Array;exports["AST_Object"]=AST_Object;exports["AST_ObjectProperty"]=AST_ObjectProperty;exports["AST_ObjectKeyVal"]=AST_ObjectKeyVal;exports["AST_ObjectSetter"]=AST_ObjectSetter;exports["AST_ObjectGetter"]=AST_ObjectGetter;exports["AST_Symbol"]=AST_Symbol;exports["AST_SymbolAccessor"]=AST_SymbolAccessor;exports["AST_SymbolDeclaration"]=AST_SymbolDeclaration;exports["AST_SymbolVar"]=AST_SymbolVar;exports["AST_SymbolConst"]=AST_SymbolConst;exports["AST_SymbolFunarg"]=AST_SymbolFunarg;exports["AST_SymbolDefun"]=AST_SymbolDefun;exports["AST_SymbolLambda"]=AST_SymbolLambda;exports["AST_SymbolCatch"]=AST_SymbolCatch;exports["AST_Label"]=AST_Label;exports["AST_SymbolRef"]=AST_SymbolRef;exports["AST_LabelRef"]=AST_LabelRef;exports["AST_This"]=AST_This;exports["AST_Constant"]=AST_Constant;exports["AST_String"]=AST_String;exports["AST_Number"]=AST_Number;exports["AST_RegExp"]=AST_RegExp;exports["AST_Atom"]=AST_Atom;exports["AST_Null"]=AST_Null;exports["AST_NaN"]=AST_NaN;exports["AST_Undefined"]=AST_Undefined;exports["AST_Hole"]=AST_Hole;exports["AST_Infinity"]=AST_Infinity;exports["AST_Boolean"]=AST_Boolean;exports["AST_False"]=AST_False;exports["AST_True"]=AST_True;exports["TreeWalker"]=TreeWalker;exports["KEYWORDS"]=KEYWORDS;exports["KEYWORDS_ATOM"]=KEYWORDS_ATOM;exports["RESERVED_WORDS"]=RESERVED_WORDS;exports["KEYWORDS_BEFORE_EXPRESSION"]=KEYWORDS_BEFORE_EXPRESSION;exports["OPERATOR_CHARS"]=OPERATOR_CHARS;exports["RE_HEX_NUMBER"]=RE_HEX_NUMBER;exports["RE_OCT_NUMBER"]=RE_OCT_NUMBER;exports["RE_DEC_NUMBER"]=RE_DEC_NUMBER;exports["OPERATORS"]=OPERATORS;exports["WHITESPACE_CHARS"]=WHITESPACE_CHARS;exports["PUNC_BEFORE_EXPRESSION"]=PUNC_BEFORE_EXPRESSION;exports["PUNC_CHARS"]=PUNC_CHARS;exports["REGEXP_MODIFIERS"]=REGEXP_MODIFIERS;exports["UNICODE"]=UNICODE;exports["is_letter"]=is_letter;exports["is_digit"]=is_digit;exports["is_alphanumeric_char"]=is_alphanumeric_char;exports["is_unicode_digit"]=is_unicode_digit;exports["is_unicode_combining_mark"]=is_unicode_combining_mark;exports["is_unicode_connector_punctuation"]=is_unicode_connector_punctuation;exports["is_identifier"]=is_identifier;exports["is_identifier_start"]=is_identifier_start;exports["is_identifier_char"]=is_identifier_char;exports["is_identifier_string"]=is_identifier_string;exports["parse_js_number"]=parse_js_number;exports["JS_Parse_Error"]=JS_Parse_Error;exports["js_error"]=js_error;exports["is_token"]=is_token;exports["EX_EOF"]=EX_EOF;exports["tokenizer"]=tokenizer;exports["UNARY_PREFIX"]=UNARY_PREFIX;exports["UNARY_POSTFIX"]=UNARY_POSTFIX;exports["ASSIGNMENT"]=ASSIGNMENT;exports["PRECEDENCE"]=PRECEDENCE;exports["STATEMENTS_WITH_LABELS"]=STATEMENTS_WITH_LABELS;exports["ATOMIC_START_TOKEN"]=ATOMIC_START_TOKEN;exports["parse"]=parse;exports["TreeTransformer"]=TreeTransformer;exports["SymbolDef"]=SymbolDef;exports["base54"]=base54;exports["OutputStream"]=OutputStream;exports["Compressor"]=Compressor;exports["SourceMap"]=SourceMap;exports["find_builtins"]=find_builtins;exports["mangle_properties"]=mangle_properties})({},function(){return this}());
