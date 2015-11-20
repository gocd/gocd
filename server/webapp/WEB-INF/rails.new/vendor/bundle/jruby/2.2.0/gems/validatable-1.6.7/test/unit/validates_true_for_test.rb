require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  
  expect false do
    validation = Validatable::ValidatesTrueFor.new stub_everything, :name, :logic => lambda { false }
    validation.valid?(stub_everything)
  end
  
  expect true do
    validation = Validatable::ValidatesTrueFor.new stub_everything, :name, :logic => lambda { true }
    validation.valid?(stub_everything)
  end
  
  expect ArgumentError do
    validation = Validatable::ValidatesTrueFor.new stub_everything, :age
  end
  
  expect true do
    options = [:message, :if, :times, :level, :groups, :logic, :key]
    Validatable::ValidatesTrueFor.new(stub_everything, :name, options.to_blank_options_hash).must_understand(options.to_blank_options_hash)
  end
  
  expect true do
    options = [:logic]
    Validatable::ValidatesTrueFor.new(stub_everything, :name, options.to_blank_options_hash).requires(options.to_blank_options_hash)
  end
  
end