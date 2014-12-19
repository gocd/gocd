require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  expect true do
    validation = Validatable::ValidationBase.new stub_everything, :base
    validation.should_validate? Object.new
  end
  
  expect true do
    validation = Validatable::ValidationBase.new stub_everything, :base, :times => 1
    validation.validate_this_time?(stub(:times_validated => 0))
  end
  
  expect true do
    validation = Validatable::ValidationBase.new stub_everything, :base
    validation.validate_this_time?(nil)
  end
  
  expect true do
    validation = Validatable::ValidationBase.new stub_everything, :base, :times => 2
    validation.validate_this_time?(stub(:times_validated => 1))
  end

  expect false do
    validation = Validatable::ValidationBase.new stub_everything, :base, :times => 1
    validation.validate_this_time?(stub(:times_validated => 1))
  end
  
  expect 1 do
    validation = Validatable::ValidationBase.new stub_everything, :base
    validation.level
  end
  
  expect ArgumentError do
    Validatable::ValidationBase.new stub_everything(:validation_keys_include? => true), :base, :times => 1
  end
  
  expect "some message 100" do
    validation = Validatable::ValidationBase.new stub_everything, :base, :message => lambda { "some message #{a_method}" }
    validation.message(stub(:a_method=>'100'))
  end
  
  expect ArgumentError do
    Validatable::ValidationBase.new(stub_everything, :base).must_understand(:foo => 1, :bar => 2)
  end
  
  expect true do
    options = {:message => nil, :if => nil, :times => nil, :level => nil, :groups => nil, :key => nil}
    Validatable::ValidationBase.new(stub_everything, :base).must_understand(options)
  end

end