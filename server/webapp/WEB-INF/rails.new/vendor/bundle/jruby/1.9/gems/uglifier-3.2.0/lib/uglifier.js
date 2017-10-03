// Set source-map.js sourceMap to uglify.js MOZ_SourceMap
MOZ_SourceMap = sourceMap;

function comments(option) {
  if (Object.prototype.toString.call(option) === '[object Array]') {
    return new RegExp(option[0], option[1]);
  } else if (option == "jsdoc") {
    return function(node, comment) {
      if (comment.type == "comment2") {
        return /@preserve|@license|@cc_on/i.test(comment.value);
      } else {
        return false;
      }
    };
  } else {
    return option;
  }
}

function readNameCache(key) {
  return UglifyJS.readNameCache(null, key);
}

function writeNameCache(key, cache) {
  return UglifyJS.writeNameCache(null, key, cache);
}

function regexOption(options) {
  if (typeof options === 'object' && options.regex) {
    return new RegExp(options.regex[0], options.regex[1]);
  } else {
    return null;
  }
}

function parse(source, options) {
  UglifyJS.base54.reset();
  var ast = UglifyJS.parse(source, options.parse_options);

  if (options.compress) {
    var compress = { warnings: false };
    UglifyJS.merge(compress, options.compress);
    ast.figure_out_scope(options.mangle);
    var compressor = UglifyJS.Compressor(compress);
    ast = compressor.compress(ast);
    ast.figure_out_scope();
  }

  if (options.mangle) {
    ast.figure_out_scope(options.mangle);
    ast.compute_char_frequency();
    ast.mangle_names(options.mangle);
  }

  if (options.mangle_properties) {
    var regex = regexOption(options.mangle_properties);
    UglifyJS.mangle_properties(ast, {
      reserved: [],
      only_cache: false,
      regex: regex,
      debug: options.mangle_properties.debug,
      ignore_quoted: options.mangle_properties.ignore_quoted
    });
  }

  if (options.enclose) {
    ast = ast.wrap_enclose(options.enclose);
  }
  return ast;
}

function copySourcesContent(sourceMap, options) {
  sourceMap.get().setSourceContent(options.parse_options.filename, options.source);

  var original = options.source_map_options.orig;

  if (original && original.sources && original.sourcesContent) {
    for(var i = 0; i < original.sources.length; i++) {
      sourceMap.get().setSourceContent(original.sources[i], original.sourcesContent[i]);
    }
  }
}

function uglifier(options) {
  var source = options.source;
  var ast = parse(source, options);
  var source_map;

  var gen_code_options = options.output;
  gen_code_options.comments = comments(options.output.comments);

  if (options.generate_map) {
    source_map = UglifyJS.SourceMap(options.source_map_options);
    gen_code_options.source_map = source_map;

    if (options.source_map_options.sources_content) {
      copySourcesContent(source_map, options);
    }
  }

  var stream = UglifyJS.OutputStream(gen_code_options);
  ast.print(stream);

  if (options.source_map_options.map_url) {
    stream += "\n//# sourceMappingURL=" + options.source_map_options.map_url;
  }

  if (options.source_map_options.url) {
    stream += "\n//# sourceURL=" + options.source_map_options.url;
  }

  if (options.generate_map) {
    if (options.source_map_options.sources_content) {
      source_map.get().setSourceContent(options.parse_options.filename, options.source);
    }
    return [stream.toString(), source_map.toString()];
  } else {
    return stream.toString();
  }
}
