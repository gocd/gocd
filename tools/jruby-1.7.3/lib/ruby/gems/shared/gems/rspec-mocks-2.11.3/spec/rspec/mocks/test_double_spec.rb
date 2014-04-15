require 'spec_helper'

module RSpec
  module Mocks
    describe TestDouble do
      before(:all) do
        Module.class_eval do
          private
          def use; end
        end
      end

      after(:all) do
        Module.class_eval do
          undef use
        end
      end

      it 'can be extended onto a module to make it a pure test double that can mock private methods' do
        double = Module.new
        double.stub(:use)
        expect { double.use }.to raise_error(/private method `use' called/)

        double = Module.new { TestDouble.extend_onto(self) }
        double.should_receive(:use).and_return(:ok)
        double.use.should be(:ok)
      end

      it 'sets the test double name when a name is passed' do
        double = Module.new { TestDouble.extend_onto(self, "MyDouble") }
        expect { double.foo }.to raise_error(/Mock "MyDouble" received/)
      end

      it 'stubs the methods passed in the stubs hash' do
        double = Module.new do
          TestDouble.extend_onto(self, "MyDouble", :a => 5, :b => 10)
        end

        double.a.should eq(5)
        double.b.should eq(10)
      end

      it 'indicates what type of test double it is in error messages' do
        double = Module.new do
          TestDouble.extend_onto(self, "A", :__declared_as => "ModuleMock")
        end
        expect { double.foo }.to raise_error(/ModuleMock "A"/)
      end

      it 'is declared as a mock by default' do
        double = Module.new { TestDouble.extend_onto(self) }
        expect { double.foo }.to raise_error(/Mock received/)
      end
    end
  end
end

