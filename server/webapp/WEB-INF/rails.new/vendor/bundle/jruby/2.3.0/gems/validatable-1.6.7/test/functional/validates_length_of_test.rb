require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesLengthOfTest < Test::Unit::TestCase
    test "given short value, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_length_of :name, :minimum => 2
      end
      instance = klass.new
      instance.valid?
      assert_equal "is invalid", instance.errors.on(:name)
    end
    
    test "given is constraint, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_length_of :name, :is => 2
      end

      instance = klass.new
      instance.valid?
      assert_equal "is invalid", instance.errors.on(:name)
    end

    test "given is constraint is met, when validated, then valid is true" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_length_of :name, :is => 2
      end

      instance = klass.new
      instance.name = "bk"
      assert_equal true, instance.valid?
    end
    
    test "given within constraint, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_length_of :name, :within => 2..4
      end

      instance = klass.new
      instance.valid?
      assert_equal "is invalid", instance.errors.on(:name)
    end

    test "given within constraint, when validated, then valid is true" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_length_of :name, :within => 2..4
      end

      instance = klass.new
      instance.name = "bk"
      assert_equal true, instance.valid?
    end
  end
end