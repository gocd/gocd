require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesFormatOfTest < Test::Unit::TestCase
    test "given invalid name, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_true_for :name, :logic => lambda { name == "nombre" }
      end
      instance = klass.new
      instance.valid?
      assert_equal "is invalid", instance.errors.on(:name)
    end

    test "given valid name, when validated, then no error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_true_for :name, :logic => lambda { name == "nombre" }
      end
      instance = klass.new
      instance.name = "nombre"
      assert_equal true, instance.valid?
    end
  end
end