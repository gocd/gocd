module Bundler
  module Source
    class Path

      class Installer < Bundler::GemInstaller
        def initialize(spec, options = {})
          @spec              = spec
          @bin_dir           = Bundler.requires_sudo? ? "#{Bundler.tmp}/bin" : "#{Bundler.rubygems.gem_dir}/bin"
          @gem_dir           = Bundler.rubygems.path(spec.full_gem_path)
          @wrappers          = options[:wrappers] || true
          @env_shebang       = options[:env_shebang] || true
          @format_executable = options[:format_executable] || false
          @build_args        = options[:build_args] || Bundler.rubygems.build_args
        end

        def generate_bin
          return if spec.executables.nil? || spec.executables.empty?

          if Bundler.requires_sudo?
            FileUtils.mkdir_p("#{Bundler.tmp}/bin") unless File.exist?("#{Bundler.tmp}/bin")
          end
          super
          if Bundler.requires_sudo?
            Bundler.mkdir_p "#{Bundler.rubygems.gem_dir}/bin"
            spec.executables.each do |exe|
              Bundler.sudo "cp -R #{Bundler.tmp}/bin/#{exe} #{Bundler.rubygems.gem_dir}/bin/"
            end
          end
        end
      end

    end
  end
end
