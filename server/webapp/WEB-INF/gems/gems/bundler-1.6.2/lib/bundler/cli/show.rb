require 'bundler/cli/common'

module Bundler
  class CLI::Show
    attr_reader :options, :gem_name
    def initialize(options, gem_name)
      @options = options
      @gem_name = gem_name
    end

    def run
      Bundler.ui.silence do
        Bundler.definition.validate_ruby!
        Bundler.load.lock
      end

      if gem_name
        if gem_name == "bundler"
          path = File.expand_path("../../../..", __FILE__)
        else
          spec = Bundler::CLI::Common.select_spec(gem_name, :regex_match)
          return unless spec
          path = spec.full_gem_path
          if !File.directory?(path)
            Bundler.ui.warn "The gem #{gem_name} has been deleted. It was installed at:"
          end
        end
        return Bundler.ui.info(path)
      end

      if options[:paths]
        Bundler.load.specs.sort_by { |s| s.name }.map do |s|
          Bundler.ui.info s.full_gem_path
        end
      else
        Bundler.ui.info "Gems included by the bundle:"
        Bundler.load.specs.sort_by { |s| s.name }.each do |s|
          desc = "  * #{s.name} (#{s.version}#{s.git_version})"
          if @options[:verbose]
            Bundler.ui.info "#{desc} - #{s.summary || 'No description available.'}"
          else
            Bundler.ui.info desc
          end
        end
      end
    end
  end
end
