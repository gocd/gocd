require 'bundler/vendored_persistent'
require 'cgi'
require 'securerandom'

module Bundler

  # Handles all the fetching with the rubygems server
  class Fetcher
    autoload :Downloader, 'bundler/fetcher/downloader'
    autoload :Dependency, 'bundler/fetcher/dependency'
    autoload :Index, 'bundler/fetcher/index'

    # This error is raised when it looks like the network is down
    class NetworkDownError < HTTPError; end
    # This error is raised if the API returns a 413 (only printed in verbose)
    class FallbackError < HTTPError; end
    # This is the error raised if OpenSSL fails the cert verification
    class CertificateFailureError < HTTPError
      def initialize(remote_uri)
        super "Could not verify the SSL certificate for #{remote_uri}.\nThere" \
          " is a chance you are experiencing a man-in-the-middle attack, but" \
          " most likely your system doesn't have the CA certificates needed" \
          " for verification. For information about OpenSSL certificates, see" \
          " bit.ly/ruby-ssl. To connect without using SSL, edit your Gemfile" \
          " sources and change 'https' to 'http'."
      end
    end
    # This is the error raised when a source is HTTPS and OpenSSL didn't load
    class SSLError < HTTPError
      def initialize(msg = nil)
        super msg || "Could not load OpenSSL.\n" \
            "You must recompile Ruby with OpenSSL support or change the sources in your " \
            "Gemfile from 'https' to 'http'. Instructions for compiling with OpenSSL " \
            "using RVM are available at rvm.io/packages/openssl."
      end
    end
    # This error is raised if HTTP authentication is required, but not provided.
    class AuthenticationRequiredError < HTTPError
      def initialize(remote_uri)
        super "Authentication is required for #{remote_uri}.\n" \
          "Please supply credentials for this source. You can do this by running:\n" \
          " bundle config #{remote_uri} username:password"
      end
    end
    # This error is raised if HTTP authentication is provided, but incorrect.
    class BadAuthenticationError < HTTPError
      def initialize(remote_uri)
        super "Bad username or password for #{remote_uri}.\n" \
          "Please double-check your credentials and correct them."
      end
    end

    # Exceptions classes that should bypass retry attempts. If your password didn't work the
    # first time, it's not going to the third time.
    AUTH_ERRORS = [AuthenticationRequiredError, BadAuthenticationError]

    class << self
      attr_accessor :disable_endpoint, :api_timeout, :redirect_limit, :max_retries
    end

    self.redirect_limit = Bundler.settings[:redirect]  # How many redirects to allow in one request
    self.api_timeout    = Bundler.settings[:timeout] # How long to wait for each API call
    self.max_retries    = Bundler.settings[:retry] # How many retries for the API call

    def initialize(remote)
      @remote = remote

      Socket.do_not_reverse_lookup = true
      connection # create persistent connection
    end

    def uri
      @remote.anonymized_uri
    end

    # fetch a gem specification
    def fetch_spec(spec)
      spec = spec - [nil, 'ruby', '']
      spec_file_name = "#{spec.join '-'}.gemspec"

      uri = URI.parse("#{remote_uri}#{Gem::MARSHAL_SPEC_DIR}#{spec_file_name}.rz")
      if uri.scheme == 'file'
        Bundler.load_marshal Gem.inflate(Gem.read_binary(uri.path))
      elsif cached_spec_path = gemspec_cached_path(spec_file_name)
        Bundler.load_gemspec(cached_spec_path)
      else
        Bundler.load_marshal Gem.inflate(downloader.fetch uri)
      end
    rescue MarshalError
      raise HTTPError, "Gemspec #{spec} contained invalid data.\n" \
        "Your network or your gem server is probably having issues right now."
    end

    # return the specs in the bundler format as an index
    def specs(gem_names, source)
      old = Bundler.rubygems.sources
      index = Bundler::Index.new

      specs = {}
      fetchers.dup.each do |f|
        unless f.api_fetcher? && !gem_names
          break if specs = f.specs(gem_names)
        end
        fetchers.delete(f)
      end
      @use_api = false if fetchers.none?(&:api_fetcher?)

      specs[remote_uri].each do |name, version, platform, dependencies|
        next if name == 'bundler'
        spec = nil
        if dependencies
          spec = EndpointSpecification.new(name, version, platform, dependencies)
        else
          spec = RemoteSpecification.new(name, version, platform, self)
        end
        spec.source = source
        spec.remote = @remote
        index << spec
      end

      index
    rescue CertificateFailureError
      Bundler.ui.info "" if gem_names && use_api # newline after dots
      raise
    ensure
      Bundler.rubygems.sources = old
    end

    def use_api
      return @use_api if defined?(@use_api)

      if remote_uri.scheme == "file" || Bundler::Fetcher.disable_endpoint
        @use_api = false
      else
        fetchers.reject! { |f| f.api_fetcher? && !f.api_available? }
        @use_api = fetchers.any?(&:api_fetcher?)
      end
    end

    def user_agent
      @user_agent ||= begin
        ruby = Bundler.ruby_version

        agent = "bundler/#{Bundler::VERSION}"
        agent << " rubygems/#{Gem::VERSION}"
        agent << " ruby/#{ruby.version}"
        agent << " (#{ruby.host})"
        agent << " command/#{ARGV.first}"

        if ruby.engine != "ruby"
          # engine_version raises on unknown engines
          engine_version = ruby.engine_version rescue "???"
          agent << " #{ruby.engine}/#{engine_version}"
        end

        agent << " options/#{Bundler.settings.all.join(",")}"

        agent << " ci/#{cis.join(",")}" if cis.any?

        # add a random ID so we can consolidate runs server-side
        agent << " " << SecureRandom.hex(8)

        # add any user agent strings set in the config
        extra_ua = Bundler.settings[:user_agent]
        agent << " " << extra_ua if extra_ua

        agent
      end
    end

    def fetchers
      @fetchers ||= FETCHERS.map { |f| f.new(downloader, remote_uri, fetch_uri, uri) }
    end

    def inspect
      "#<#{self.class}:0x#{object_id} uri=#{uri}>"
    end

  private

    FETCHERS = [Dependency, Index]

    def cis
      env_cis = {
        "TRAVIS" => "travis",
        "CIRCLECI" => "circle",
        "SEMAPHORE" => "semaphore",
        "JENKINS_URL" => "jenkins",
        "BUILDBOX" => "buildbox",
        "GO_SERVER_URL" => "go",
        "SNAP_CI" => "snap",
        "CI_NAME" => ENV["CI_NAME"],
        "CI" => "ci"
      }
      env_cis.find_all{ |env, ci| ENV[env]}.map{ |env, ci| ci }
    end

    def connection
      @connection ||= begin
        needs_ssl = remote_uri.scheme == "https" ||
          Bundler.settings[:ssl_verify_mode] ||
          Bundler.settings[:ssl_client_cert]
        raise SSLError if needs_ssl && !defined?(OpenSSL::SSL)

        con = Net::HTTP::Persistent.new 'bundler', :ENV

        if remote_uri.scheme == "https"
          con.verify_mode = (Bundler.settings[:ssl_verify_mode] ||
            OpenSSL::SSL::VERIFY_PEER)
          con.cert_store = bundler_cert_store
        end

        if Bundler.settings[:ssl_client_cert]
          pem = File.read(Bundler.settings[:ssl_client_cert])
          con.cert = OpenSSL::X509::Certificate.new(pem)
          con.key  = OpenSSL::PKey::RSA.new(pem)
        end

        con.read_timeout = Fetcher.api_timeout
        con.override_headers["User-Agent"] = user_agent
        con
      end
    end

    # cached gem specification path, if one exists
    def gemspec_cached_path spec_file_name
      paths = Bundler.rubygems.spec_cache_dirs.map { |dir| File.join(dir, spec_file_name) }
      paths = paths.select {|path| File.file? path }
      paths.first
    end

    HTTP_ERRORS = [
      Timeout::Error, EOFError, SocketError, Errno::ENETDOWN,
      Errno::EINVAL, Errno::ECONNRESET, Errno::ETIMEDOUT, Errno::EAGAIN,
      Net::HTTPBadResponse, Net::HTTPHeaderSyntaxError, Net::ProtocolError,
      Net::HTTP::Persistent::Error
    ]

    def bundler_cert_store
      store = OpenSSL::X509::Store.new
      if Bundler.settings[:ssl_ca_cert]
        if File.directory? Bundler.settings[:ssl_ca_cert]
          store.add_path Bundler.settings[:ssl_ca_cert]
        else
          store.add_file Bundler.settings[:ssl_ca_cert]
        end
      else
        store.set_default_paths
        certs = File.expand_path("../ssl_certs/*.pem", __FILE__)
        Dir.glob(certs).each { |c| store.add_file c }
      end
      store
    end

  private

    def fetch_uri
      @fetch_uri ||= begin
        if remote_uri.host == "rubygems.org"
          uri = remote_uri.dup
          uri.host = "bundler.rubygems.org"
          uri
        else
          remote_uri
        end
      end
    end

    def remote_uri
      @remote.uri
    end

    def downloader
      @downloader ||= Downloader.new(connection, self.class.redirect_limit)
    end

  end
end
