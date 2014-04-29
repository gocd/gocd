require 'spec_helper'
require 'rspec/core/formatters/documentation_formatter'

module RSpec::Core::Formatters
  describe DocumentationFormatter do
    it "numbers the failures" do

      examples = [
        double("example 1",
               :description => "first example",
               :execution_result => {:status => 'failed', :exception => Exception.new }
              ),
        double("example 2",
               :description => "second example",
               :execution_result => {:status => 'failed', :exception => Exception.new }
              )
      ]

      output = StringIO.new
      RSpec.configuration.stub(:color_enabled?) { false }

      formatter = RSpec::Core::Formatters::DocumentationFormatter.new(output)

      examples.each {|e| formatter.example_failed(e) }

      expect(output.string).to match(/first example \(FAILED - 1\)/m)
      expect(output.string).to match(/second example \(FAILED - 2\)/m)
    end

    it "represents nested group using hierarchy tree" do
      output = StringIO.new
      RSpec.configuration.stub(:color_enabled?) { false }

      formatter = RSpec::Core::Formatters::DocumentationFormatter.new(output)

      group = RSpec::Core::ExampleGroup.describe("root")
      context1 = group.describe("context 1")
      context1.example("nested example 1.1"){}
      context1.example("nested example 1.2"){}

      context11 = context1.describe("context 1.1")
      context11.example("nested example 1.1.1"){}
      context11.example("nested example 1.1.2"){}

      context2 = group.describe("context 2")
      context2.example("nested example 2.1"){}
      context2.example("nested example 2.2"){}

      group.run(RSpec::Core::Reporter.new(formatter))

      expect(output.string).to eql("
root
  context 1
    nested example 1.1
    nested example 1.2
    context 1.1
      nested example 1.1.1
      nested example 1.1.2
  context 2
    nested example 2.1
    nested example 2.2
")
    end

    it "strips whitespace for each row" do
      output = StringIO.new
      RSpec.configuration.stub(:color_enabled?) { false }

      formatter = RSpec::Core::Formatters::DocumentationFormatter.new(output)

      group = RSpec::Core::ExampleGroup.describe(" root ")
      context1 = group.describe(" nested ")
      context1.example(" example 1 ") {}
      context1.example(" example 2 ", :pending => true){}
      context1.example(" example 3 ") { fail }

      group.run(RSpec::Core::Reporter.new(formatter))

      expect(output.string).to eql("
root
  nested
    example 1
    example 2 (PENDING: No reason given)
    example 3 (FAILED - 1)
")
    end
  end
end
