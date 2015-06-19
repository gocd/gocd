require 'spec_helper'

describe 'Jasmine::Application' do
  it 'should map paths provided by the config' do
    handler1 = double(:handler1)
    handler2 = double(:handler2)
    app1 = double(:app1)
    app2 = double(:app2)
    rack_path_map = {'/foo' => lambda { handler1 }, '/bar' => lambda { handler2 }}
    config = double(:config, :rack_path_map => rack_path_map, :rack_apps => [])
    builder = double('Rack::Builder.new')
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
        raise 'Unexpected path passed'
      end
    end

    Jasmine::Application.app(config, builder).should == builder
  end

  it 'should run rack apps provided by the config' do
    app1 = double(:app1)
    app2 = double(:app2)
    app3 = double(:app3)
    app4 = double(:app4)
    block = lambda { 'foo' }
    config = double(:config, :rack_path_map => [], :rack_apps => [
        { :app => app1 },
        { :app => app2, :block => block },
        { :app => app3, :args => [:foo, :bar], :block => block },
        { :app => app4, :args => [:bar] }
    ])
    builder = double('Rack::Builder.new')
    builder.should_receive(:use).with(app1)
    builder.should_receive(:use) do |*args, &arg_block|
      args.should == [app2]
      arg_block.should == block
    end
    builder.should_receive(:use) do |*args, &arg_block|
      args.should == [app3, :foo, :bar]
      arg_block.should == block
    end
    builder.should_receive(:use).with(app4, :bar)
    Jasmine::Application.app(config, builder).should == builder
  end
end