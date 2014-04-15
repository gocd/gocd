require 'spec_helper'

describe Jasmine::RspecFormatter do
  describe "environment variables" do
    def stub_env_hash(hash)
      ENV.stub!(:[]) do |arg|
        hash[arg]
      end
    end
    describe "browser configuration" do
      it "should use firefox by default" do
        pending
        stub_env_hash({"JASMINE_BROWSER" => nil})
        config = double('config')
        formatter = Jasmine::RspecFormatter.new(config)
        Jasmine::SeleniumDriver.should_receive(:new).
          with("firefox", anything).
          and_return(mock(Jasmine::SeleniumDriver, :connect => true))
        formatter.start
      end

      it "should use ENV['JASMINE_BROWSER'] if set" do
        pending
        stub_env_hash({"JASMINE_BROWSER" => "mosaic"})

        Jasmine::SeleniumDriver.should_receive(:new).
          with("mosaic", anything).
          and_return(mock(Jasmine::SeleniumDriver, :connect => true))
        formatter.start
      end
    end

    describe "jasmine host" do
      it "should use http://localhost by default" do
        pending
        stub_env_hash({})
        config = Jasmine::Config.new
        config.instance_variable_set(:@jasmine_server_port, '1234')
        config.stub!(:start_jasmine_server)

        Jasmine::SeleniumDriver.should_receive(:new).
          with(anything, "http://localhost:1234/").
          and_return(mock(Jasmine::SeleniumDriver, :connect => true))
        config.start
      end

      it "should use ENV['JASMINE_HOST'] if set" do
        pending
        stub_env_hash({"JASMINE_HOST" => "http://some_host"})
        config = Jasmine::Config.new
        config.instance_variable_set(:@jasmine_server_port, '1234')
        config.stub!(:start_jasmine_server)

        Jasmine::SeleniumDriver.should_receive(:new).
          with(anything, "http://some_host:1234/").
          and_return(mock(Jasmine::SeleniumDriver, :connect => true))
        config.start
      end

      it "should use ENV['JASMINE_PORT'] if set" do
        pending
        stub_env_hash({"JASMINE_PORT" => "4321"})
        config = Jasmine::Config.new
        Jasmine.stub!(:wait_for_listener)
        config.stub!(:start_server)
        Jasmine::SeleniumDriver.should_receive(:new).
          with(anything, "http://localhost:4321/").
          and_return(mock(Jasmine::SeleniumDriver, :connect => true))
        config.start
      end
    end

    describe "external selenium server" do
      it "should use an external selenium server if SELENIUM_SERVER is set" do
        pending
        stub_env_hash({"SELENIUM_SERVER" => "http://myseleniumserver.com:4441"})
        Selenium::WebDriver.should_receive(:for).with(:remote, :url => "http://myseleniumserver.com:4441", :desired_capabilities => :firefox)
        Jasmine::SeleniumDriver.new('firefox', 'http://localhost:8888')
      end
      it "should use an local selenium server with a specific port if SELENIUM_SERVER_PORT is set" do
        pending
        stub_env_hash({"SELENIUM_SERVER_PORT" => "4441"})
        Selenium::WebDriver.should_receive(:for).with(:remote, :url => "http://localhost:4441/wd/hub", :desired_capabilities => :firefox)
        Jasmine::SeleniumDriver.new('firefox', 'http://localhost:8888')
      end
    end
  end
end
