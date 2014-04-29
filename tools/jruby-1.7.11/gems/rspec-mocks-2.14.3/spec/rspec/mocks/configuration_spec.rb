require 'spec_helper'

module RSpec
  module Mocks
    describe Configuration do
      let(:config) { Configuration.new }
      let(:mod_1)  { Module.new }
      let(:mod_2)  { Module.new }

      def instance_methods_of(mod)
        mod_1.instance_methods.map(&:to_sym)
      end

      it 'adds stub and should_receive to the given modules' do
        expect(instance_methods_of(mod_1)).not_to include(:stub, :should_receive)
        expect(instance_methods_of(mod_2)).not_to include(:stub, :should_receive)

        config.add_stub_and_should_receive_to(mod_1, mod_2)

        expect(instance_methods_of(mod_1)).to include(:stub, :should_receive)
        expect(instance_methods_of(mod_2)).to include(:stub, :should_receive)
      end

      shared_examples_for "configuring the syntax" do
        def sandboxed
          orig_syntax = RSpec::Mocks.configuration.syntax
          yield
        ensure
          configure_syntax(orig_syntax)
        end

        around(:each) { |ex| sandboxed(&ex) }
        let(:dbl) { double }
        let(:should_methods)       { [:should_receive, :stub, :should_not_receive] }
        let(:should_class_methods) { [:any_instance] }
        let(:expect_methods)      { [:receive, :allow, :expect_any_instance_of, :allow_any_instance_of] }

        it 'defaults to enabling both the :should and :expect syntaxes' do
          expect(dbl).to respond_to(*should_methods)
          expect(self).to respond_to(*expect_methods)
        end

        context 'when configured to :expect' do
          before { configure_syntax :expect }

          it 'removes the should methods from every object' do
            expect(dbl).not_to respond_to(*should_methods)
          end

          it 'removes `any_instance` from every class' do
            expect(Class.new).not_to respond_to(*should_class_methods)
          end

          it 'adds the expect methods to the example group context' do
            expect(self).to respond_to(*expect_methods)
          end

          it 'reports that the syntax is :expect' do
            expect(configured_syntax).to eq([:expect])
          end

          it 'is a no-op when configured a second time' do
            expect(Syntax.default_should_syntax_host).not_to receive(:method_undefined)
            expect(::RSpec::Mocks::ExampleMethods).not_to receive(:method_added)
            configure_syntax :expect
          end
        end

        context 'when configured to :should' do
          before { configure_syntax :should }

          it 'adds the should methods to every object' do
            expect(dbl).to respond_to(*should_methods)
          end

          it 'adds `any_instance` to every class' do
            expect(Class.new).to respond_to(*should_class_methods)
          end

          it 'removes the expect methods from the example group context' do
            expect(self).not_to respond_to(*expect_methods)
          end

          it 'reports that the syntax is :should' do
            expect(configured_syntax).to eq([:should])
          end

          it 'is a no-op when configured a second time' do
            Syntax.default_should_syntax_host.should_not_receive(:method_added)
            ::RSpec::Mocks::ExampleMethods.should_not_receive(:method_undefined)
            configure_syntax :should
          end
        end

        context 'when configured to [:should, :expect]' do
          before { configure_syntax [:should, :expect] }

          it 'adds the should methods to every object' do
            expect(dbl).to respond_to(*should_methods)
          end

          it 'adds `any_instance` to every class' do
            expect(Class.new).to respond_to(*should_class_methods)
          end

          it 'adds the expect methods to the example group context' do
            expect(self).to respond_to(*expect_methods)
          end

          it 'reports that both syntaxes are enabled' do
            expect(configured_syntax).to eq([:should, :expect])
          end
        end
      end

      describe "configuring rspec-mocks directly" do
        it_behaves_like "configuring the syntax" do
          def configure_syntax(syntax)
            RSpec::Mocks.configuration.syntax = syntax
          end

          def configured_syntax
            RSpec::Mocks.configuration.syntax
          end
        end
      end

      describe "configuring using the rspec-core config API" do
        it_behaves_like "configuring the syntax" do
          def configure_syntax(syntax)
            RSpec.configure do |rspec|
              rspec.mock_with :rspec do |c|
                c.syntax = syntax
              end
            end
          end

          def configured_syntax
            RSpec.configure do |rspec|
              rspec.mock_with :rspec do |c|
                return c.syntax
              end
            end
          end
        end
      end
    end
  end
end

