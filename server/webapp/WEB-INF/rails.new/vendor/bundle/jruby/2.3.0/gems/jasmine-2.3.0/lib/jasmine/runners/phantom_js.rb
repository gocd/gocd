require 'phantomjs'

module Jasmine
  module Runners
    class PhantomJs
      def initialize(formatter, jasmine_server_url, prevent_phantom_js_auto_install, show_console_log, phantom_config_script, show_full_stack_trace)
        @formatter = formatter
        @jasmine_server_url = jasmine_server_url
        @prevent_phantom_js_auto_install = prevent_phantom_js_auto_install
        @show_console_log = show_console_log
        @phantom_config_script = phantom_config_script
        @show_full_stack_trace = show_full_stack_trace
      end

      def run
        phantom_script = File.join(File.dirname(__FILE__), 'phantom_jasmine_run.js')
        command = "#{phantom_js_path} '#{phantom_script}' #{jasmine_server_url} #{show_console_log} '#{@phantom_config_script}'"
        IO.popen(command) do |output|
          output.each do |line|
            if line =~ /^jasmine_spec_result/
              line = line.sub(/^jasmine_spec_result/, '')
              raw_results = JSON.parse(line, :max_nesting => false)
              results = raw_results.map { |r| Result.new(r.merge!("show_full_stack_trace" => @show_full_stack_trace)) }
              formatter.format(results)
            elsif line =~ /^jasmine_suite_result/
              line = line.sub(/^jasmine_suite_result/, '')
              raw_results = JSON.parse(line, :max_nesting => false)
              results = raw_results.map { |r| Result.new(r.merge!("show_full_stack_trace" => @show_full_stack_trace)) }
              failures = results.select(&:failed?)
              if failures.any?
                formatter.format(failures)
              end
            elsif line =~ /^Failed to configure phantom$/
              config_failure = Result.new('fullName' => line,
                                          'failedExpectations' => [],
                                          'description' => '',
                                          'status' => 'failed',
                                          'show_full_stack_trace' => @show_full_stack_trace)
              formatter.format([config_failure])
              @show_console_log = true
              puts line
            elsif show_console_log
              puts line
            end
          end
        end
        formatter.done
      end

      def phantom_js_path
        prevent_phantom_js_auto_install ? 'phantomjs' : Phantomjs.path
      end

      def boot_js
        File.expand_path('phantom_boot.js', File.dirname(__FILE__))
      end

      private
      attr_reader :formatter, :jasmine_server_url, :prevent_phantom_js_auto_install, :show_console_log
    end
  end
end

