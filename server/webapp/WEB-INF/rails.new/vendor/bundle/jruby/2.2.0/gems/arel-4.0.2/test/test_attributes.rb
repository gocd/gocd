require 'helper'

module Arel
  describe 'Attributes' do
    it 'responds to lower' do
      relation  = Table.new(:users)
      attribute = relation[:foo]
      node      = attribute.lower
      assert_equal 'LOWER', node.name
      assert_equal [attribute], node.expressions
    end

    describe 'equality' do
      it 'is equal with equal ivars' do
        array = [Attribute.new('foo', 'bar'), Attribute.new('foo', 'bar')]
        assert_equal 1, array.uniq.size
      end

      it 'is not equal with different ivars' do
        array = [Attribute.new('foo', 'bar'), Attribute.new('foo', 'baz')]
        assert_equal 2, array.uniq.size
      end
    end

    describe 'for' do
      it 'deals with unknown column types' do
        column = Struct.new(:type).new :crazy
        Attributes.for(column).must_equal Attributes::Undefined
      end

      it 'returns the correct constant for strings' do
        [:string, :text, :binary].each do |type|
          column = Struct.new(:type).new type
          Attributes.for(column).must_equal Attributes::String
        end
      end

      it 'returns the correct constant for ints' do
        column = Struct.new(:type).new :integer
        Attributes.for(column).must_equal Attributes::Integer
      end

      it 'returns the correct constant for floats' do
        column = Struct.new(:type).new :float
        Attributes.for(column).must_equal Attributes::Float
      end

      it 'returns the correct constant for decimals' do
        column = Struct.new(:type).new :decimal
        Attributes.for(column).must_equal Attributes::Decimal
      end

      it 'returns the correct constant for boolean' do
        column = Struct.new(:type).new :boolean
        Attributes.for(column).must_equal Attributes::Boolean
      end

      it 'returns the correct constant for time' do
        [:date, :datetime, :timestamp, :time].each do |type|
          column = Struct.new(:type).new type
          Attributes.for(column).must_equal Attributes::Time
        end
      end
    end
  end
end
