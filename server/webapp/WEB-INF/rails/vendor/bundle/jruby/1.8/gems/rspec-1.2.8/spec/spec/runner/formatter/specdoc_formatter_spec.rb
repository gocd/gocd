require File.dirname(__FILE__) + '/../../../spec_helper.rb'
require 'spec/runner/formatter/specdoc_formatter'

module Spec
  module Runner
    module Formatter
      describe SpecdocFormatter do
        with_sandboxed_options do
          attr_reader :io, :formatter, :example_group
          before(:each) do
            @io = StringIO.new
            options.stub!(:dry_run).and_return(false)
            options.stub!(:colour).and_return(false)
            @formatter = SpecdocFormatter.new(options, io)
            @example_group = ::Spec::Example::ExampleGroup.describe("ExampleGroup") do
              specify "example" do
              end
            end
          end

          describe "where ExampleGroup has no superclasss with a description" do
            before do
              example_group_started
            end

            def example_group_started
              formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(example_group))
            end

            describe "#dump_summary" do
              it "should produce standard summary without pending when pending has a 0 count" do
                formatter.dump_summary(3, 2, 1, 0)
                io.string.should have_example_group_output("\nFinished in 3 seconds\n\n2 examples, 1 failure\n")
              end

              it "should produce standard summary" do
                formatter.dump_summary(3, 2, 1, 4)
                io.string.should have_example_group_output("\nFinished in 3 seconds\n\n2 examples, 1 failure, 4 pending\n")
              end
            end

            describe "#example_group_started" do
              it "should push ExampleGroup name" do
                io.string.should eql("\nExampleGroup\n")
              end
            end

            describe "#example_failed" do
              describe "where ExampleGroup has no superclasss with a description" do
                describe "when having an error" do
                  it "should push failing spec name and failure number" do
                    formatter.example_failed(
                      example_group.it("spec"),
                      98,
                      Spec::Runner::Reporter::Failure.new("g", "c s", RuntimeError.new)
                    )
                    io.string.should have_example_group_output("- spec (FAILED - 98)\n")
                  end
                end

                describe "when having an expectation failure" do
                  it "should push failing spec name and failure number" do
                    formatter.example_failed(
                      example_group.it("spec"),
                      98,
                      Spec::Runner::Reporter::Failure.new("g", "c s", Spec::Expectations::ExpectationNotMetError.new)
                    )
                    io.string.should have_example_group_output("- spec (FAILED - 98)\n")
                  end
                end
              end

              describe "where ExampleGroup has two superclasses with a description" do
                attr_reader :child_example_group, :grand_child_example_group
              
                def example_group_started
                  @child_example_group = Class.new(example_group).describe("Child ExampleGroup")
                  @grand_child_example_group = Class.new(child_example_group).describe("GrandChild ExampleGroup")
                  formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(grand_child_example_group))
                end

                describe "when having an error" do
                  it "should push failing spec name and failure number" do
                    formatter.example_failed(
                    example_group.it("spec"),
                    98,
                    Spec::Runner::Reporter::Failure.new("g", "c s", RuntimeError.new)
                    )
                    io.string.should have_nested_example_group_output("- spec (FAILED - 98)\n")
                  end
                end

                describe "when having an expectation" do
                  it "should push failing spec name and failure number" do
                    formatter.example_failed(
                      example_group.it("spec"),
                      98,
                      Spec::Runner::Reporter::Failure.new("g", "c s", Spec::Expectations::ExpectationNotMetError.new)
                    )
                    io.string.should have_nested_example_group_output("- spec (FAILED - 98)\n")
                  end
                end

                def have_nested_example_group_output(expected_output)
                  expected_full_output = "\nExampleGroup Child ExampleGroup GrandChild ExampleGroup\n#{expected_output}"
                  ::Spec::Matchers::SimpleMatcher.new(expected_full_output) do |actual|
                    actual == expected_full_output
                  end
                end
              end
            end

            describe "#start" do
              it "should push nothing on start" do
                formatter.start(5)
                io.string.should have_example_group_output("")
              end
            end
          
            describe "#start_dump" do
              it "should push nothing on start dump" do
                formatter.start_dump
                io.string.should have_example_group_output("")
              end
            end

            describe "#example_passed" do
              it "should push passing spec name" do
                formatter.example_passed(example_group.it("spec"))
                io.string.should have_example_group_output("- spec\n")
              end
            end

            describe "#example_pending" do
              it "should push pending example name and message" do
                formatter.example_pending(example_group.examples.first, 'reason', "#{__FILE__}:#{__LINE__}")
                io.string.should have_example_group_output("- example (PENDING: reason)\n")
              end

              it "should dump pending" do
                formatter.example_pending(example_group.examples.first, 'reason', "#{__FILE__}:#{__LINE__}")
                io.rewind
                formatter.dump_pending
                io.string.should =~ /Pending\:\n\nExampleGroup example \(reason\)\n/
              end
            end

            def have_example_group_output(expected_output)
              expected = "\nExampleGroup\n#{expected_output}"
              ::Spec::Matchers::SimpleMatcher.new(expected) do |actual|
                actual == expected
              end
            end
          end
        end
      end
    end
  end
end
