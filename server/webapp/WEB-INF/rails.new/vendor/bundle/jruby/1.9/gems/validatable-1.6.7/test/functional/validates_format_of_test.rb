require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesFormatOfTest < Test::Unit::TestCase
    test "given invalid name format, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_format_of :name, :with => /.+/
      end
      instance = klass.new
      instance.valid?
      assert_equal "is invalid", instance.errors.on(:name)
    end

    test "given invalid name format and nil name, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_format_of :name, :with => /.+/, :if => Proc.new { !name.nil? }
      end
      assert_equal true, klass.new.valid?
    end
    
    test "given invalid name format and a name, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_format_of :name, :with => /.+/, :if => Proc.new { name.nil? }
      end
      assert_equal false, klass.new.valid?
    end
  end
end