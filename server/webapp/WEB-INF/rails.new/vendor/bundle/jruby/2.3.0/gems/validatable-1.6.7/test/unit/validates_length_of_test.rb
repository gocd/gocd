require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :maximum => 8
    validation.valid?(stub(:username=>"usernamefdfd"))
  end
  
  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :minimum => 2
    instance = stub(:username=>"u")
    validation.valid?(instance)
  end

  expect true do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :minimum => 2, :maximum => 8
    instance = stub(:username=>"udfgdf")
    validation.valid?(instance)
  end
  
  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :is => 2
    instance = stub(:username=>"u")
    validation.valid?(instance)
  end
  
  expect true do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :is => 2
    instance = stub(:username=>"uu")
    validation.valid?(instance)
  end
  
  expect true do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4
    instance = stub(:username => "aa")
    validation.valid?(instance)
  end

  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4
    instance = stub(:username => "a")
    validation.valid?(instance)
  end

  expect true do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4
    instance = stub(:username => "aaaa")
    validation.valid?(instance)
  end

  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4
    instance = stub(:username => "aaaaa")
    validation.valid?(instance)
  end

  expect false do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4
    instance = stub(:username => nil)
    validation.valid?(instance)
  end


  expect true do
    validation = Validatable::ValidatesLengthOf.new stub_everything, :username, :within => 2..4, :allow_nil => true
    instance = stub(:username => nil)
    validation.valid?(instance)
  end
  
  expect true do
    options = [:message, :if, :times, :level, :groups, :maximum, :minimum, :is, :within, :allow_nil]
    Validatable::ValidatesLengthOf.new(stub_everything, :test).must_understand(options.to_blank_options_hash)
  end
  
end