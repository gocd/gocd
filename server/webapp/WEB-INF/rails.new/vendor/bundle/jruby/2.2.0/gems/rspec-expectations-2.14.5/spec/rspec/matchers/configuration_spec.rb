require 'spec_helper'
require 'delegate'

module RSpec
  module Matchers
    describe "RSpec::Matchers.configuration" do
      it 'returns a memoized configuration instance' do
        expect(RSpec::Matchers.configuration).to be_a(RSpec::Matchers::Configuration)
        expect(RSpec::Matchers.configuration).to be(RSpec::Matchers.configuration)
      end
    end

    describe Configuration do
      let(:config) { Configuration.new }

      describe "#backtrace_formatter" do
        let(:original_backtrace) { %w[ clean-me/a.rb other/file.rb clean-me/b.rb ] }
        let(:cleaned_backtrace)  { %w[ other/file.rb ] }

        let(:formatted_backtrace) do
          config.backtrace_formatter.format_backtrace(original_backtrace)
        end

        before do
          @old_patterns = RSpec.configuration.backtrace_exclusion_patterns
          RSpec.configuration.backtrace_exclusion_patterns = [/clean-me/]
        end

        after do
          RSpec.configuration.backtrace_exclusion_patterns = @old_patterns
        end

        it "defaults to rspec-core's backtrace formatter when rspec-core is loaded" do
          expect(config.backtrace_formatter).to be(RSpec::Core::BacktraceFormatter)
          expect(formatted_backtrace).to eq(cleaned_backtrace)
        end

        it "defaults to a null formatter when rspec-core is not loaded" do
          hide_const("RSpec::Core::BacktraceFormatter")
          expect(formatted_backtrace).to eq(original_backtrace)
        end

        it "can be set to another backtrace formatter" do
          config.backtrace_formatter = double(:format_backtrace => ['a'])
          expect(formatted_backtrace).to eq(['a'])
        end
      end

      context 'on an interpreter that does not provide BasicObject', :uses_should, :unless => defined?(::BasicObject) do
        before { RSpec::Expectations::Syntax.disable_should(Delegator) }

        let(:klass) do
          Class.new(SimpleDelegator) do
            def delegated?; true; end
          end
        end

        let(:instance) { klass.new(Object.new) }

        it 'provides a means to manually add it Delegator' do
          instance.should_not respond_to(:delegated?) # because #should is being delegated...
          config.add_should_and_should_not_to Delegator
          instance.should respond_to(:delegated?) # now it should work!
        end
      end

      shared_examples_for "configuring the expectation syntax" do
        before do
          @orig_syntax = RSpec::Matchers.configuration.syntax
        end

        after do
          configure_syntax(@orig_syntax)
        end

        it 'can limit the syntax to :should' do
          configure_syntax :should
          configured_syntax.should eq([:should])

          3.should eq(3)
          3.should_not eq(4)
          lambda { expect(6).to eq(6) }.should raise_error(NameError)
        end

        it 'is a no-op when configured to :should twice' do
          configure_syntax :should
          Expectations::Syntax.default_should_host.should_not_receive(:method_added)
          configure_syntax :should
          RSpec::Mocks.verify # because configure_syntax is called again in an after hook
        end

        it 'can limit the syntax to :expect' do
          configure_syntax :expect
          expect(configured_syntax).to eq([:expect])

          expect(3).to eq(3)
          expect { 3.should eq(3) }.to raise_error(NameError)
          expect { 3.should_not eq(3) }.to raise_error(NameError)
        end

        it 'is a no-op when configured to :expect twice' do
          RSpec::Matchers.stub(:method_added).and_raise("no methods should be added here")

          configure_syntax :expect
          configure_syntax :expect
        end

        it 'can re-enable the :should syntax' do
          configure_syntax :expect
          configure_syntax [:should, :expect]
          configured_syntax.should eq([:should, :expect])

          3.should eq(3)
          3.should_not eq(4)
          expect(3).to eq(3)
        end

        it 'can re-enable the :expect syntax' do
          configure_syntax :should
          configure_syntax [:should, :expect]
          configured_syntax.should eq([:should, :expect])

          3.should eq(3)
          3.should_not eq(4)
          expect(3).to eq(3)
        end

        it 'does not add the deprecated #should to ExpectationTarget when only :should is enabled' do
          et = Expectations::ExpectationTarget

          configure_syntax :should
          et.new(Proc.new {}).should be_an(et)
          et.new(Proc.new {}).should_not be_a(Proc)
        end

        it 'does not add the deprecated #should to ExpectationTarget when only :expect is enabled' do
          configure_syntax :expect
          expect(expect(3)).not_to respond_to(:should)
          expect(expect(3)).not_to respond_to(:should_not)
        end

        context 'when both :expect and :should are enabled' do
          before { allow(RSpec).to receive(:deprecate) }

          it 'allows `expect {}.should` to be used' do
            configure_syntax [:should, :expect]
            expect { raise "boom" }.should raise_error("boom")
            expect { }.should_not raise_error
          end

          it 'prints a deprecation notice when `expect {}.should` is used' do
            configure_syntax [:should, :expect]

            expect(RSpec).to receive(:deprecate)
            expect { raise "boom" }.should raise_error("boom")

            expect(RSpec).to receive(:deprecate)
            expect { }.should_not raise_error
          end
        end
      end

      describe "configuring rspec-expectations directly" do
        it_behaves_like "configuring the expectation syntax" do
          def configure_syntax(syntax)
            RSpec::Matchers.configuration.syntax = syntax
          end

          def configured_syntax
            RSpec::Matchers.configuration.syntax
          end
        end
      end

      describe "configuring using the rspec-core config API" do
        it_behaves_like "configuring the expectation syntax" do
          def configure_syntax(syntax)
            RSpec.configure do |rspec|
              rspec.expect_with :rspec do |c|
                c.syntax = syntax
              end
            end
          end

          def configured_syntax
            RSpec.configure do |rspec|
              rspec.expect_with :rspec do |c|
                return c.syntax
              end
            end
          end
        end
      end

      it 'enables both syntaxes by default' do
        # This is kinda a hack, but since we want to enforce use of
        # the expect syntax within our specs here, we have modified the
        # config setting, which makes it hard to get at the original
        # default value. in spec_helper.rb we store the default value
        # in $default_expectation_syntax so we can use it here.
        expect($default_expectation_syntax).to match_array([:expect, :should])
      end
    end
  end
end

