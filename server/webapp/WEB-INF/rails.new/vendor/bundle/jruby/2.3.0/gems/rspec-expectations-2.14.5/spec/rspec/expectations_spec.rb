module RSpec
  describe Expectations do
    describe '.method_handle_for(object, method_name)' do

      class UntamperedClass
        def foo
          :bar
        end
      end

      class ClassWithMethodOverridden < UntamperedClass
        def method
          :baz
        end
      end

      if RUBY_VERSION.to_f > 1.8
        class BasicClass < BasicObject
          def foo
            :bar
          end
        end

        class BasicClassWithKernel < BasicClass
          include ::Kernel
        end
      end

      it 'fetches method definitions for vanilla objects' do
        object = UntamperedClass.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for objects with method redefined' do
        object = ClassWithMethodOverridden.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for basic objects', :if => RUBY_VERSION.to_i >= 2 do
        object = BasicClass.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for basic objects with kernel mixed in', :if => RUBY_VERSION.to_f > 1.8 do
        object = BasicClassWithKernel.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end
    end
  end
end
