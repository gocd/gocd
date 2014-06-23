require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  
  expect false do
    validation = Validatable::ValidatesPresenceOf.new stub_everything, :name
    validation.valid?(stub_everything)
  end
  
  expect true do
    validation = Validatable::ValidatesPresenceOf.new stub_everything, :name
    validation.valid?(stub(:name=>"book"))
  end
  
  expect true do
    validation = Validatable::ValidatesPresenceOf.new stub_everything, :employee
    validation.valid?(stub(:employee => stub(:nil? => false)))
  end
  
  expect true do
    options = {:message => nil, :if => nil, :times => nil, :level => nil, :groups => nil}
    Validatable::ValidatesPresenceOf.new(stub_everything, :test).must_understand(options)
  end
  
end