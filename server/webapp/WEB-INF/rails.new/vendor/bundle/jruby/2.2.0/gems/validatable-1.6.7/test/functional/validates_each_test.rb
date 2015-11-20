require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

functional_tests do
  expect :is_set do
    klass = Class.new do
      include Validatable
      attr_accessor :name, :result
      validates_each :name, :logic => lambda { @result = :is_set }
    end
    instance = klass.new
    instance.valid?
    instance.result
  end
end