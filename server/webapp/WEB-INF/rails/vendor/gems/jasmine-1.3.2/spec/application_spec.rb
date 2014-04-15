require 'spec_helper'

#Rspec 1 doesn't correctly pass blocks to stubs, so skip (covered by integration tests)
#https://groups.google.com/forum/?fromgroups=#!topic/rspec/XT7paH2asCo

if Jasmine::Dependencies.rspec2?
  describe "Jasmine::Application" do
    it "should map paths provided by the config" do
      handler1 = double(:handler1)
      handler2 = double(:handler2)
      app1 = double(:app1)
      app2 = double(:app2)
      rack_path_map = {"/foo" => lambda { handler1 }, "/bar" => lambda { handler2 }}
      config = double(:config, :rack_path_map => rack_path_map, :rack_apps => [])
      builder = double("Rack::Builder.new")
      #Rack::Builder instance evals, so builder.run is invalid syntax,
      #this is the only way to stub out the 'run' dsl it gives to the block.
      Jasmine::Application.stub(:run).with(handler1).and_return(app1)
      Jasmine::Application.stub(:run).with(handler2).and_return(app2)

      builder.should_receive(:map).twice do |path, &app|
        if path == '/foo'
          app.call.should == app1
        elsif path == '/bar'
          app.call.should == app2
        else
          raise "Unexpected path passed"
        end
      end

      Jasmine::Application.app(config, builder).should == builder
    end
    it "should run rack apps provided by the config" do
      app1 = double(:app1)
      app2 = double(:app2)
      block = lambda { "foo" }
      config = double(:config, :rack_path_map => [], :rack_apps => [[app1, nil], [app2, block]])
      builder = double("Rack::Builder.new")
      builder.should_receive(:use).with(app1)
      builder.should_receive(:use).with(app2, &block)
      Jasmine::Application.app(config, builder).should == builder
    end
  end
end
