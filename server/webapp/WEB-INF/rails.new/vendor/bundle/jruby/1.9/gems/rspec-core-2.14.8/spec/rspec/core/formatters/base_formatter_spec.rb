require 'spec_helper'
require 'rspec/core/formatters/base_formatter'

describe RSpec::Core::Formatters::BaseFormatter do

  let(:output)    { StringIO.new }
  let(:formatter) { RSpec::Core::Formatters::BaseFormatter.new(output) }

  describe "backtrace_line" do
    it "trims current working directory" do
      expect(formatter.__send__(:backtrace_line, File.expand_path(__FILE__))).to eq("./spec/rspec/core/formatters/base_formatter_spec.rb")
    end

    it "leaves the original line intact" do
      original_line = File.expand_path(__FILE__)
      formatter.__send__(:backtrace_line, original_line)
      expect(original_line).to eq(File.expand_path(__FILE__))
    end

    it "deals gracefully with a security error" do
      safely do
        formatter.__send__(:backtrace_line, __FILE__)
        # on some rubies, this doesn't raise a SecurityError; this test just
        # assures that if it *does* raise an error, the error is caught inside
      end
    end
  end

  describe "read_failed_line" do
    it "deals gracefully with a heterogeneous language stack trace" do
      exception = double(:Exception, :backtrace => [
        "at Object.prototypeMethod (foo:331:18)",
        "at Array.forEach (native)",
        "at a_named_javascript_function (/some/javascript/file.js:39:5)",
        "/some/line/of/ruby.rb:14"
      ])
      example = double(:Example, :file_path => __FILE__)
      expect {
        formatter.send(:read_failed_line, exception, example)
      }.not_to raise_error
    end

    it "deals gracefully with a security error" do
      exception = double(:Exception, :backtrace => [ "#{__FILE__}:#{__LINE__}"])
      example = double(:Example, :file_path => __FILE__)
      safely do
        expect {
          formatter.send(:read_failed_line, exception, example)
        }.not_to raise_error
      end
    end

    context "when ruby reports a bogus line number in the stack trace" do
      it "reports the filename and that it was unable to find the matching line" do
        exception = double(:Exception, :backtrace => [ "#{__FILE__}:10000000" ])
        example = double(:Example, :file_path => __FILE__)

        msg = formatter.send(:read_failed_line, exception, example)
        expect(msg).to include("Unable to find matching line")
      end
    end

    context "when String alias to_int to_i" do
      before do
        String.class_eval do
          alias :to_int :to_i
        end
      end

      after do
        String.class_eval do
          undef to_int
        end
      end

      it "doesn't hang when file exists" do
        pending("This issue still exists on JRuby, but should be resolved shortly: https://github.com/rspec/rspec-core/issues/295", :if => RUBY_PLATFORM == 'java')
        exception = double(:Exception, :backtrace => [ "#{__FILE__}:#{__LINE__}"])

        example = double(:Example, :file_path => __FILE__)
        expect(formatter.send(:read_failed_line, exception, example)).to eql(
          %Q{        exception = double(:Exception, :backtrace => [ "\#{__FILE__}:\#{__LINE__}"])\n})
      end

    end
  end

  describe "#format_backtrace" do
    let(:rspec_expectations_dir) { "/path/to/rspec-expectations/lib" }
    let(:rspec_core_dir) { "/path/to/rspec-core/lib" }
    let(:backtrace) do
      [
        "#{rspec_expectations_dir}/rspec/matchers/operator_matcher.rb:51:in `eval_match'",
        "#{rspec_expectations_dir}/rspec/matchers/operator_matcher.rb:29:in `=='",
        "./my_spec.rb:5",
        "#{rspec_core_dir}/rspec/core/example.rb:52:in `run'",
        "#{rspec_core_dir}/rspec/core/runner.rb:37:in `run'",
        RSpec::Core::Runner::AT_EXIT_HOOK_BACKTRACE_LINE,
        "./my_spec.rb:3"
      ]
    end

    it "removes lines from rspec and lines that come before the invocation of the at_exit autorun hook" do
      expect(formatter.format_backtrace(backtrace, double.as_null_object)).to eq(["./my_spec.rb:5"])
    end
  end

end
