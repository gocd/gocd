require 'spec_helper'

describe Marshal, 'extensions' do
  # An object that raises when code attempts to dup it.
  #
  # Because we manipulate the internals of RSpec::Mocks.space below, we need
  # an object that simply blows up when #dup is called without using any
  # partial mocking or stubbing from rspec-mocks itself.
  class UndupableObject
    def dup
      raise NotImplementedError
    end
  end

  describe '#dump' do
    context 'when rspec-mocks has not been fully initialized' do
      def without_space
        stashed_space, RSpec::Mocks.space = RSpec::Mocks.space, nil
        yield
      ensure
        RSpec::Mocks.space = stashed_space
      end

      it 'does not duplicate the object before serialization' do
        obj = UndupableObject.new
        without_space do
          serialized = Marshal.dump(obj)
          expect(Marshal.load(serialized)).to be_an(UndupableObject)
        end
      end
    end

    context 'when rspec-mocks has been fully initialized' do
      it 'duplicates objects with stubbed or mocked implementations before serialization' do
        obj = double(:foo => "bar")

        serialized = Marshal.dump(obj)
        expect(Marshal.load(serialized)).to be_an(obj.class)
      end

      it 'does not duplicate other objects before serialization' do
        obj = UndupableObject.new

        serialized = Marshal.dump(obj)
        expect(Marshal.load(serialized)).to be_an(UndupableObject)
      end

      it 'does not duplicate nil before serialization' do
        serialized = Marshal.dump(nil)
        expect(Marshal.load(serialized)).to be_nil
      end
    end
  end
end
