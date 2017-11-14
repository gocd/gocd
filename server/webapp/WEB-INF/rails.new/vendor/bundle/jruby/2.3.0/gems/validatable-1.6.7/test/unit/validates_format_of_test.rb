require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  
  expect false do
    validation = Validatable::ValidatesFormatOf.new stub_everything, :name, :with => /book/
    validation.valid?(stub_everything)
  end
  
  expect true do
    validation = Validatable::ValidatesFormatOf.new stub_everything, :name, :with => /book/
    validation.valid?(stub(:name=>"book"))
  end
  
  expect true do
    validation = Validatable::ValidatesFormatOf.new stub_everything, :age, :with => /14/
    validation.valid?(stub(:age=>14))
  end
  
  expect ArgumentError do
    validation = Validatable::ValidatesFormatOf.new stub_everything, :age
  end
  
  expect true do
    options = [:message, :if, :times, :level, :groups, :with, :key]
    Validatable::ValidatesFormatOf.new(stub_everything, :test, options.to_blank_options_hash).must_understand(options.to_blank_options_hash)
  end
  
  expect true do
    options = [:with]
    Validatable::ValidatesFormatOf.new(stub_everything, :name, options.to_blank_options_hash).requires(options.to_blank_options_hash)
  end
  
end