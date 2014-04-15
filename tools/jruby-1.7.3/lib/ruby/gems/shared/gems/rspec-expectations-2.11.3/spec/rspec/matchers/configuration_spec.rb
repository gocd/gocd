require 'spec_helper'
require 'delegate'

module RSpec
  module Matchers
    describe ".configuration" do
      it 'returns a memoized configuration instance' do
        RSpec::Matchers.configuration.should be_a(RSpec::Matchers::Configuration)
        RSpec::Matchers.configuration.should be(RSpec::Matchers.configuration)
      end

      context 'on an interpreter that does not provide BasicObject', :unless => defined?(::BasicObject) do
        before { RSpec::Expectations::Syntax.disable_should(Delegator) }

        let(:klass) do
          Class.new(SimpleDelegator) do
            def delegated?; true; end
          end
        end

        let(:instance) { klass.new(Object.new) }

        it 'provides a means to manually add it Delegator' do
          instance.should_not respond_to(:delegated?) # because #should is being delegated...
          RSpec::Matchers.configuration.add_should_and_should_not_to Delegator
          instance.should respond_to(:delegated?) # now it should work!
        end
      end
    end

    shared_examples_for "configuring the expectation syntax" do
      # We want a sandboxed method that ensures that we wind up with
      # both syntaxes properly enabled when the example ends.
      #
      # On platforms that fork, using a sub process is the easiest,
      # most robust way to achieve that.
      #
      # On jRuby we just re-enable both syntaxes at the end of the example;
      # however, this is a generally inferior approach because it depends on
      # the code-under-test working properly; if it doesn't work properly,
      # it could leave things in a "broken" state where tons of other examples fail.
      if RUBY_PLATFORM == "java"
        def sandboxed
          yield
        ensure
          configure_syntax([:should, :expect])
        end
      else
        include InSubProcess
        alias sandboxed in_sub_process
      end

      it 'is configured to :should and :expect by default' do
        configured_syntax.should eq([:should, :expect])

        3.should eq(3)
        3.should_not eq(4)
        expect(3).to eq(3)
      end

      it 'can limit the syntax to :should' do
        sandboxed do
          configure_syntax :should
          configured_syntax.should eq([:should])

          3.should eq(3)
          3.should_not eq(4)
          lambda { expect(6).to eq(6) }.should raise_error(NameError)
        end
      end

      it 'is a no-op when configured to :should twice' do
        sandboxed do
          ::Kernel.stub(:method_added).and_raise("no methods should be added here")

          configure_syntax :should
          configure_syntax :should
        end
      end

      it 'can limit the syntax to :expect' do
        sandboxed do
          configure_syntax :expect
          expect(configured_syntax).to eq([:expect])

          expect(3).to eq(3)
          expect { 3.should eq(3) }.to raise_error(NameError)
          expect { 3.should_not eq(3) }.to raise_error(NameError)
        end
      end

      it 'is a no-op when configured to :expect twice' do
        sandboxed do
          RSpec::Matchers.stub(:method_added).and_raise("no methods should be added here")

          configure_syntax :expect
          configure_syntax :expect
        end
      end

      it 'can re-enable the :should syntax' do
        sandboxed do
          configure_syntax :expect
          configure_syntax [:should, :expect]
          configured_syntax.should eq([:should, :expect])

          3.should eq(3)
          3.should_not eq(4)
          expect(3).to eq(3)
        end
      end

      it 'can re-enable the :expect syntax' do
        sandboxed do
          configure_syntax :should
          configure_syntax [:should, :expect]
          configured_syntax.should eq([:should, :expect])

          3.should eq(3)
          3.should_not eq(4)
          expect(3).to eq(3)
        end
      end

      it 'does not add the deprecated #should to ExpectationTarget when only :should is enabled' do
        et = Expectations::ExpectationTarget

        sandboxed do
          configure_syntax :should
          et.new(Proc.new {}).should be_an(et)
          et.new(Proc.new {}).should_not be_a(Proc)
        end
      end

      it 'does not add the deprecated #should to ExpectationTarget when only :expect is enabled' do
        sandboxed do
          configure_syntax :expect
          expect(expect(3)).not_to respond_to(:should)
          expect(expect(3)).not_to respond_to(:should_not)
        end
      end

      context 'when both :expect and :should are enabled' do
        before { RSpec.stub(:warn) }

        it 'allows `expect {}.should` to be used' do
          sandboxed do
            configure_syntax [:should, :expect]
            expect { raise "boom" }.should raise_error("boom")
            expect { }.should_not raise_error
          end
        end

        it 'prints a deprecation notice when `expect {}.should` is used' do
          sandboxed do
            configure_syntax [:should, :expect]

            RSpec.should_receive(:warn).with(/please use `expect \{ \}.to.*instead/)
            expect { raise "boom" }.should raise_error("boom")

            RSpec.should_receive(:warn).with(/please use `expect \{ \}.to_not.*instead/)
            expect { }.should_not raise_error
          end
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

  end
end

