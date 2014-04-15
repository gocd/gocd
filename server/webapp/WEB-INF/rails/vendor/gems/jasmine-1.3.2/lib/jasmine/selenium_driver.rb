module Jasmine
  require 'json'
  class SeleniumDriver
    def initialize(browser, http_address)
      require 'selenium-webdriver'
      selenium_server = if ENV['SELENIUM_SERVER']
        ENV['SELENIUM_SERVER']
      elsif ENV['SELENIUM_SERVER_PORT']
        "http://localhost:#{ENV['SELENIUM_SERVER_PORT']}/wd/hub"
      end
      options = if browser == "firefox" && ENV["JASMINE_FIREBUG"]
                  require File.join(File.dirname(__FILE__), "firebug/firebug")
                  profile = Selenium::WebDriver::Firefox::Profile.new
                  profile.enable_firebug
                  {:profile => profile}
                end || {}
      @driver = if selenium_server
        Selenium::WebDriver.for :remote, :url => selenium_server, :desired_capabilities => browser.to_sym
      else
        Selenium::WebDriver.for browser.to_sym, options
      end
      @http_address = http_address
    end

    def connect
      @driver.navigate.to @http_address
    end

    def disconnect
      @driver.quit
    end

    def eval_js(script)
      result = @driver.execute_script(script)
      JSON.parse("{\"result\":#{result}}", :max_nesting => false)["result"]
    end

    def json_generate(obj)
      JSON.generate(obj)
    end
  end
end
