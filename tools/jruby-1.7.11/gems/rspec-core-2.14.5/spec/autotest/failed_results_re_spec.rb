require "spec_helper"

describe "failed_results_re for autotest" do
  def run_example
    group = RSpec::Core::ExampleGroup.describe("group")
    group.example("example") { yield }
    io = StringIO.new
    formatter = RSpec::Core::Formatters::BaseTextFormatter.new(io)
    reporter = RSpec::Core::Reporter.new(formatter)

    group.run(reporter)
    reporter.report(1, nil) {}
    io.string
  end

  shared_examples "autotest failed_results_re" do
    it "matches a failure" do
      output = run_example { fail }
      expect(output).to match(Autotest::Rspec2.new.failed_results_re)
      expect(output).to include(__FILE__.sub(File.expand_path('.'),'.'))
    end

    it "does not match when there are no failures" do
      output = run_example { } # pass
      expect(output).not_to match(Autotest::Rspec2.new.failed_results_re)
      expect(output).not_to include(__FILE__.sub(File.expand_path('.'),'.'))
    end
  end

  context "with color enabled" do
    before do
      RSpec.configuration.stub(:color_enabled? => true)
    end

    include_examples "autotest failed_results_re"
  end

  context "with color disabled " do
    before do
      RSpec.configuration.stub(:color_enabled? => false)
    end

    include_examples "autotest failed_results_re"
  end
end
