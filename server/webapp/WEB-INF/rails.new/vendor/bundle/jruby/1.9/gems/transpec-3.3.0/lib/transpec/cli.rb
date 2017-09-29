# coding: utf-8

require 'transpec/cli/option_parser'
require 'transpec/commit_message'
require 'transpec/config'
require 'transpec/converter'
require 'transpec/dynamic_analyzer'
require 'transpec/project'
require 'transpec/report'
require 'transpec/spec_suite'
require 'rainbow'
require 'rainbow/ext/string' unless String.method_defined?(:color)

module Transpec
  class CLI
    attr_reader :project, :config, :report

    def self.run(args = ARGV)
      new.run(args)
    end

    def initialize
      @project = Project.new
      @config = Config.new
      @report = Report.new
    end

    def run(args)
      begin
        paths = OptionParser.new(config).parse(args)
        validate_project!
      rescue => error
        warn error.message
        return false
      end

      begin
        process(paths)
      rescue DynamicAnalyzer::AnalysisError => error
        warn "\n" + error.message.color(:red)
        return false
      end

      display_summary
      generate_commit_message
      display_final_guide

      true
    end

    def process(paths)
      unless config.skip_dynamic_analysis?
        runtime_data = run_dynamic_analysis(paths)
      end

      spec_suite = SpecSuite.new(project, paths, runtime_data)
      # Actually #analyze does not need to be invoked here, but doing this will avoid long freeze
      # while conversion of files.
      puts 'Gathering the spec suite data...'
      spec_suite.analyze
      puts

      spec_suite.specs.each do |spec|
        convert_spec(spec, spec_suite)
      end
    end

    def run_dynamic_analysis(paths)
      runtime_data = nil

      puts 'Copying the project for dynamic analysis...'

      DynamicAnalyzer.new(project: project, rspec_command: config.rspec_command) do |analyzer|
        puts "Running dynamic analysis with command #{analyzer.rspec_command.inspect}..."
        runtime_data = analyzer.analyze(paths)
      end

      puts

      runtime_data
    end

    def convert_spec(spec, spec_suite)
      puts "Converting #{spec.path}"

      converter = Converter.new(spec_suite, config)
      converter.convert_file!(spec)

      warn_annotations(converter.report)
      report << converter.report
    rescue Parser::SyntaxError, EncodingError => error
      warn_file_error(error, spec.path)
      report.file_errors << error
    end

    private

    def validate_project!
      unless config.forced?
        if Git.command_available? && Git.inside_of_repository? && !Git.clean?
          fail 'The current Git repository is not clean. Aborting. ' \
               'If you want to proceed forcibly, use -f/--force option.'
        end
      end

      if project.rspec_version < Transpec.required_rspec_version
        fail "Your project must have rspec gem dependency #{Transpec.required_rspec_version} " \
             "or later but currently it's #{project.rspec_version}. Aborting."
      end
    end

    def display_summary
      puts

      unless report.records.empty?
        puts 'Summary:'
        puts
        puts report.colored_summary
        puts
      end

      puts report.colored_stats
    end

    def generate_commit_message
      return if report.records.empty?
      return unless Git.command_available? && Git.inside_of_repository?

      commit_message = CommitMessage.new(report, project.rspec_version, ARGV)
      commit_message_path = Git.write_commit_message(commit_message)
      commit_message_path = Pathname.new(commit_message_path).relative_path_from(Pathname.pwd)

      puts
      puts 'A commit message that describes the conversion summary was generated to'.color(:cyan)
      puts "#{commit_message_path}. To use the message, type the following command for".color(:cyan)
      puts 'the next commit:'.color(:cyan)
      puts "    git commit -aeF #{commit_message_path}"
    end

    def display_final_guide
      return if report.records.empty?

      puts
      puts "Done! Now run #{'rspec'.bright} and check if everything is green."
    end

    def warn_file_error(error, path)
      message = case error
                when Parser::SyntaxError
                  "Syntax error at #{error.diagnostic.location}."
                when EncodingError
                  "Encoding error in #{path}."
                else
                  "#{error.message.capitalize} in #{path}."
                end

      message << ' Skipping the file.'
      warn message.color(:red)
    end

    def warn_annotations(report)
      annotations = report.records.map(&:annotation).compact
      annotations.concat(report.conversion_errors)
      annotations.sort_by! { |a| a.source_range.line }

      annotations.each do |annotation|
        warn_annotation(annotation)
      end
    end

    def warn_annotation(annotation)
      color = annotation.is_a?(Annotation) ? :yellow : :magenta
      message = annotation.message.color(color) + $RS
      message << highlighted_source(annotation)
      warn message
    end

    def highlighted_source(annotation)
      filename = annotation.source_buffer.name.color(:cyan)

      line_number = annotation.source_range.line

      source = annotation.source_range.source_line
      highlight_range = annotation.source_range.column_range
      source[highlight_range] = source[highlight_range].underline

      [filename, line_number, source].join(':')
    end
  end
end
