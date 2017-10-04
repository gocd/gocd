require 'spec_helper'
require 'rspec/core/formatters/json_formatter'
require 'json'
require 'rspec/core/reporter'

# todo, someday:
# it "lists the groups (describe and context) separately"
# it "includes full 'execution_result'"
# it "relativizes backtrace paths"
# it "includes profile information (implements dump_profile)"
# it "shows the pending message if one was given"
# it "shows the seed if run was randomized"
# it "lists pending specs that were fixed"
describe RSpec::Core::Formatters::JsonFormatter do
  let(:output) { StringIO.new }
  let(:formatter) { RSpec::Core::Formatters::JsonFormatter.new(output) }
  let(:reporter) { RSpec::Core::Reporter.new(formatter) }

  it "outputs json (brittle high level functional test)" do
    group = RSpec::Core::ExampleGroup.describe("one apiece") do
      it("succeeds") { expect(1).to eq 1 }
      it("fails") { fail "eek" }
      it("pends") { pending "world peace" }
    end
    succeeding_line = __LINE__ - 4
    failing_line = __LINE__ - 4
    pending_line = __LINE__ - 4

    now = Time.now
    Time.stub(:now).and_return(now)
    reporter.report(2) do |r|
      group.run(r)
    end

    # grab the actual backtrace -- kind of a cheat
    failing_backtrace = formatter.output_hash[:examples][1][:exception][:backtrace]
    this_file = relative_path(__FILE__)

    expected = {
      :examples => [
        {
          :description => "succeeds",
          :full_description => "one apiece succeeds",
          :status => "passed",
          :file_path => this_file,
          :line_number => succeeding_line,
        },
        {
          :description => "fails",
          :full_description => "one apiece fails",
          :status => "failed",
          :file_path => this_file,
          :line_number => failing_line,
          :exception => {:class => "RuntimeError", :message => "eek", :backtrace => failing_backtrace}
        },
        {
          :description => "pends",
          :full_description => "one apiece pends",
          :status => "pending",
          :file_path => this_file,
          :line_number => pending_line,
        },
      ],
      :summary => {
        :duration => formatter.output_hash[:summary][:duration],
        :example_count => 3,
        :failure_count => 1,
        :pending_count => 1,
      },
      :summary_line => "3 examples, 1 failure, 1 pending"
    }
    expect(formatter.output_hash).to eq expected
    expect(output.string).to eq expected.to_json
  end

  describe "#stop" do
    it "adds all examples to the output hash" do
      formatter.stop
      expect(formatter.output_hash[:examples]).not_to be_nil
    end
  end

  describe "#close" do
    it "outputs the results as a JSON string" do
      expect(output.string).to eq ""
      formatter.close
      expect(output.string).to eq({}.to_json)
    end
  end

  describe "#message" do
    it "adds a message to the messages list" do
      formatter.message("good job")
      expect(formatter.output_hash[:messages]).to eq ["good job"]
    end
  end

  describe "#dump_summary" do
    it "adds summary info to the output hash" do
      duration, example_count, failure_count, pending_count = 1.0, 2, 1, 1
      formatter.dump_summary(duration, example_count, failure_count, pending_count)
      summary = formatter.output_hash[:summary]
      %w(duration example_count failure_count pending_count).each do |key|
        expect(summary[key.to_sym]).to eq eval(key)
      end
      summary_line = formatter.output_hash[:summary_line]
      expect(summary_line).to eq "2 examples, 1 failure, 1 pending"
    end

    it "ignores --profile" do
      allow(RSpec.configuration).to receive(:profile_examples).and_return(true)
      formatter.dump_summary(1.0, 2, 1, 1)
    end
  end
end
