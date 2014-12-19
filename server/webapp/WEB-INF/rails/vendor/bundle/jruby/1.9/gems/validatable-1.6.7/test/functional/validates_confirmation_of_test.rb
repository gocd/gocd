require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesConfirmationOfTest < Test::Unit::TestCase
    test "given non matching attributes, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name, :name_confirmation
        validates_confirmation_of :name
      end
      instance = klass.new
      instance.name = "foo"
      instance.name_confirmation = "bar"
      instance.valid?
      assert_equal "doesn't match confirmation", instance.errors.on(:name)
    end

    test "given matching attributes, when validated, then no error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name, :name_confirmation
        validates_confirmation_of :name
      end
      instance = klass.new
      instance.name = "foo"
      instance.name_confirmation = "foo"
      assert_equal true, instance.valid?
    end

    test "given matching attributes of different case, when validated with case sensitive false, then no error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name, :name_confirmation
        validates_confirmation_of :name, :case_sensitive => false
      end
      instance = klass.new
      instance.name = "foo"
      instance.name_confirmation = "FOO"
      assert_equal true, instance.valid?
    end

    test "given matching attributes of different case, when validated with case sensitive true, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name, :name_confirmation
        validates_confirmation_of :name
      end
      instance = klass.new
      instance.name = "foo"
      instance.name_confirmation = "FOO"
      assert_equal false, instance.valid?
      assert_equal "doesn't match confirmation", instance.errors.on(:name)
      
    end
  end
end