require "tmpdir"
require "execjs/runtime"

module ExecJS
  class ExternalRuntime < Runtime
    class Context < Runtime::Context
      def initialize(runtime, source = "")
        source = encode(source)

        @runtime = runtime
        @source  = source
      end

      def eval(source, options = {})
        source = encode(source)

        if /\S/ =~ source
          exec("return eval(#{::JSON.generate("(#{source})", quirks_mode: true)})")
        end
      end

      def exec(source, options = {})
        source = encode(source)
        source = "#{@source}\n#{source}" if @source
        source = @runtime.compile_source(source)

        tmpfile = write_to_tempfile(source)
        begin
          extract_result(@runtime.exec_runtime(tmpfile.path))
        ensure
          File.unlink(tmpfile)
        end
      end

      def call(identifier, *args)
        eval "#{identifier}.apply(this, #{::JSON.generate(args)})"
      end

      protected
        # See Tempfile.create on Ruby 2.1
        def create_tempfile(basename)
          tmpfile = nil
          Dir::Tmpname.create(basename) do |tmpname|
            mode    = File::WRONLY | File::CREAT | File::EXCL
            tmpfile = File.open(tmpname, mode, 0600)
          end
          tmpfile
        end

        def write_to_tempfile(contents)
          tmpfile = create_tempfile(['execjs', 'js'])
          tmpfile.write(contents)
          tmpfile.close
          tmpfile
        end

        def extract_result(output)
          status, value = output.empty? ? [] : ::JSON.parse(output, create_additions: false)
          if status == "ok"
            value
          elsif value =~ /SyntaxError:/
            raise RuntimeError, value
          else
            raise ProgramError, value
          end
        end
    end

    attr_reader :name

    def initialize(options)
      @name        = options[:name]
      @command     = options[:command]
      @runner_path = options[:runner_path]
      @encoding    = options[:encoding]
      @deprecated  = !!options[:deprecated]
      @binary      = nil

      @popen_options = {}
      @popen_options[:external_encoding] = @encoding if @encoding
      @popen_options[:internal_encoding] = ::Encoding.default_internal || 'UTF-8'

      if @runner_path
        instance_eval generate_compile_method(@runner_path)
      end
    end

    def available?
      require 'json'
      binary ? true : false
    end

    def deprecated?
      @deprecated
    end

    private
      def binary
        @binary ||= which(@command)
      end

      def locate_executable(cmd)
        if ExecJS.windows? && File.extname(cmd) == ""
          cmd << ".exe"
        end

        if File.executable? cmd
          cmd
        else
          path = ENV['PATH'].split(File::PATH_SEPARATOR).find { |p|
            full_path = File.join(p, cmd)
            File.executable?(full_path) && File.file?(full_path)
          }
          path && File.expand_path(cmd, path)
        end
      end

    protected
      def generate_compile_method(path)
        <<-RUBY
        def compile_source(source)
          <<-RUNNER
          #{IO.read(path)}
          RUNNER
        end
        RUBY
      end

      def json2_source
        @json2_source ||= IO.read(ExecJS.root + "/support/json2.js")
      end

      def encode_source(source)
        encoded_source = encode_unicode_codepoints(source)
        ::JSON.generate("(function(){ #{encoded_source} })()", quirks_mode: true)
      end

      def encode_unicode_codepoints(str)
        str.gsub(/[\u0080-\uffff]/) do |ch|
          "\\u%04x" % ch.codepoints.to_a
        end
      end

      def exec_runtime(filename)
        io = IO.popen(binary.split(' ') << filename, @popen_options.merge({err: [:child, :out]}))
        output = io.read
        io.close

        if $?.success?
          output
        else
          raise RuntimeError, output
        end
      end
      # Internally exposed for Context.
      public :exec_runtime

      def which(command)
        Array(command).find do |name|
          name, args = name.split(/\s+/, 2)
          path = locate_executable(name)

          next unless path

          args ? "#{path} #{args}" : path
        end
      end
  end
end
