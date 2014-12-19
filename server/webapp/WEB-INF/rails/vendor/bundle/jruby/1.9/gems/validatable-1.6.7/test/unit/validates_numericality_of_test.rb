require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
 
  expect false do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :nothing
    instance = stub(:nothing => nil)
    validation.valid?(instance)
  end

  expect true do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :some_int
    instance = stub(:some_int => 50)
    validation.valid?(instance)
  end  
 
  expect true do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :some_decimal
    instance = stub(:some_decimal => 1.23)
    validation.valid?(instance)
  end

  expect false do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :some_decimal, :only_integer => true
    instance = stub(:some_decimal => 1.23)
    validation.valid?(instance)
  end
  
  expect true do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :some_negative_number, :only_integer => true
    instance = stub(:some_negative_number => "-1")
    validation.valid?(instance)
  end
  
  expect false do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :some_non_numeric
    instance = stub(:some_non_numeric => "50F")
    validation.valid?(instance)
  end

  expect false do
    validation = Validatable::ValidatesNumericalityOf.new stub_everything, :multiple_dots
    instance = stub(:multiple_dots => "50.0.0")
    validation.valid?(instance)
  end
  
  expect true do
    options = [:message, :if, :times, :level, :groups, :only_integer]
    Validatable::ValidatesNumericalityOf.new(stub_everything, :test).must_understand(options.to_blank_options_hash)
  end
  
end