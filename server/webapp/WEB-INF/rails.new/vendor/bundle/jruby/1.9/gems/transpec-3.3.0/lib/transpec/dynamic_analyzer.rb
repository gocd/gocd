# coding: utf-8

require 'transpec/dynamic_analyzer/constants'
require 'transpec/dynamic_analyzer/rewriter'
require 'transpec/dynamic_analyzer/runtime_data'
require 'transpec/directory_cloner'
require 'transpec/project'
require 'transpec/spec_suite'
require 'erb'
require 'shellwords'
require 'tmpdir'
require 'English'

module Transpec
  class DynamicAnalyzer
    attr_reader :project, :rspec_command, :silent
    alias_method :silent?, :silent

    def initialize(options = {})
      @project = options[:project] || Project.new
      @rspec_command = options[:rspec_command] || default_rspec_command
      @silent = options[:silent] || false

      if block_given?
        in_copied_project do
          yield self
        end
      end
    end

    def default_rspec_command
      if project.using_bundler?
        'bundle exec rspec'
      else
        'rspec'
      end
    end

    def analyze(paths = [])
      in_copied_project do
        rewrite_specs(paths)
        put_analysis_helper
        modify_dot_rspec
        run_rspec(paths)
        load_analysis_result
      end
    end

    private

    def in_copied_project(&block)
      return yield if @in_copied_project

      @in_copied_project = true

      Dir.mktmpdir do |tmpdir|
        copied_project_path = DirectoryCloner.copy_recursively(project.path, tmpdir)
        Dir.chdir(copied_project_path, &block)
      end
    ensure
      @in_copied_project = false
    end

    def run_rspec(paths)
      project.with_bundler_clean_env do
        command = "#{rspec_command} #{paths.shelljoin}"

        if silent?
          `#{command} 2> /dev/null`
        else
          system(command)
        end
      end
    end

    def rewrite_specs(paths)
      rewriter = Rewriter.new

      spec_suite = SpecSuite.new(project, paths)

      spec_suite.specs.each do |spec|
        next if spec.error
        rewriter.rewrite_file!(spec)
      end
    end

    def helper_filename
      File.basename(HELPER_TEMPLATE_FILE, '.erb')
    end

    def helper_source
      erb_path = File.join(File.dirname(__FILE__), 'dynamic_analyzer', HELPER_TEMPLATE_FILE)
      erb = ERB.new(File.read(erb_path), nil)
      erb.result(binding)
    end

    def put_analysis_helper
      File.write(helper_filename, helper_source)
    end

    def modify_dot_rspec
      filename = '.rspec'
      content = "--require ./#{helper_filename}\n"
      content << File.read(filename) if File.exist?(filename)
      File.write(filename, content)
    end

    def load_analysis_result
      File.open(RESULT_FILE) do |file|
        RuntimeData.load(file)
      end
    rescue Errno::ENOENT
      message = 'Failed running dynamic analysis. ' \
                'Transpec runs your specs in a copied project directory. ' \
                'If your project requires some special setup or commands to run specs, ' \
                'use -c/--rspec-command option.'
      raise AnalysisError, message
    end

    class AnalysisError < StandardError; end
  end
end
