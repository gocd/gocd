require 'spec_helper'
require 'rspec/core/formatters/deprecation_formatter'
require 'tempfile'

module RSpec::Core::Formatters
  describe DeprecationFormatter do
    let(:deprecation_stream) { StringIO.new }
    let(:summary_stream)     { StringIO.new }
    let(:formatter) { DeprecationFormatter.new deprecation_stream, summary_stream }

    def with_start_defined_on_kernel
      return yield if ::Kernel.method_defined?(:start)

      begin
        ::Kernel.module_eval { def start(*); raise "boom"; end }
        yield
      ensure
        ::Kernel.module_eval { undef start }
      end
    end

    it 'does not blow up when `Kernel` defines `start`' do
      with_start_defined_on_kernel do
        reporter = ::RSpec::Core::Reporter.new(formatter)
        reporter.start(3)
      end
    end

    describe "#deprecation" do
      it "includes the method" do
        formatter.deprecation(:deprecated => "i_am_deprecated")
        deprecation_stream.rewind
        expect(deprecation_stream.read).to match(/i_am_deprecated is deprecated/)
      end

      it "includes the replacement" do
        formatter.deprecation(:replacement => "use_me")
        deprecation_stream.rewind
        expect(deprecation_stream.read).to match(/Use use_me instead/)
      end

      it "includes the call site if provided" do
        formatter.deprecation(:call_site => "somewhere")
        deprecation_stream.rewind
        expect(deprecation_stream.read).to match(/Called from somewhere/)
      end

      it "prints a message if provided, ignoring other data" do
        formatter.deprecation(:message => "this message", :deprecated => "x", :replacement => "y", :call_site => "z")
        deprecation_stream.rewind
        expect(deprecation_stream.read).to eq "this message"
      end
    end

    describe "#deprecation_summary" do
      it "is printed when deprecations go to a file" do
        file = File.open("#{Dir.tmpdir}/deprecation_summary_example_output", "w")
        summary_stream = StringIO.new
        formatter = DeprecationFormatter.new file, summary_stream
        formatter.deprecation(:deprecated => 'i_am_deprecated')
        formatter.deprecation_summary
        summary_stream.rewind
        expect(summary_stream.read).to match(/1 deprecation logged to .*deprecation_summary_example_output/)
      end

      it "pluralizes for more than one deprecation" do
        file = File.open("#{Dir.tmpdir}/deprecation_summary_example_output", "w")
        summary_stream = StringIO.new
        formatter = DeprecationFormatter.new file, summary_stream
        formatter.deprecation(:deprecated => 'i_am_deprecated')
        formatter.deprecation(:deprecated => 'i_am_deprecated_also')
        formatter.deprecation_summary
        summary_stream.rewind
        expect(summary_stream.read).to match(/2 deprecations/)
      end

      it "is not printed when there are no deprecations" do
        file = File.open("#{Dir.tmpdir}/deprecation_summary_example_output", "w")
        summary_stream = StringIO.new
        formatter = DeprecationFormatter.new file, summary_stream
        formatter.deprecation_summary
        summary_stream.rewind
        expect(summary_stream.read).to eq ""
      end

      it "is not printed when deprecations go to an IO instance" do
        summary_stream = StringIO.new
        formatter = DeprecationFormatter.new StringIO.new, summary_stream
        formatter.deprecation(:deprecated => 'i_am_deprecated')
        formatter.deprecation_summary
        summary_stream.rewind
        expect(summary_stream.read).to eq ""
      end
    end
  end
end
