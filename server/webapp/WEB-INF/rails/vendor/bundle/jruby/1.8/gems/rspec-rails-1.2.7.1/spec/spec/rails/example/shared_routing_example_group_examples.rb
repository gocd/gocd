class CustomRouteSpecController < ActionController::Base; end
class RspecOnRailsSpecsController < ActionController::Base; end

share_as :RoutingExampleGroupSpec do
  it "support custom routes" do
    route_for(:controller => "custom_route_spec", :action => "custom_route").
      should == "/custom_route"
  end

  it "support existing routes" do
    route_for(:controller => "controller_spec", :action => "some_action").
      should == "/controller_spec/some_action"
  end

  it "support existing routes with additional parameters" do
    route_for(:controller => "controller_spec", :action => "some_action", :param => '1').
      should == "/controller_spec/some_action?param=1"
  end
  
  it "recognize routes with methods besides :get" do
    route_for(:controller => "rspec_on_rails_specs", :action => "update", :id => "37").
      should == {:path => "/rspec_on_rails_specs/37", :method => :put}
  end

  it "generate params for custom routes" do
    params_from(:get, '/custom_route').
      should == {:controller => "custom_route_spec", :action => "custom_route"}
  end

  it "generate params for existing routes" do
    params_from(:get, '/controller_spec/some_action').
      should == {:controller => "controller_spec", :action => "some_action"}
  end

  it "generate params for an existing route with a query parameter" do
    params_from(:get, '/controller_spec/some_action?param=1').
      should == {:controller => "controller_spec", :action => "some_action", :param => '1'}
  end

  it "generate params for an existing route with multiple query parameters" do
    params_from(:get, '/controller_spec/some_action?param1=1&param2=2').
      should == {:controller => "controller_spec", :action => "some_action", :param1 => '1', :param2 => '2' }
  end
end