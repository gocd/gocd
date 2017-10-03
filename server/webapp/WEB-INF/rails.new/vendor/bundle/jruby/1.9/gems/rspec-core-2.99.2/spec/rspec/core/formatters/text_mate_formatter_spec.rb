# encoding: utf-8
require 'spec_helper'
require 'rspec/core/formatters/text_mate_formatter'
require 'nokogiri'

module RSpec
  module Core
    module Formatters
      describe TextMateFormatter do

        let(:root) { File.expand_path("#{File.dirname(__FILE__)}/../../../..") }
        let(:expected_file) do
          "#{File.dirname(__FILE__)}/text_mate_formatted.html"
        end

        let(:generated_html) do
          allow(RSpec).to receive(:deprecate)
          options = RSpec::Core::ConfigurationOptions.new(
            %w[spec/rspec/core/resources/formatter_specs.rb --format textmate --order defined]
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

        context 'produced HTML', :if => RUBY_VERSION >= '2.0.0' do
          it "produces HTML identical to the one we designed manually" do
            Dir.chdir(root) do
              actual_doc = Nokogiri::HTML(generated_html)
              backtrace_lines = actual_doc.search("div.backtrace a")
              actual_doc.search("div.backtrace").remove

              expected_doc = Nokogiri::HTML(expected_html)
              expected_doc.search("div.backtrace").remove

              expect(actual_doc.inner_html).to eq(expected_doc.inner_html)

              backtrace_lines.each do |backtrace_line|
                expect(backtrace_line['href']).to include("txmt://open?url=")
              end
            end
          end
        end

      end
    end
  end
end
