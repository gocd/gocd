require 'spec_helper'
require 'pp'
require 'stringio'

describe RSpec::Core::Example, :parent_metadata => 'sample' do
  let(:example_group) do
    RSpec::Core::ExampleGroup.describe('group description')
  end

  let(:example_instance) do
    example_group.example('example description') { }
  end

  it_behaves_like "metadata hash builder" do
    def metadata_hash(*args)
      example = example_group.example('example description', *args)
      example.metadata
    end
  end

  def capture_stdout
    orig_stdout = $stdout
    $stdout = StringIO.new
    yield
    $stdout.string
  ensure
    $stdout = orig_stdout
  end

  it 'can be pretty printed' do
    output = ignoring_warnings { capture_stdout { pp example_instance } }
    expect(output).to include("RSpec::Core::Example")
  end

  describe "#exception" do
    it "supplies the first exception raised, if any" do
      example = example_group.example { raise "first" }
      example_group.after { raise "second" }
      example_group.run
      expect(example.exception.message).to eq("first")
    end

    it "returns nil if there is no exception" do
      example = example_group.example('example') { }
      example_group.run
      expect(example.exception).to be_nil
    end

    it "returns false for pending_fixed? if not pending fixed" do
      example = example_group.example { fail }
      example_group.run
      expect(example.exception).not_to be_pending_fixed
    end

    it "returns true for pending_fixed? if pending fixed" do
      example = example_group.example do
        pending("fixed") {}
      end
      example_group.run
      expect(example.exception).to be_pending_fixed
    end
  end

  describe "when there is an explicit description" do
    context "when RSpec.configuration.format_docstrings is set to a block" do
      it "formats the description using the block" do
        RSpec.configuration.format_docstrings { |s| s.strip }
        example = example_group.example(' an example with whitespace ') {}
        example_group.run
        expect(example.description).to eql('an example with whitespace')
      end
    end
  end

  describe "when there is no explicit description" do
    def expect_with(*frameworks)
      RSpec.configuration.stub(:expecting_with_rspec?).and_return(frameworks.include?(:rspec))

      if frameworks.include?(:stdlib)
        example_group.class_eval do
          def assert(val)
            raise "Expected #{val} to be true" unless val
          end
        end
      end
    end

    context "when RSpec.configuration.format_docstrings is set to a block" do
      it "formats the description using the block" do
        RSpec.configuration.format_docstrings { |s| s.upcase }
        example_group.example { expect(5).to eq(5) }
        example_group.run
        pattern = /EXAMPLE AT #{relative_path(__FILE__).upcase}:#{__LINE__ - 2}/
        expect(example_group.examples.first.description).to match(pattern)
      end
    end

    context "when `expect_with :rspec` is configured" do
      before(:each) { expect_with :rspec }

      it "uses the matcher-generated description" do
        example_group.example { expect(5).to eq(5) }
        example_group.run
        expect(example_group.examples.first.description).to eq("should eq 5")
      end

      it "uses the matcher-generated description in the full description" do
        example_group.example { expect(5).to eq(5) }
        example_group.run
        expect(example_group.examples.first.full_description).to eq("group description should eq 5")
      end

      it "uses the file and line number if there is no matcher-generated description" do
        example = example_group.example {}
        example_group.run
        expect(example.description).to match(/example at #{relative_path(__FILE__)}:#{__LINE__ - 2}/)
      end

      it "uses the file and line number if there is an error before the matcher" do
        example = example_group.example { expect(5).to eq(5) }
        example_group.before { raise }
        example_group.run
        expect(example.description).to match(/example at #{relative_path(__FILE__)}:#{__LINE__ - 3}/)
      end
    end

    context "when `expect_with :rspec, :stdlib` is configured" do
      before(:each) { expect_with :rspec, :stdlib }

      it "uses the matcher-generated description" do
        example_group.example { expect(5).to eq(5) }
        example_group.run
        expect(example_group.examples.first.description).to eq("should eq 5")
      end

      it "uses the file and line number if there is no matcher-generated description" do
        example = example_group.example {}
        example_group.run
        expect(example.description).to match(/example at #{relative_path(__FILE__)}:#{__LINE__ - 2}/)
      end

      it "uses the file and line number if there is an error before the matcher" do
        example = example_group.example { expect(5).to eq(5) }
        example_group.before { raise }
        example_group.run
        expect(example.description).to match(/example at #{relative_path(__FILE__)}:#{__LINE__ - 3}/)
      end
    end

    context "when `expect_with :stdlib` is configured" do
      before(:each) { expect_with :stdlib }

      it "does not attempt to get the generated description from RSpec::Matchers" do
        RSpec::Matchers.should_not_receive(:generated_description)
        example_group.example { assert 5 == 5 }
        example_group.run
      end

      it "uses the file and line number" do
        example = example_group.example { assert 5 == 5 }
        example_group.run
        expect(example.description).to match(/example at #{relative_path(__FILE__)}:#{__LINE__ - 2}/)
      end
    end
  end

  describe '#described_class' do
    it "returns the class (if any) of the outermost example group" do
      expect(described_class).to eq(RSpec::Core::Example)
    end
  end

  describe "accessing metadata within a running example" do
    it "has a reference to itself when running" do
      expect(example.description).to eq("has a reference to itself when running")
    end

    it "can access the example group's top level metadata as if it were its own" do
      expect(example.example_group.metadata).to include(:parent_metadata => 'sample')
      expect(example.metadata).to include(:parent_metadata => 'sample')
    end
  end

  describe "accessing options within a running example" do
    it "can look up option values by key", :demo => :data do
      expect(example.metadata[:demo]).to eq(:data)
    end
  end

  describe "#run" do
    it "sets its reference to the example group instance to nil" do
      group = RSpec::Core::ExampleGroup.describe do
        example('example') { expect(1).to eq(1) }
      end
      group.run
      expect(group.examples.first.instance_variable_get("@example_group_instance")).to be_nil
    end

    it "runs after(:each) when the example passes" do
      after_run = false
      group = RSpec::Core::ExampleGroup.describe do
        after(:each) { after_run = true }
        example('example') { expect(1).to eq(1) }
      end
      group.run
      expect(after_run).to be_true, "expected after(:each) to be run"
    end

    it "runs after(:each) when the example fails" do
      after_run = false
      group = RSpec::Core::ExampleGroup.describe do
        after(:each) { after_run = true }
        example('example') { expect(1).to eq(2) }
      end
      group.run
      expect(after_run).to be_true, "expected after(:each) to be run"
    end

    it "runs after(:each) when the example raises an Exception" do
      after_run = false
      group = RSpec::Core::ExampleGroup.describe do
        after(:each) { after_run = true }
        example('example') { raise "this error" }
      end
      group.run
      expect(after_run).to be_true, "expected after(:each) to be run"
    end

    context "with an after(:each) that raises" do
      it "runs subsequent after(:each)'s" do
        after_run = false
        group = RSpec::Core::ExampleGroup.describe do
          after(:each) { after_run = true }
          after(:each) { raise "FOO" }
          example('example') { expect(1).to eq(1) }
        end
        group.run
        expect(after_run).to be_true, "expected after(:each) to be run"
      end

      it "stores the exception" do
        group = RSpec::Core::ExampleGroup.describe
        group.after(:each) { raise "FOO" }
        example = group.example('example') { expect(1).to eq(1) }

        group.run

        expect(example.metadata[:execution_result][:exception].message).to eq("FOO")
      end
    end

    it "wraps before/after(:each) inside around" do
      results = []
      group = RSpec::Core::ExampleGroup.describe do
        around(:each) do |e|
          results << "around (before)"
          e.run
          results << "around (after)"
        end
        before(:each) { results << "before" }
        after(:each) { results << "after" }
        example { results << "example" }
      end

      group.run
      expect(results).to eq([
                          "around (before)",
                          "before",
                          "example",
                          "after",
                          "around (after)"
                        ])
    end

    context "clearing ivars" do
      it "sets ivars to nil to prep them for GC" do
        group = RSpec::Core::ExampleGroup.describe do
          before(:all)  { @before_all  = :before_all }
          before(:each) { @before_each = :before_each }
          after(:each)  { @after_each = :after_each }
          after(:all)   { @after_all  = :after_all }
        end
        group.example("does something") do
          expect(@before_all).to eq(:before_all)
          expect(@before_each).to eq(:before_each)
        end
        expect(group.run(double.as_null_object)).to be_true
        group.new do |example|
          %w[@before_all @before_each @after_each @after_all].each do |ivar|
            expect(example.instance_variable_get(ivar)).to be_nil
          end
        end
      end

      it "does not impact the before_all_ivars which are copied to each example" do
        group = RSpec::Core::ExampleGroup.describe do
          before(:all) { @before_all = "abc" }
          example("first") { expect(@before_all).not_to be_nil }
          example("second") { expect(@before_all).not_to be_nil }
        end
        expect(group.run).to be_true
      end
    end

    context 'when the example raises an error' do
      def run_and_capture_reported_message(group)
        reported_msg = nil
        # We can't use should_receive(:message).with(/.../) here,
        # because if that fails, it would fail within our example-under-test,
        # and since there's already two errors, it would just be reported again.
        RSpec.configuration.reporter.stub(:message) { |msg| reported_msg = msg }
        group.run
        reported_msg
      end

      it "prints any around hook errors rather than silencing them" do
        group = RSpec::Core::ExampleGroup.describe do
          around(:each) { |e| e.run; raise "around" }
          example("e") { raise "example" }
        end

        message = run_and_capture_reported_message(group)
        expect(message).to match(/An error occurred in an around.* hook/i)
      end

      it "prints any after hook errors rather than silencing them" do
        group = RSpec::Core::ExampleGroup.describe do
          after(:each) { raise "after" }
          example("e") { raise "example" }
        end

        message = run_and_capture_reported_message(group)
        expect(message).to match(/An error occurred in an after.* hook/i)
      end

      it 'does not print mock expectation errors' do
        group = RSpec::Core::ExampleGroup.describe do
          example do
            foo = double
            foo.should_receive(:bar)
            raise "boom"
          end
        end

        message = run_and_capture_reported_message(group)
        expect(message).to be_nil
      end
    end
  end

  describe "#pending" do
    context "in the example" do
      it "sets the example to pending" do
        group = RSpec::Core::ExampleGroup.describe do
          example { pending }
        end
        group.run
        expect(group.examples.first).to be_pending
      end

      it "allows post-example processing in around hooks (see https://github.com/rspec/rspec-core/issues/322)" do
        blah = nil
        group = RSpec::Core::ExampleGroup.describe do
          around do |example|
            example.run
            blah = :success
          end
          example { pending }
        end
        group.run
        expect(blah).to be(:success)
      end
    end

    context "in before(:each)" do
      it "sets each example to pending" do
        group = RSpec::Core::ExampleGroup.describe do
          before(:each) { pending }
          example {}
          example {}
        end
        group.run
        expect(group.examples.first).to be_pending
        expect(group.examples.last).to be_pending
      end
    end

    context "in before(:all)" do
      it "sets each example to pending" do
        group = RSpec::Core::ExampleGroup.describe do
          before(:all) { pending }
          example {}
          example {}
        end
        group.run
        expect(group.examples.first).to be_pending
        expect(group.examples.last).to be_pending
      end
    end

    context "in around(:each)" do
      it "sets the example to pending" do
        group = RSpec::Core::ExampleGroup.describe do
          around(:each) { pending }
          example {}
        end
        group.run
        expect(group.examples.first).to be_pending
      end
    end
  end

  describe "timing" do
    it "uses RSpec::Core::Time as to not be affected by changes to time in examples" do
      reporter = double(:reporter).as_null_object
      group = RSpec::Core::ExampleGroup.describe
      example = group.example
      example.__send__ :start, reporter
      Time.stub(:now => Time.utc(2012, 10, 1))
      example.__send__ :finish, reporter
      expect(example.metadata[:execution_result][:run_time]).to be < 0.2
    end
  end

  it 'does not interfere with per-example randomness when running examples in a random order' do
    values = []

    RSpec.configuration.order = :random

    RSpec::Core::ExampleGroup.describe do
      # The bug was only triggered when the examples
      # were in nested contexts; see https://github.com/rspec/rspec-core/pull/837
      context { example { values << rand } }
      context { example { values << rand } }
    end.run

    expect(values.uniq).to have(2).values
  end
end
