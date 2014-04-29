# encoding: utf-8
require 'spec_helper'
require 'rspec/core/formatters/html_formatter'
require 'nokogiri'

module RSpec
  module Core
    module Formatters
      describe HtmlFormatter, :if => RUBY_VERSION =~ /^(1.8.7|1.9.2|1.9.3|2.0.0)$/ do
        let(:suffix) {
          if ::RUBY_PLATFORM == 'java'
            "-jruby"
          elsif defined?(Rubinius)
            "-rbx"
          else
            ""
          end
        }

        let(:root) { File.expand_path("#{File.dirname(__FILE__)}/../../../..") }
        let(:expected_file) do
          "#{File.dirname(__FILE__)}/html_formatted-#{::RUBY_VERSION}#{suffix}.html"
        end

        let(:generated_html) do
          options = RSpec::Core::ConfigurationOptions.new(
            %w[spec/rspec/core/resources/formatter_specs.rb --format html --order default]
          )
          options.parse_options

          err, out = StringIO.new, StringIO.new
          err.set_encoding("utf-8") if err.respond_to?(:set_encoding)
          out.set_encoding("utf-8") if out.respond_to?(:set_encoding)

          command_line = RSpec::Core::CommandLine.new(options)
          command_line.instance_variable_get("@configuration").backtrace_cleaner.inclusion_patterns = []
          command_line.run(err, out)
          out.string.gsub(/\d+\.\d+(s| seconds)/, "n.nnnn\\1")
        end

        let(:expected_html) do
          unless File.file?(expected_file)
            raise "There is no HTML file with expected content for this platform: #{expected_file}"
          end
          File.read(expected_file)
        end

        before do
          RSpec.configuration.stub(:load_spec_files) do
            RSpec.configuration.files_to_run.map {|f| load File.expand_path(f) }
          end
        end

        # Uncomment this group temporarily in order to overwrite the expected
        # with actual.  Use with care!!!
        describe "file generator", :if => ENV['GENERATE'] do
          it "generates a new comparison file" do
            Dir.chdir(root) do
              File.open(expected_file, 'w') {|io| io.write(generated_html)}
            end
          end
        end

        def extract_backtrace_from(doc)
          doc.search("div.backtrace").
            collect {|e| e.at("pre").inner_html}.
            collect {|e| e.split("\n")}.flatten.
            select  {|e| e =~ /formatter_specs\.rb/}
        end

        it "produces HTML identical to the one we designed manually" do
          Dir.chdir(root) do
            actual_doc = Nokogiri::HTML(generated_html)
            actual_backtraces = extract_backtrace_from(actual_doc)
            actual_doc.css("div.backtrace").remove

            expected_doc = Nokogiri::HTML(expected_html)
            expected_backtraces = extract_backtrace_from(expected_doc)
            expected_doc.search("div.backtrace").remove

            expect(actual_doc.inner_html).to eq(expected_doc.inner_html)

            expected_backtraces.each_with_index do |expected_line, i|
              expected_path, expected_line_number, expected_suffix = expected_line.split(':')
              actual_path, actual_line_number, actual_suffix = actual_backtraces[i].split(':')
              expect(File.expand_path(actual_path)).to eq(File.expand_path(expected_path))
              expect(actual_line_number).to eq(expected_line_number)
              expect(actual_suffix).to eq(expected_suffix)
            end
          end
        end
      end
    end
  end
end
