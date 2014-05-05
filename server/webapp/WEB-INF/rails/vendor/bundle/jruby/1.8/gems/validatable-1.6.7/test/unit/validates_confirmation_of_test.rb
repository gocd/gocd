require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  
  expect true do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username
    instance = stub(:username=>"username", :username_confirmation=>"username")
    validation.valid?(instance)
  end
  
  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username
    instance = stub(:username=>"username", :username_confirmation=>"usessrname")
    validation.valid?(instance)
  end
  
  expect true do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => false
    instance = stub(:username=>"username", :username_confirmation=>"USERNAME")
    validation.valid?(instance)
  end
  
  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => true
    instance = stub(:username=>"username", :username_confirmation=>"USERNAME")
    validation.valid?(instance)
  end
  
  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => true
    validation.valid?(stub(:username => nil, :username_confirmation => 'something'))
  end

  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => true
    validation.valid?(stub(:username => 'something', :username_confirmation => nil))
  end

  expect true do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => true
    validation.valid?(stub(:username => nil, :username_confirmation => nil))
  end
  
  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => false
    validation.valid?(stub(:username => nil, :username_confirmation => 'something'))
  end

  expect false do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => false
    validation.valid?(stub(:username => 'something', :username_confirmation => nil))
  end

  expect true do
    validation = Validatable::ValidatesConfirmationOf.new stub_everything, :username, :case_sensitive => false
    validation.valid?(stub(:username => nil, :username_confirmation => nil))
  end
  
  expect true do
    options = { :message => nil, :if => nil, :times => nil, :level => nil, :groups => nil, :case_sensitive => nil }
    Validatable::ValidatesConfirmationOf.new(stub_everything, :test).must_understand(options)
  end
  
end