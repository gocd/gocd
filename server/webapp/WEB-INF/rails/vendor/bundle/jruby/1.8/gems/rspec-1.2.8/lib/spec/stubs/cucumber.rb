# This plugs RSpec's mocking/stubbing framework into cucumber
require 'spec/mocks'
Before {$rspec_stubs ||= Spec::Mocks::Space.new}
After  {$rspec_stubs.reset_all}
World(Spec::Mocks::ExampleMethods)
