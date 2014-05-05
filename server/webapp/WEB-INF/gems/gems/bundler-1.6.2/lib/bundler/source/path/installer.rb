module Bundler
  class Source
    class Path

      class Installer < Bundler::GemInstaller
        def initialize(spec, options = {})
          @spec              = spec
          @tmp_bin_dir       = "#{Bundler.tmp(spec.full_name)}/bin"
          @gem_bin_dir       = "#{Bundler.rubygems.gem_dir}/bin"
          @bin_dir           = Bundler.requires_sudo? ? @tmp_bin_dir : @gem_bin_dir
          @gem_dir           = Bundler.rubygems.path(spec.full_gem_path)
          @wrappers          = options[:wrappers] || true
          @env_shebang       = options[:env_shebang] || true
          @format_executable = options[:format_executable] || false
          @build_args        = options[:build_args] || Bundler.rubygems.build_args
        end

        def generate_bin
          return if spec.executables.nil? || spec.executables.empty?

          if Bundler.requires_sudo?
            FileUtils.mkdir_p(@tmp_bin_dir) unless File.exist?(@tmp_bin_dir)
          end

          super

          if Bundler.requires_sudo?
            Bundler.mkdir_p @gem_bin_dir
            spec.executables.each do |exe|
              Bundler.sudo "cp -R #{@tmp_bin_dir}/#{exe} #{@gem_bin_dir}"
            end
          end
        end
      end

    end
  end
end
