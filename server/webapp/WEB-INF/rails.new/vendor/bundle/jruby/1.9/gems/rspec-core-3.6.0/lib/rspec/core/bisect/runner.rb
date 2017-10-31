RSpec::Support.require_rspec_core "shell_escape"
require 'open3'
require 'shellwords'

module RSpec
  module Core
    module Bisect
      # Provides an API to run the suite for a set of locations, using
      # the given bisect server to capture the results.
      # @private
      class Runner
        attr_reader :original_cli_args

        def initialize(server, original_cli_args)
          @server            = server
          @original_cli_args = original_cli_args.reject { |arg| arg.start_with?("--bisect") }
        end

        def run(locations)
          run_locations(locations, original_results.failed_example_ids)
        end

        def command_for(locations)
          parts = []

          parts << RUBY << load_path
          parts << open3_safe_escape(RSpec::Core.path_to_executable)

          parts << "--format"   << "bisect"
          parts << "--drb-port" << @server.drb_port

          parts.concat reusable_cli_options
          parts.concat locations.map { |l| open3_safe_escape(l) }

          parts.join(" ")
        end

        def repro_command_from(locations)
          parts = []

          parts.concat environment_repro_parts
          parts << "rspec"
          parts.concat Formatters::Helpers.organize_ids(locations)
          parts.concat original_cli_args_without_locations

          parts.join(" ")
        end

        def original_results
          @original_results ||= run_locations(original_locations)
        end

      private

        include RSpec::Core::ShellEscape
        # On JRuby, Open3.popen3 does not handle shellescaped args properly:
        # https://github.com/jruby/jruby/issues/2767
        if RSpec::Support::Ruby.jruby?
          # :nocov:
          alias open3_safe_escape quote
          # :nocov:
        else
          alias open3_safe_escape escape
        end

        def run_locations(*capture_args)
          @server.capture_run_results(*capture_args) do
            run_command command_for([])
          end
        end

        # `Open3.capture2e` does not work on JRuby:
        # https://github.com/jruby/jruby/issues/2766
        if Open3.respond_to?(:capture2e) && !RSpec::Support::Ruby.jruby?
          def run_command(cmd)
            Open3.capture2e(bisect_environment_hash, cmd).first
          end
        else # for 1.8.7
          # :nocov:
          def run_command(cmd)
            out = err = nil

            original_spec_opts = ENV['SPEC_OPTS']
            ENV['SPEC_OPTS'] = spec_opts_without_bisect

            Open3.popen3(cmd) do |_, stdout, stderr|
              # Reading the streams blocks until the process is complete
              out = stdout.read
              err = stderr.read
            end

            "Stdout:\n#{out}\n\nStderr:\n#{err}"
          ensure
            ENV['SPEC_OPTS'] = original_spec_opts
          end
          # :nocov:
        end

        def bisect_environment_hash
          if ENV.key?('SPEC_OPTS')
            { 'SPEC_OPTS' => spec_opts_without_bisect }
          else
            {}
          end
        end

        def environment_repro_parts
          bisect_environment_hash.map do |k, v|
            %Q(#{k}="#{v}")
          end
        end

        def spec_opts_without_bisect
          Shellwords.join(
            Shellwords.split(ENV.fetch('SPEC_OPTS', '')).reject do |arg|
              arg =~ /^--bisect/
            end
          )
        end

        def reusable_cli_options
          @reusable_cli_options ||= begin
            opts = original_cli_args_without_locations

            if (port = parsed_original_cli_options[:drb_port])
              opts -= %W[ --drb-port #{port} ]
            end

            parsed_original_cli_options.fetch(:formatters) { [] }.each do |(name, out)|
              opts -= %W[ --format #{name} -f -f#{name} ]
              opts -= %W[ --out #{out} -o -o#{out} ]
            end

            opts
          end
        end

        def original_cli_args_without_locations
          @original_cli_args_without_locations ||= begin
            files_or_dirs = parsed_original_cli_options.fetch(:files_or_directories_to_run)
            @original_cli_args - files_or_dirs
          end
        end

        def parsed_original_cli_options
          @parsed_original_cli_options ||= Parser.parse(@original_cli_args)
        end

        def original_locations
          parsed_original_cli_options.fetch(:files_or_directories_to_run)
        end

        def load_path
          @load_path ||= "-I#{$LOAD_PATH.map { |p| open3_safe_escape(p) }.join(':')}"
        end

        # Path to the currently running Ruby executable, borrowed from Rake:
        # https://github.com/ruby/rake/blob/v10.4.2/lib/rake/file_utils.rb#L8-L12
        # Note that we skip `ENV['RUBY']` because we don't have to deal with running
        # RSpec from within a MRI source repository:
        # https://github.com/ruby/rake/commit/968682759b3b65e42748cd2befb2ff3e982272d9
        RUBY = File.join(
          RbConfig::CONFIG['bindir'],
          RbConfig::CONFIG['ruby_install_name'] + RbConfig::CONFIG['EXEEXT']).
          sub(/.*\s.*/m, '"\&"')
      end
    end
  end
end
