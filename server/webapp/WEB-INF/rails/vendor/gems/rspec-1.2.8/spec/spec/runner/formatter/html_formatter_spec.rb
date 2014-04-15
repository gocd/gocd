# require File.dirname(__FILE__) + '/../../../spec_helper'

begin # See rescue all the way at the bottom

require 'nokogiri' # Needed to compare generated with wanted HTML
require 'spec/runner/formatter/html_formatter'

module Spec
  module Runner
    module Formatter
      describe HtmlFormatter do

        treats_method_missing_as_private

        attr_reader :root, :expected_file, :expected_html
          
        before do
          @root = File.expand_path("#{File.dirname(__FILE__)}/../../../..")
          suffix = jruby? ? '-jruby' : ''
          @expected_file = "#{File.dirname(__FILE__)}/html_formatted-#{::RUBY_VERSION}#{suffix}.html"
          raise "There is no HTML file with expected content for this platform: #{expected_file}" unless File.file?(expected_file)
          @expected_html = File.read(expected_file)
        end

#        # Uncomment this line temporarily in order to overwrite the expected with actual.
#        # Use with care!!!
#        describe "file generator" do
#          it "generates a new comparison file" do
#            Dir.chdir(root) do
#              args = [
#                'examples/failing/mocking_example.rb',
#                'examples/failing/diffing_spec.rb',
#                'examples/passing/stubbing_example.rb',
#                'examples/passing/pending_example.rb',
#                '--format',
#                'html',
#                "--diff"
#              ]
#              err = StringIO.new
#              out = StringIO.new
#              run_with ::Spec::Runner::OptionParser.parse(args, err, out)
#
#              seconds = /\d+\.\d+ seconds/
#              html = out.string.gsub seconds, 'x seconds'
#
#              File.open(expected_file, 'w') {|io| io.write(html)}
#            end
#          end
#        end

        it "should produce HTML identical to the one we designed manually with --diff" do
          Dir.chdir(root) do
            args = [
              'examples/failing/mocking_example.rb',
              'examples/failing/diffing_spec.rb',
              'examples/passing/stubbing_example.rb',
              'examples/passing/pending_example.rb',
              '--format',
              'html',
              "--diff"
            ]
            err = StringIO.new
            out = StringIO.new
            run_with ::Spec::Runner::OptionParser.parse(args, err, out)

            seconds = /\d+\.\d+ seconds/
            html = out.string.gsub seconds, 'x seconds'
            expected_html.gsub! seconds, 'x seconds'

            doc = Nokogiri::HTML(html)
            backtraces = doc.search("div.backtrace").collect {|e| e.at("pre").inner_html}
            doc.css("div.backtrace").remove

            expected_doc = Nokogiri::HTML(expected_html)
            expected_backtraces = expected_doc.search("div.backtrace").collect {|e| e.at("pre").inner_html}
            expected_doc.search("div.backtrace").remove

            doc.inner_html.should == expected_doc.inner_html

            expected_backtraces.each_with_index do |expected_line, i|
              expected_path, expected_line_number, expected_suffix = expected_line.split(':')
              actual_path, actual_line_number, actual_suffix = backtraces[i].split(':')
              File.expand_path(actual_path).should == File.expand_path(expected_path)
              actual_line_number.should == expected_line_number
            end
          end
        end

        it "should produce HTML identical to the one we designed manually with --dry-run" do
          Dir.chdir(root) do
            args = [
              'examples/failing/mocking_example.rb',
              'examples/failing/diffing_spec.rb',
              'examples/passing/stubbing_example.rb',
              'examples/passing/pending_example.rb',
              '--format',
              'html',
              "--dry-run"
            ]
            err = StringIO.new
            out = StringIO.new
            run_with ::Spec::Runner::OptionParser.parse(args, err, out)

            seconds = /\d+\.\d+ seconds/
            html = out.string.gsub seconds, 'x seconds'
            expected_html.gsub! seconds, 'x seconds'

            html.should =~ /This was a dry-run/m
          end
        end
      end
    end
  end
end

rescue LoadError
  warn "nokogiri not loaded -- skipping HtmlFormatter specs"
end
