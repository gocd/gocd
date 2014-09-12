require "shellwords"
require "stringio"
require "tempfile"
require "rbconfig"

module YUI #:nodoc:
  class Compressor
    VERSION = "0.12.0"

    class Error < StandardError; end
    class OptionError   < Error; end
    class RuntimeError  < Error; end

    attr_reader :options

    def self.default_options #:nodoc:
      { :charset => "utf-8", :line_break => nil }
    end

    def self.compressor_type #:nodoc:
      raise Error, "create a CssCompressor or JavaScriptCompressor instead"
    end

    def initialize(options = {}) #:nodoc:
      @options = self.class.default_options.merge(options)
      @command = [path_to_java]
      @command.push(*java_opts)
      @command.push("-jar")
      @command.push(path_to_jar_file)
      @command.push(*(command_option_for_type + command_options))
      @command.compact!
    end

    def command #:nodoc:
      if RbConfig::CONFIG['host_os'] =~ /mswin|mingw/
        # Shellwords is only for bourne shells, so windows shells get this
        # extremely remedial escaping
        escaped_cmd = @command.map do |word|
          if word =~ / /
            word = "\"%s\"" % word
          end

          word
        end
      else
        escaped_cmd = @command.map { |word| Shellwords.escape(word) }
      end

      escaped_cmd.join(" ")
    end

    # Compress a stream or string of code with YUI Compressor. (A stream is
    # any object that responds to +read+ and +close+ like an IO.) If a block
    # is given, you can read the compressed code from the block's argument.
    # Otherwise, +compress+ returns a string of compressed code.
    #
    # ==== Example: Compress CSS
    #   compressor = YUI::CssCompressor.new
    #   compressor.compress(<<-END_CSS)
    #     div.error {
    #       color: red;
    #     }
    #     div.warning {
    #       display: none;
    #     }
    #   END_CSS
    #   # => "div.error{color:red;}div.warning{display:none;}"
    #
    # ==== Example: Compress JavaScript
    #   compressor = YUI::JavaScriptCompressor.new
    #   compressor.compress('(function () { var foo = {}; foo["bar"] = "baz"; })()')
    #   # => "(function(){var foo={};foo.bar=\"baz\"})();"
    #
    # ==== Example: Compress and gzip a file on disk
    #   File.open("my.js", "r") do |source|
    #     Zlib::GzipWriter.open("my.js.gz", "w") do |gzip|
    #       compressor.compress(source) do |compressed|
    #         while buffer = compressed.read(4096)
    #           gzip.write(buffer)
    #         end
    #       end
    #     end
    #   end
    #
    def compress(stream_or_string)
      streamify(stream_or_string) do |stream|
        tempfile = Tempfile.new('yui_compress')
        tempfile.write stream.read
        tempfile.flush
        full_command = "%s %s" % [command, tempfile.path]

        begin
          output = `#{full_command}`
        rescue Exception => e
          # windows shells tend to blow up here when the command fails
          raise RuntimeError, "compression failed: %s" % e.message
        ensure
          tempfile.close!
        end

        if $?.exitstatus.zero?
          output
        else
          # Bourne shells tend to blow up here when the command fails, usually
          # because java is missing
          raise RuntimeError, "Command '%s' returned non-zero exit status" %
            full_command
        end
      end
    end

    private
      def command_options
        options.inject([]) do |command_options, (name, argument)|
          method = begin
            method(:"command_option_for_#{name}")
          rescue NameError
            raise OptionError, "undefined option #{name.inspect}"
          end

          command_options.concat(method.call(argument))
        end
      end

      def path_to_java
        options.delete(:java) || "java"
      end

      def java_opts
        options.delete(:java_opts).to_s.split(/\s+/)
      end

      def path_to_jar_file
        options.delete(:jar_file) || File.join(File.dirname(__FILE__), *%w".. yuicompressor-2.4.8.jar")
      end

      def streamify(stream_or_string)
        if stream_or_string.respond_to?(:read)
          yield stream_or_string
        else
          yield StringIO.new(stream_or_string.to_s)
        end
      end

      def command_option_for_type
        ["--type", self.class.compressor_type.to_s]
      end

      def command_option_for_charset(charset)
        ["--charset", charset.to_s]
      end

      def command_option_for_line_break(line_break)
        line_break ? ["--line-break", line_break.to_s] : []
      end
  end

  class CssCompressor < Compressor
    def self.compressor_type #:nodoc:
      "css"
    end

    # Creates a new YUI::CssCompressor for minifying CSS code.
    #
    # Options are:
    #
    # <tt>:charset</tt>::    Specifies the character encoding to use. Defaults to
    #                        <tt>"utf-8"</tt>.
    # <tt>:line_break</tt>:: By default, CSS will be compressed onto a single
    #                        line. Use this option to specify the maximum
    #                        number of characters in each line before a newline
    #                        is added. If <tt>:line_break</tt> is 0, a newline
    #                        is added after each CSS rule.
    #
    def initialize(options = {})
      super
    end
  end

  class JavaScriptCompressor < Compressor
    def self.compressor_type #:nodoc:
      "js"
    end

    def self.default_options #:nodoc:
      super.merge(
        :munge    => false,
        :optimize => true,
        :preserve_semicolons => false
      )
    end

    # Creates a new YUI::JavaScriptCompressor for minifying JavaScript code.
    #
    # Options are:
    #
    # <tt>:charset</tt>::    Specifies the character encoding to use. Defaults to
    #                        <tt>"utf-8"</tt>.
    # <tt>:line_break</tt>:: By default, JavaScript will be compressed onto a
    #                        single line. Use this option to specify the
    #                        maximum number of characters in each line before a
    #                        newline is added. If <tt>:line_break</tt> is 0, a
    #                        newline is added after each JavaScript statement.
    # <tt>:munge</tt>::      Specifies whether YUI Compressor should shorten local
    #                        variable names when possible. Defaults to +false+.
    # <tt>:optimize</tt>::   Specifies whether YUI Compressor should optimize
    #                        JavaScript object property access and object literal
    #                        declarations to use as few characters as possible.
    #                        Defaults to +true+.
    # <tt>:preserve_semicolons</tt>:: Defaults to +false+. If +true+, YUI
    #                                 Compressor will ensure semicolons exist
    #                                 after each statement to appease tools like
    #                                 JSLint.
    #
    def initialize(options = {})
      super
    end

    private
      def command_option_for_munge(munge)
        munge ? [] : ["--nomunge"]
      end

      def command_option_for_optimize(optimize)
        optimize ? [] : ["--disable-optimizations"]
      end

      def command_option_for_preserve_semicolons(preserve_semicolons)
        preserve_semicolons ? ["--preserve-semi"] : []
      end
  end
end
