# encoding: utf-8
require 'spec_helper'
require 'rspec/core/formatters/html_formatter'
require 'nokogiri'

module RSpec
  module Core
    module Formatters
      describe HtmlFormatter do

        let(:root) { File.expand_path("#{File.dirname(__FILE__)}/../../../..") }
        let(:expected_file) do
          "#{File.dirname(__FILE__)}/html_formatted.html"
        end

        let(:generated_html) do
          options = RSpec::Core::ConfigurationOptions.new(
            %w[spec/rspec/core/resources/formatter_specs.rb --format html --order defined]
          )
          options.parse_options

          err, out = StringIO.new, StringIO.new
          err.set_encoding("utf-8") if err.respond_to?(:set_encoding)
          out.set_encoding("utf-8") if out.respond_to?(:set_encoding)

          runner = RSpec::Core::Runner.new(options)
          configuration = runner.instance_variable_get(:@configuration)

          configuration.backtrace_formatter.inclusion_patterns = []
          configuration.output_stream = out
          configuration.deprecation_stream = err

          runner.run(err, out)

          html = out.string.gsub(/\d+\.\d+(s| seconds)/, "n.nnnn\\1")

          actual_doc = Nokogiri::HTML(html)
          actual_doc.css("div.backtrace pre").each do |elem|
            # This is to minimize churn on backtrace lines that we do not
            # assert on anyway.
            backtrace = elem.inner_html.lines.
              select {|e| e =~ /formatter_specs\.rb/ }.
              map {|x| x.chomp.split(":")[0..1].join(':') }.
              join("\n")

            elem.inner_html = backtrace
          end
          actual_doc.inner_html
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

        describe 'produced HTML', :if => RUBY_VERSION <= '2.0.0' do
          # Rubies before 2 are a wild west of different outputs, and it's not
          # worth the effort to maintain accurate fixtures for all of them.
          # Since we are verifying fixtures on other rubies, if this code at
          # least runs we can be reasonably confident the output is right since
          # behaviour variances that we care about across versions is neglible.
          it 'is present' do
            expect(generated_html).to be
          end
        end

        describe 'produced HTML', :slow, :if => RUBY_VERSION >= '2.0.0' do
          def build_and_verify_formatter_output
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

          it "is identical to the one we designed manually" do
            build_and_verify_formatter_output
          end

          context 'with mathn loaded' do
            include MathnIntegrationSupport

            it "is identical to the one we designed manually", :slow do
              with_mathn_loaded { build_and_verify_formatter_output }
            end
          end
        end
      end
    end
  end
end
