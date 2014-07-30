# encoding: UTF-8

require "execjs"
require "json"
require "uglifier/version"

# A wrapper around the UglifyJS interface
class Uglifier
  Error = ExecJS::Error
  JS = <<-JS
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
        }
      } else {
        return option;
      }
    }

    var options = %s;
    var source = options.source;
    var ast = UglifyJS.parse(source, options.parse_options);
    ast.figure_out_scope();

    if (options.compress) {
      var compressor = UglifyJS.Compressor(options.compress);
      ast = ast.transform(compressor);
      ast.figure_out_scope();
    }

    if (options.mangle) {
      ast.compute_char_frequency();
      ast.mangle_names(options.mangle);
    }

    if (options.enclose) {
      ast = ast.wrap_enclose(options.enclose);
    }

    var gen_code_options = options.output;
    gen_code_options.comments = comments(options.output.comments);

    if (options.generate_map) {
        var source_map = UglifyJS.SourceMap(options.source_map_options);
        gen_code_options.source_map = source_map;
    }

    var stream = UglifyJS.OutputStream(gen_code_options);

    ast.print(stream);
    if (options.generate_map) {
        return [stream.toString(), source_map.toString()];
    } else {
        return stream.toString();
    }
  JS

  # UglifyJS source patch
  SourcePath = File.expand_path("../uglify.js", __FILE__)
  # ES5 shims source path
  ES5FallbackPath = File.expand_path("../es5.js", __FILE__)
  # String.split shim source path
  SplitFallbackPath = File.expand_path("../split.js", __FILE__)

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
      :space_colon => false, # Insert space before colons (only with beautifier)
      :width => 80, # Specify line width when beautifier is used (only with beautifier)
      :preamble => nil # Preamble for the generated JS file. Can be used to insert any code or comment.
    },
    :mangle => {
      :eval => false, # Mangle names when eval of when is used in scope
      :except => ["$super"], # Argument names to be excluded from mangling
      :sort => false, # Assign shorter names to most frequently used variables. Often results in bigger output after gzip.
      :toplevel => false # Mangle names declared in the toplevel scope
    }, # Mangle variable and function names, set to false to skip mangling
    :compress => {
      :sequences => true, # Allow statements to be joined by commas
      :properties => true, # Rewrite property access using the dot notation
      :dead_code => true, # Remove unreachable code
      :drop_debugger => true, # Remove debugger; statements
      :unsafe => false, # Apply "unsafe" transformations
      :conditionals => true, # Optimize for if-s and conditional expressions
      :comparisons => true, # Apply binary node optimizations for comparisons
      :evaluate => true, # Attempt to evaluate constant expressions
      :booleans => true, # Various optimizations to boolean contexts
      :loops => true, # Optimize loops when condition can be statically determined
      :unused => true, # Drop unreferenced functions and variables
      :hoist_funs => true, # Hoist function declarations
      :hoist_vars => false, # Hoist var declarations
      :if_return => true, # Optimizations for if/return and if/continue
      :join_vars => true, # Join consecutive var statements
      :cascade => true, # Cascade sequences
      :negate_iife => true, # Negate immediately invoked function expressions to avoid extra parens
      :pure_getters => false, # Assume that object property access does not have any side-effects
      :pure_funcs => nil, # List of functions without side-effects. Can safely discard function calls when the result value is not used
      :drop_console => false, # Drop calls to console.* functions
      :angular => false, # Process @ngInject annotations
      :keep_fargs => false # Preserve unused function arguments
    }, # Apply transformations to code, set to false to skip
    :define => {}, # Define values for symbol replacement
    :enclose => false, # Enclose in output function wrapper, define replacements as key-value pairs
    :source_filename => nil, # The filename of the input file
    :source_root => nil, # The URL of the directory which contains :source_filename
    :output_filename => nil, # The filename or URL where the minified output can be found
    :input_source_map => nil, # The contents of the source map describing the input
    :screw_ie8 => false # Don't bother to generate safe code for IE8
  }
  # rubocop:enable LineLength

  # Minifies JavaScript code using implicit context.
  #
  # source should be a String or IO object containing valid JavaScript.
  # options contain optional overrides to Uglifier::DEFAULTS
  #
  # Returns minified code as String
  def self.compile(source, options = {})
    new(options).compile(source)
  end

  # Minifies JavaScript code and generates a source map using implicit context.
  #
  # source should be a String or IO object containing valid JavaScript.
  # options contain optional overrides to Uglifier::DEFAULTS
  #
  # Returns a pair of [minified code as String, source map as a String]
  def self.compile_with_map(source, options = {})
    new(options).compile_with_map(source)
  end

  # Initialize new context for Uglifier with given options
  #
  # options - Hash of options to override Uglifier::DEFAULTS
  def initialize(options = {})
    (options.keys - DEFAULTS.keys - [:comments, :squeeze, :copyright])[0..1].each do |missing|
      raise ArgumentError, "Invalid option: #{missing}"
    end
    @options = options
    @context = ExecJS.compile(File.open(ES5FallbackPath, "r:UTF-8").read +
                              File.open(SplitFallbackPath, "r:UTF-8").read +
                              File.open(SourcePath, "r:UTF-8").read)
  end

  # Minifies JavaScript code
  #
  # source should be a String or IO object containing valid JavaScript.
  #
  # Returns minified code as String
  def compile(source)
    run_uglifyjs(source, false)
  end
  alias_method :compress, :compile

  # Minifies JavaScript code and generates a source map
  #
  # source should be a String or IO object containing valid JavaScript.
  #
  # Returns a pair of [minified code as String, source map as a String]
  def compile_with_map(source)
    run_uglifyjs(source, true)
  end

  private

  # Run UglifyJS for given source code
  def run_uglifyjs(source, generate_map)
    @context.exec(Uglifier::JS % json_encode(
      :source => read_source(source),
      :output => output_options,
      :compress => compressor_options,
      :mangle => mangle_options,
      :parse_options => parse_options,
      :source_map_options => source_map_options,
      :generate_map => generate_map,
      :enclose => enclose_options
    ))
  end

  def read_source(source)
    if source.respond_to?(:read)
      source.read
    else
      source.to_s
    end
  end

  def mangle_options
    conditional_option(@options[:mangle], DEFAULTS[:mangle])
  end

  def compressor_options
    defaults = conditional_option(
      DEFAULTS[:compress],
      :global_defs => @options[:define] || {},
      :screw_ie8 => @options[:screw_ie8] || DEFAULTS[:screw_ie8]
    )
    conditional_option(@options[:compress] || @options[:squeeze], defaults)
  end

  def comment_options
    case comment_setting
    when :all, true
      true
    when :jsdoc
      "jsdoc"
    when :copyright
      encode_regexp(/Copyright/i)
    when Regexp
      encode_regexp(comment_setting)
    else
      false
    end
  end

  def comment_setting
    if @options.has_key?(:output) && @options[:output].has_key?(:comments)
      @options[:output][:comments]
    elsif @options.has_key?(:comments)
      @options[:comments]
    elsif @options[:copyright] == false
      :none
    else
      DEFAULTS[:output][:comments]
    end
  end

  def output_options
    DEFAULTS[:output].merge(@options[:output] || {}).merge(
      :comments => comment_options,
      :screw_ie8 => screw_ie8?
    ).reject { |key, _| key == :ie_proof }
  end

  def screw_ie8?
    if (@options[:output] || {}).has_key?(:ie_proof)
      false
    else
      @options[:screw_ie8] || DEFAULTS[:screw_ie8]
    end
  end

  def source_map_options
    {
      :file => @options[:output_filename],
      :root => @options[:source_root],
      :orig => @options[:input_source_map]
    }
  end

  def parse_options
    { :filename => @options[:source_filename] }
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

  def json_encode(obj)
    JSON.dump(obj)
  end

  def encode_regexp(regexp)
    modifiers = if regexp.casefold?
                  "i"
                else
                  ""
                end

    [regexp.source, modifiers]
  end

  def conditional_option(value, defaults)
    if value == true || value.nil?
      defaults
    elsif value
      defaults.merge(value)
    else
      false
    end
  end
end
