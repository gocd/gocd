require 'spec_helper'

describe Jasmine::Server do
  describe "rack ~> 1.0" do
    before do
      Jasmine::Dependencies.stub(:legacy_rack?).and_return(true)
    end

    it "should run the handler with the application" do
      server = double(:server)
      port = 1234
      application = double(:application)
      Rack::Handler.should_receive(:get).with("webrick").and_return(server)
      server.should_receive(:run).with(application, hash_including(:Port => port))
      Jasmine::Server.new(port, application).start
    end
  end

  describe "rack >= 1.1" do
    before do
      Jasmine::Dependencies.stub(:legacy_rack?).and_return(false)
      if !Rack.constants.include?(:Server)
        Rack::Server = double("Rack::Server")
      end
    end

    it "should create a Rack::Server with the correct port when passed" do
      port = 1234
      Rack::Server.should_receive(:new).with(hash_including(:Port => port)).and_return(double(:server).as_null_object)
      Jasmine::Server.new(port, double(:app)).start
    end

    it "should start the server" do
      server = double(:server)
      Rack::Server.should_receive(:new) { server.as_null_object }
      server.should_receive(:start)
      Jasmine::Server.new('8888', double(:app)).start
    end

    it "should set the app as the instance variable on the rack server" do
      app = double('application')
      server = double(:server)
      Rack::Server.should_receive(:new) { server.as_null_object }
      Jasmine::Server.new(1234, app).start
      server.instance_variable_get(:@app).should == app
    end
  end
end
