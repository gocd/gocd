require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesNumericalityOfTest < Test::Unit::TestCase
    test "when validating numericality and the value is nil an error should exist on the instance" do
      klass = Class.new do
        include Validatable
        attr_accessor :nothing
        validates_numericality_of :nothing
      end
      instance = klass.new
      instance.valid?
      assert_equal "must be a number", instance.errors.on(:nothing)
    end

    test "when validating numericality and the value has a non numeric character an error should exist on the instance" do
      klass = Class.new do
        include Validatable
        validates_numericality_of :some_string
        
        def some_string
          "some_string"
        end
      end
      instance = klass.new
      instance.valid?
      assert_equal "must be a number", instance.errors.on(:some_string)
    end
    
    test "when validating a number no error will be in the instance" do
      klass = Class.new do
        include Validatable
        validates_numericality_of :valid_number
        
        def valid_number
          1.23
        end
      end
      instance = klass.new
      instance.valid?
      assert_equal nil, instance.errors.on(:valid_number)
    end
    
    test "when validating an integer and the value is a decimal an error should exist on the instance" do
      klass = Class.new do
        include Validatable
        validates_numericality_of :valid_number, :only_integer => true
        attr_accessor :valid_number
      end
      
      instance = klass.new
      instance.valid_number = 1.23
      instance.valid?
      assert_equal "must be a number", instance.errors.on(:valid_number)
    end
  end
end