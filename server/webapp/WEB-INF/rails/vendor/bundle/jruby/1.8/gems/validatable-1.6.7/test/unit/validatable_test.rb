require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  expect false do
    validation = stub_everything(:should_validate? => true, :attribute => "attribute", :level => 1, :groups => [])
    klass = Class.new do
      include Validatable
      validations << validation
    end
    klass.new.valid?
  end
  
  expect true do
    klass = Class.new do
      include Validatable
    end
    instance = klass.new
    instance.errors.add(:attribute, "message")
    instance.valid?
    instance.errors.empty?
  end
  
  expect false do
    klass = Class.new do
      include Validatable
    end
    klass.validation_keys_include?("anything")
  end
  
  expect true do
    validation = stub_everything(:key => "key")
    klass = Class.new do
      include Validatable
      validations << validation
    end
    klass.validation_keys_include?("key")
  end
end
