require File.dirname(__FILE__) + '/../../../spec_helper'
require 'controller_spec_controller'
require File.join(File.dirname(__FILE__), "/shared_routing_example_group_examples.rb")

describe "Routing Examples", :type => :routing do
  
  include RoutingExampleGroupSpec

end
