# encoding: UTF-8

require "json"
require "base64"
require "execjs"
require "uglifier/version"

# A wrapper around the UglifyJS interface
class Uglifier
  # Error class for compilation errors.
  class Error < StandardError; end

  # UglifyJS source path
  SourcePath = File.expand_path("../uglify.js", __FILE__)
  # UglifyJS with Harmony source path
  HarmonySourcePath = File.expand_path("../uglify-harmony.js", __FILE__)
  # Source Map path
  SourceMapPath = File.expand_path("../source-map.js", __FILE__)
  # ES5 shims source path
  ES5FallbackPath = File.expand_path("../es5.js", __FILE__)
  # String.split shim source path
  SplitFallbackPath = File.expand_path("../split.js", __FILE__)
  # UglifyJS wrapper path
  UglifyJSWrapperPath = File.expand_path("../uglifier.js", __FILE__)

  # Default options for compilation
  DEFAULTS = {
    # rubocop:disable LineLength
    :output => {
      :ascii_only => true, # Escape non-ASCII characterss
      :comments => :copyright, # Preserve comments (:all, :jsdoc, :copyright, :none)
      :inline_script => false, # Escape occurrences of </script in strings
      :quote_keys => false, # Quote keys in object literals
      :max_line_len => 32 * 1024, # Maximum line length in minified code
      :bracketize => false, # Bracketize if, for, do, while or with statements, even if their body is a single statement
      :semicolons => true, # Separate statements with semicolons
      :preserve_line => false, # Preserve line numbers in outputs
      :beautify => false, # Beautify output
      :indent_level => 4, # Indent level in spaces
      :indent_start => 0, # Starting indent level
      :width => 80, # Specify line width when beautifier is used (only with beautifier)
      :preamble => nil, # Preamble for the generated JS file. Can be used to insert any code or comment.
      :wrap_iife => false, # Wrap IIFEs in parenthesis. Note: this disables the negate_iife compression option.
      :shebang => true, # Preserve shebang (#!) in preamble (shell scripts)
      :quote_style => 0, # Quote style, possible values :auto (default), :single, :double, :original
      :keep_quoted_props => false # Keep quotes property names
    },
    :mangle => {
      :eval => false, # Mangle names when eval of when is used in scope
      :reserved => ["$super"], # Argument names to be excluded from mangling
      :properties => false, # Mangle property names
      :toplevel => false, # Mangle names declared in the toplevel scope
    }, # Mangle variable and function names, set to false to skip mangling
    :compress => {
      :sequences => true, # Allow statements to be joined by commas
      :properties => true, # Rewrite property access using the dot notation
      :dead_code => true, # Remove unreachable code
      :drop_debugger => true, # Remove debugger; statements
      :unsafe => false, # Apply "unsafe" transformations
      :unsafe_comps => false, # Reverse < and <= to > and >= to allow improved compression. This might be unsafe when an at least one of two operands is an object with computed values due the use of methods like get, or valueOf. This could cause change in execution order after operands in the comparison are switching. Compression only works if both comparisons and unsafe_comps are both set to true.
      :unsafe_math => false, # Optimize numerical expressions like 2 * x * 3 into 6 * x, which may give imprecise floating point results.
      :unsafe_proto => false, # Optimize expressions like Array.prototype.slice.call(a) into [].slice.call(a)
      :conditionals => true, # Optimize for if-s and conditional expressions
      :comparisons => true, # Apply binary node optimizations for comparisons
      :evaluate => true, # Attempt to evaluate constant expressions
      :booleans => true, # Various optimizations to boolean contexts
      :loops => true, # Optimize loops when condition can be statically determined
      :unused => true, # Drop unreferenced functions and variables (simple direct variable assignments do not count as references unless set to `"keep_assign"`)
      :toplevel => false, # Drop unreferenced top-level functions and variables
      :top_retain => [], # prevent specific toplevel functions and variables from `unused` removal
      :hoist_funs => true, # Hoist function declarations
      :hoist_vars => false, # Hoist var declarations
      :if_return => true, # Optimizations for if/return and if/continue
      :join_vars => true, # Join consecutive var statements
      :cascade => true, # Cascade sequences
      :collapse_vars => true, # Collapse single-use var and const definitions when possible.
      :reduce_funcs => false, # Inline single-use functions as function expressions. Depends on reduce_vars.
      :reduce_vars => false, # Collapse variables assigned with and used as constant values.
      :negate_iife => true, # Negate immediately invoked function expressions to avoid extra parens
      :pure_getters => false, # Assume that object property access does not have any side-effects
      :pure_funcs => nil, # List of functions without side-effects. Can safely discard function calls when the result value is not used
      :drop_console => false, # Drop calls to console.* functions
      :keep_fargs => false, # Preserve unused function arguments
      :keep_fnames => false, # Do not drop names in function definitions
      :passes => 1, # Number of times to run compress. Raising the number of passes will increase compress time, but can produce slightly smaller code.
      :keep_infinity => false, # Prevent compression of Infinity to 1/0
      :side_effects => true, # Pass false to disable potentially dropping functions marked as "pure" using pure comment annotation. See UglifyJS documentation for details.
      :switches => true, # de-duplicate and remove unreachable switch branches
    }, # Apply transformations to code, set to false to skip
    :parse => {
      :bare_returns => false, # Allow top-level return statements.
      :expression => false, # Parse a single expression, rather than a program (for parsing JSON).
      :html5_comments => true, # Ignore HTML5 comments in input
      :shebang => true, # support #!command as the first line
      :strict => false
    },
    :define => {}, # Define values for symbol replacement
    :keep_fnames => false, # Generate code safe for the poor souls relying on Function.prototype.name at run-time. Sets both compress and mangle keep_fanems to true.
    :toplevel => false,
    :ie8 => true, # Generate safe code for IE8
    :source_map => false, # Generate source map
    :harmony => false # Enable ES6/Harmony mode (experimental). Disabling mangling and compressing is recommended with Harmony mode.
  }

  EXTRA_OPTIONS = [:comments, :mangle_properties]

  MANGLE_PROPERTIES_DEFAULTS = {
    :debug => false, # Add debug prefix and suffix to mangled properties
    :regex => nil, # A regular expression to filter property names to be mangled
    :keep_quoted => false, # Keep quoted property names
    :reserved => [], # List of properties that should not be mangled
    :builtins => false, # Mangle properties that overlap with standard JS globals
  }

  SOURCE_MAP_DEFAULTS = {
    :map_url => false, # Url for source mapping to be appended in minified source
    :url => false, # Url for original source to be appended in minified source
    :sources_content => false, # Include original source content in map
    :filename => nil, # The filename of the input file
    :root => nil, # The URL of the directory which contains :filename
    :output_filename => nil, # The filename or URL where the minified output can be found
    :input_source_map => nil # The contents of the source map describing the input
  }

  # rubocop:enable LineLength

  # Minifies JavaScript code using implicit context.
  #
  # @param source [IO, String] valid JS source code.
  # @param options [Hash] optional overrides to +Uglifier::DEFAULTS+
  # @return [String] minified code.
  def self.compile(source, options = {})
    new(options).compile(source)
  end

  # Minifies JavaScript code and generates a source map using implicit context.
  #
  # @param source [IO, String] valid JS source code.
  # @param options [Hash] optional overrides to +Uglifier::DEFAULTS+
  # @return [Array(String, String)] minified code and source map.
  def self.compile_with_map(source, options = {})
    new(options).compile_with_map(source)
  end

  # Initialize new context for Uglifier with given options
  #
  # @param options [Hash] optional overrides to +Uglifier::DEFAULTS+
  def initialize(options = {})
    (options.keys - DEFAULTS.keys - EXTRA_OPTIONS)[0..1].each do |missing|
      raise ArgumentError, "Invalid option: #{missing}"
    end
    @options = options

    source = harmony? ? source_with(HarmonySourcePath) : source_with(SourcePath)
    @context = ExecJS.compile(source)
  end

  # Minifies JavaScript code
  #
  # @param source [IO, String] valid JS source code.
  # @return [String] minified code.
  def compile(source)
    if @options[:source_map]
      compiled, source_map = run_uglifyjs(source, true)
      source_map_uri = Base64.strict_encode64(source_map)
      source_map_mime = "application/json;charset=utf-8;base64"
      compiled + "\n//# sourceMappingURL=data:#{source_map_mime},#{source_map_uri}"
    else
      run_uglifyjs(source, false)
    end
  end
  alias_method :compress, :compile

  # Minifies JavaScript code and generates a source map
  #
  # @param source [IO, String] valid JS source code.
  # @return [Array(String, String)] minified code and source map.
  def compile_with_map(source)
    run_uglifyjs(source, true)
  end

  private

  def source_map_comments
    return '' unless @options[:source_map].respond_to?(:[])

    suffix = ''
    if @options[:source_map][:map_url]
      suffix += "\n//# sourceMappingURL=" + @options[:source_map][:map_url]
    end

    if @options[:source_map][:url]
      suffix += "\n//# sourceURL=" + @options[:source_map][:url]
    end
    suffix
  end

  def source_with(path)
    [ES5FallbackPath, SplitFallbackPath, SourceMapPath, path,
     UglifyJSWrapperPath].map do |file|
      File.open(file, "r:UTF-8", &:read)
    end.join("\n")
  end

  # Run UglifyJS for given source code
  def run_uglifyjs(input, generate_map)
    source = read_source(input)
    input_map = input_source_map(source, generate_map)
    options = {
      :source => source,
      :output => output_options,
      :compress => compressor_options,
      :mangle => mangle_options,
      :parse => parse_options,
      :sourceMap => source_map_options(input_map),
      :ie8 => ie8?
    }

    parse_result(@context.call("uglifier", options), generate_map)
  end

  def harmony?
    @options[:harmony]
  end

  def error_message(result)
    result['error']['message'] +
      if result['error']['message'].start_with?("Unexpected token") && !harmony?
        ". To use ES6 syntax, harmony mode must be enabled with " \
        "Uglifier.new(:harmony => true)."
      else
        ""
      end
  end

  def parse_result(result, generate_map)
    raise Error, error_message(result) if result.has_key?('error')

    if generate_map
      [result['code'] + source_map_comments, result['map']]
    else
      result['code'] + source_map_comments
    end
  end

  def read_source(source)
    if source.respond_to?(:read)
      source.read
    else
      source.to_s
    end
  end

  def mangle_options
    defaults = conditional_option(
      DEFAULTS[:mangle],
      :keep_fnames => keep_fnames?(:mangle)
    )

    conditional_option(
      @options[:mangle],
      defaults,
      :properties => mangle_properties_options
    )
  end

  def mangle_properties_options
    mangle_options = conditional_option(@options[:mangle], DEFAULTS[:mangle])

    mangle_properties_options =
      if @options.has_key?(:mangle_properties)
        @options[:mangle_properties]
      else
        mangle_options && mangle_options[:properties]
      end

    options = conditional_option(mangle_properties_options, MANGLE_PROPERTIES_DEFAULTS)

    if options && options[:regex]
      options.merge(:regex => encode_regexp(options[:regex]))
    else
      options
    end
  end

  def compressor_options
    defaults = conditional_option(
      DEFAULTS[:compress],
      :global_defs => @options[:define] || {}
    )

    conditional_option(
      @options[:compress],
      defaults,
      { :keep_fnames => keep_fnames?(:compress) }.merge(negate_iife_block)
    )
  end

  # Prevent negate_iife when wrap_iife is true
  def negate_iife_block
    if output_options[:wrap_iife]
      { :negate_iife => false }
    else
      {}
    end
  end

  def comment_options
    case comment_setting
    when :all, true
      true
    when :jsdoc
      "jsdoc"
    when :copyright
      encode_regexp(/(^!)|Copyright/i)
    when Regexp
      encode_regexp(comment_setting)
    else
      false
    end
  end

  def quote_style
    option = conditional_option(@options[:output], DEFAULTS[:output])[:quote_style]
    case option
    when :single
      1
    when :double
      2
    when :original
      3
    when Numeric
      option
    else # auto
      0
    end
  end

  def comment_setting
    if @options.has_key?(:output) && @options[:output].has_key?(:comments)
      @options[:output][:comments]
    elsif @options.has_key?(:comments)
      @options[:comments]
    else
      DEFAULTS[:output][:comments]
    end
  end

  def output_options
    DEFAULTS[:output].merge(@options[:output] || {}).merge(
      :comments => comment_options,
      :quote_style => quote_style
    )
  end

  def ie8?
    @options.fetch(:ie8, DEFAULTS[:ie8])
  end

  def keep_fnames?(type)
    if @options[:keep_fnames] || DEFAULTS[:keep_fnames]
      true
    else
      @options[type].respond_to?(:[]) && @options[type][:keep_fnames] ||
        DEFAULTS[type].respond_to?(:[]) && DEFAULTS[type][:keep_fnames]
    end
  end

  def source_map_options(input_map)
    options = conditional_option(@options[:source_map], SOURCE_MAP_DEFAULTS) || SOURCE_MAP_DEFAULTS

    {
      :input => options[:filename],
      :filename => options[:output_filename],
      :root => options.fetch(:root) { input_map ? input_map["sourceRoot"] : nil },
      :content => input_map,
      #:map_url => options[:map_url],
      :url => options[:url],
      :includeSources => options[:sources_content]
    }
  end

  def parse_options
    conditional_option(@options[:parse], DEFAULTS[:parse])
      .merge(parse_source_map_options)
  end

  def parse_source_map_options
    if @options[:source_map].respond_to?(:[])
      { :filename => @options[:source_map][:filename] }
    else
      {}
    end
  end

  def enclose_options
    if @options[:enclose]
      @options[:enclose].map do |pair|
        pair.first + ':' + pair.last
      end
    else
      false
    end
  end

  def encode_regexp(regexp)
    modifiers = if regexp.casefold?
                  "i"
                else
                  ""
                end

    [regexp.source, modifiers]
  end

  def conditional_option(value, defaults, overrides = {})
    if value == true || value.nil?
      defaults.merge(overrides)
    elsif value
      defaults.merge(value).merge(overrides)
    else
      false
    end
  end

  def sanitize_map_root(map)
    if map.nil?
      nil
    elsif map.is_a? String
      sanitize_map_root(JSON.parse(map))
    elsif map["sourceRoot"] == ""
      map.merge("sourceRoot" => nil)
    else
      map
    end
  end

  def extract_source_mapping_url(source)
    comment_start = %r{(?://|/\*\s*)}
    comment_end = %r{\s*(?:\r?\n?\*/|$)?}
    source_mapping_regex = /#{comment_start}[@#]\ssourceMappingURL=\s*(\S*?)#{comment_end}/
    rest = /\s#{comment_start}[@#]\s[a-zA-Z]+=\s*(?:\S*?)#{comment_end}/
    regex = /#{source_mapping_regex}(?:#{rest})*\Z/m
    match = regex.match(source)
    match && match[1]
  end

  def input_source_map(source, generate_map)
    return nil unless generate_map
    source_map_options = @options[:source_map].is_a?(Hash) ? @options[:source_map] : {}
    sanitize_map_root(source_map_options.fetch(:input_source_map) do
      url = extract_source_mapping_url(source)
      if url && url.start_with?("data:")
        Base64.strict_decode64(url.split(",", 2)[-1])
      end
    end)
  rescue ArgumentError, JSON::ParserError
    nil
  end
end
