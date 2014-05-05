require "spec_helper"

describe "gemcutter's dependency API", :realworld => true do
  def wait_for_server(port, seconds = 15)
    tries = 0
    TCPSocket.new("127.0.0.1", port)
  rescue => e
    raise(e) if tries > (seconds * 2)
    tries += 1
    sleep 0.5
    retry
  end

  context "when Gemcutter API takes too long to respond" do
    before do
      # need to hack, so we can require rack
      old_gem_home = ENV['GEM_HOME']
      ENV['GEM_HOME'] = Spec::Path.base_system_gems.to_s
      require 'rack'
      ENV['GEM_HOME'] = old_gem_home

      port = 21453
      port += 1 while TCPSocket.new("127.0.0.1", port) rescue false
      @server_uri = "http://127.0.0.1:#{port}"

      require File.expand_path('../../support/artifice/endpoint_timeout', __FILE__)
      require 'thread'
      @t = Thread.new {
        server = Rack::Server.start(:app       => EndpointTimeout,
                                    :Host      => '0.0.0.0',
                                    :Port      => port,
                                    :server    => 'webrick',
                                    :AccessLog => [])
        server.start
      }
      @t.run

      wait_for_server(port)
    end

    after do
      @t.kill
    end

    it "times out and falls back on the modern index" do
      gemfile <<-G
        source "#{@server_uri}"
        gem "rack"

        old_v, $VERBOSE = $VERBOSE, nil
        Bundler::Fetcher.api_timeout = 1
        $VERBOSE = old_v
      G

      bundle :install
      expect(out).to include("Fetching source index from #{@server_uri}/")
      should_be_installed "rack 1.0.0"
    end
  end
end
