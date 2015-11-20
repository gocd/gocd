module Bundler
  class Source
    class Path

      class Installer < Bundler::GemInstaller
        attr_reader :spec

        def initialize(spec, options = {})
          @spec              = spec
          @gem_dir           = Bundler.rubygems.path(spec.full_gem_path)
          @wrappers          = true
          @env_shebang       = true
          @format_executable = options[:format_executable] || false
          @build_args        = options[:build_args] || Bundler.rubygems.build_args
          @gem_bin_dir       = "#{Bundler.rubygems.gem_dir}/bin"

          if Bundler.requires_sudo?
            @tmp_dir = Bundler.tmp(spec.full_name).to_s
            @bin_dir = "#{@tmp_dir}/bin"
          else
            @bin_dir = @gem_bin_dir
          end
        end

        def generate_bin
          return if spec.executables.nil? || spec.executables.empty?

          super

          if Bundler.requires_sudo?
            Bundler.mkdir_p @gem_bin_dir
            spec.executables.each do |exe|
              Bundler.sudo "cp -R #{@bin_dir}/#{exe} #{@gem_bin_dir}"
            end
          end
        ensure
          Bundler.rm_rf(@tmp_dir) if Bundler.requires_sudo?
        end
      end

    end
  end
end
