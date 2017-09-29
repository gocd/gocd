# coding: utf-8

require 'transpec/version'

module Transpec
  class CommitMessage
    def initialize(report, rspec_version, cli_args = [])
      @report = report
      @rspec_version = rspec_version
      @cli_args = cli_args
    end

    def to_s
      conversion_summary = @report.summary(bullet: '*', separate_by_blank_line: true)

      <<-END.gsub(/^\s+\|/, '').chomp
        |Convert specs to RSpec #{@rspec_version} syntax with Transpec
        |
        |This conversion is done by Transpec #{Transpec::Version} with the following command:
        |    transpec #{smart_cli_args}
        |
        |#{conversion_summary}
        |
        |For more details: https://github.com/yujinakayama/transpec#supported-conversions
      END
    end

    def smart_cli_args
      @cli_args.map do |arg|
        if arg.include?(' ')
          arg.inspect
        else
          arg
        end
      end.join(' ')
    end
  end
end
