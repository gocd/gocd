require 'spec_helper'

describe JsRoutes, "options" do

  before(:each) do
    evaljs(_presetup) if _presetup
    with_warnings(_warnings) do
      evaljs(JsRoutes.generate(_options))
    end
  end

  let(:_presetup) { nil }
  let(:_options) { {} }
  let(:_warnings) { true }

  describe "serializer" do
    context "when specified" do
      # define custom serializer
      # this is a nonsense serializer, which always returns foo=bar
      # for all inputs
      let(:_presetup){ %q(function myCustomSerializer(object, prefix) { return "foo=bar"; }) }
      let(:_options) { {:serializer => "myCustomSerializer"} }

      it "should set configurable serializer" do
        # expect the nonsense serializer above to have appened foo=bar
        # to the end of the path
        expect(evaljs(%q(Routes.inboxes_path()))).to eql("/inboxes?foo=bar")
      end
    end

    context "when specified, but not function" do
      let(:_presetup){ %q(var myCustomSerializer = 1) }
      let(:_options) { {:serializer => "myCustomSerializer"} }

      it "should set configurable serializer" do
        # expect to use default
        expect(evaljs(%q(Routes.inboxes_path({a: 1})))).to eql("/inboxes?a=1")
      end
    end

    context "when configured in js" do
      let(:_options) { {:serializer =>%q(function (object, prefix) { return "foo=bar"; })} }

      it "uses JS serializer" do
        evaljs("Routes.configure({serializer: function (object, prefix) { return 'bar=baz'; }})")
        expect(evaljs(%q(Routes.inboxes_path({a: 1})))).to eql("/inboxes?bar=baz")
      end
    end
  end

  context "when exclude is specified" do

    let(:_options) { {:exclude => /^admin_/} }

    it "should exclude specified routes from file" do
      expect(evaljs("Routes.admin_users_path")).to be_nil
    end

    it "should not exclude routes not under specified pattern" do
      expect(evaljs("Routes.inboxes_path()")).not_to be_nil
    end

    context "for rails engine" do
      let(:_options) { {:exclude => /^blog_app_posts/} }

      it "should exclude specified engine route" do
        expect(evaljs("Routes.blog_app_posts_path")).to be_nil
      end
    end
  end

  context "when include is specified" do

    let(:_options) { {:include => /^admin_/} }

    it "should exclude specified routes from file" do
      expect(evaljs("Routes.admin_users_path()")).not_to be_nil
    end

    it "should not exclude routes not under specified pattern" do
      expect(evaljs("Routes.inboxes_path")).to be_nil
    end

    context "for rails engine" do
      let(:_options) { {:include => /^blog_app_posts/} }

      it "should include specified engine route" do
        expect(evaljs("Routes.blog_app_posts_path()")).not_to be_nil
      end
    end
  end

  context "when prefix with trailing slash is specified" do

    let(:_options) { {:prefix => "/myprefix/" } }

    it "should render routing with prefix" do
        expect(evaljs("Routes.inbox_path(1)")).to eq("/myprefix#{test_routes.inbox_path(1)}")
    end

    it "should render routing with prefix set in JavaScript" do
      evaljs("Routes.configure({prefix: '/newprefix/'})")
      expect(evaljs("Routes.config().prefix")).to eq("/newprefix/")
      expect(evaljs("Routes.inbox_path(1)")).to eq("/newprefix#{test_routes.inbox_path(1)}")
    end

  end

  context "when prefix with http:// is specified" do

    let(:_options) { {:prefix => "http://localhost:3000" } }

    it "should render routing with prefix" do
      expect(evaljs("Routes.inbox_path(1)")).to eq(_options[:prefix] + test_routes.inbox_path(1))
    end
  end

  context "when prefix without trailing slash is specified" do

    let(:_options) { {:prefix => "/myprefix" } }

    it "should render routing with prefix" do
      expect(evaljs("Routes.inbox_path(1)")).to eq("/myprefix#{test_routes.inbox_path(1)}")
    end

    it "should render routing with prefix set in JavaScript" do
      evaljs("Routes.configure({prefix: '/newprefix/'})")
      expect(evaljs("Routes.inbox_path(1)")).to eq("/newprefix#{test_routes.inbox_path(1)}")
    end

  end

  context "when default format is specified" do
    let(:_options) { {:default_url_options => {format: "json"}} }
    let(:_warnings) { nil }

    it "should render routing with default_format" do
      expect(evaljs("Routes.inbox_path(1)")).to eq(test_routes.inbox_path(1, :format => "json"))
    end

    it "should render routing with default_format and zero object" do
      expect(evaljs("Routes.inbox_path(0)")).to eq(test_routes.inbox_path(0, :format => "json"))
    end

    it "should override default_format when spefified implicitly" do
      expect(evaljs("Routes.inbox_path(1, {format: 'xml'})")).to eq(test_routes.inbox_path(1, :format => "xml"))
    end

    it "should override nullify implicitly when specified implicitly" do
      expect(evaljs("Routes.inbox_path(1, {format: null})")).to eq(test_routes.inbox_path(1))
    end


    it "shouldn't require the format" do
      pending if Rails.version < "4.0"
      expect(evaljs("Routes.json_only_path()")).to eq(test_routes.json_only_path(:format => 'json'))
    end
  end

  it "shouldn't include the format when {:format => false} is specified" do
    expect(evaljs("Routes.no_format_path()")).to eq(test_routes.no_format_path())
    expect(evaljs("Routes.no_format_path({format: 'json'})")).to eq(test_routes.no_format_path(format: 'json'))
  end

  describe "when namespace option is specified" do
    let(:_options) { {:namespace => "PHM"} }
    it "should use this namespace for routing" do
      expect(evaljs("window.Routes")).to be_nil
      expect(evaljs("PHM.inbox_path")).not_to be_nil
    end
  end

  describe "when nested namespace option is specified" do
    context "and defined on client" do
      let(:_presetup) { "window.PHM = {}" }
      let(:_options) { {:namespace => "PHM.Routes"} }
      it "should use this namespace for routing" do
        expect(evaljs("PHM.Routes.inbox_path")).not_to be_nil
      end
    end

    context "but undefined on client" do
      let(:_options) { {:namespace => "PHM.Routes"} }
      it "should initialize namespace" do
        expect(evaljs("window.PHM.Routes.inbox_path")).not_to be_nil
      end
    end

    context "and some parts are defined" do
      let(:_presetup) { "window.PHM = { Utils: {} };" }
      let(:_options) { {:namespace => "PHM.Routes"} }
      it "should not overwrite existing parts" do
        expect(evaljs("window.PHM.Utils")).not_to be_nil
        expect(evaljs("window.PHM.Routes.inbox_path")).not_to be_nil
      end
    end
  end

  describe "default_url_options" do
    context "with optional route parts" do
      context "provided" do
        let(:_options) { { :default_url_options => { :optional_id => "12", :format => "json" } } }
        it "should use this opions to fill optional parameters" do
          expect(evaljs("Routes.things_path()")).to eq(test_routes.things_path(:optional_id => 12, :format => "json"))
        end
      end

      context "not provided" do
        let(:_options) { { :default_url_options => { :format => "json" } } }
        it "breaks" do
          expect(evaljs("Routes.foo_all_path()")).to eq(test_routes.foo_all_path(:format => "json"))
        end
      end
    end

    context "with required route parts" do
      let(:_options) { {:default_url_options => {:inbox_id => "12"}} }
      it "should use this opions to fill optional parameters" do
        expect(evaljs("Routes.inbox_messages_path()")).to eq(test_routes.inbox_messages_path(:inbox_id => 12))
      end
    end

    context "when overwritten on JS level" do
        let(:_options) { { :default_url_options => { :format => "json" } } }
      it "uses JS defined value" do
        evaljs("Routes.configure({default_url_options: {format: 'xml'}})")
        expect(evaljs("Routes.inboxes_path()")).to eq(test_routes.inboxes_path(format: 'xml'))
      end
    end
  end

  describe "trailing_slash" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should working in params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true})")).to eq(test_routes.inbox_path(1, :trailing_slash => true))
      end

      it "should working with additional params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true, test: 'params'})")).to eq(test_routes.inbox_path(1, :trailing_slash => true, :test => 'params'))
      end
    end

    context "with default_url_options option" do
      let(:_options) { {:default_url_options => {:trailing_slash => true}} }
      it "should working" do
        expect(evaljs("Routes.inbox_path(1, {test: 'params'})")).to eq(test_routes.inbox_path(1, :trailing_slash => true, :test => 'params'))
      end

      it "should remove it by params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: false})")).to eq(test_routes.inbox_path(1))
      end
    end

    context "with disabled default_url_options option" do
      let(:_options) { {:default_url_options => {:trailing_slash => false}} }
      it "should not use trailing_slash" do
        expect(evaljs("Routes.inbox_path(1, {test: 'params'})")).to eq(test_routes.inbox_path(1, :test => 'params'))
      end

      it "should use it by params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true})")).to eq(test_routes.inbox_path(1, :trailing_slash => true))
      end
    end
  end

  describe "camel_case" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should use snake case routes" do
        expect(evaljs("Routes.inbox_path(1)")).to eq(test_routes.inbox_path(1))
        expect(evaljs("Routes.inboxPath")).to be_nil
      end
    end

    context "with true" do
      let(:_options) { { :camel_case => true } }
      it "should generate camel case routes" do
        expect(evaljs("Routes.inbox_path")).to be_nil
        expect(evaljs("Routes.inboxPath")).not_to be_nil
        expect(evaljs("Routes.inboxPath(1)")).to eq(test_routes.inbox_path(1))
        expect(evaljs("Routes.inboxMessagesPath(10)")).to eq(test_routes.inbox_messages_path(:inbox_id => 10))
      end
    end
  end

  describe "url_links" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should generate only path links" do
        expect(evaljs("Routes.inbox_path(1)")).to eq(test_routes.inbox_path(1))
        expect(evaljs("Routes.inbox_url")).to be_nil
      end
    end

    context "when configuring with default_url_options" do
      context "when only host option is specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com"} } }

        it "uses the specified host, defaults protocol to http, defaults port to 80 (leaving it blank)" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com#{test_routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com#{test_routes.new_session_path}")
        end

        it "does not override host when specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq(test_routes.sso_url)
        end

        it "does not override port when specified in route" do
          expect(evaljs("Routes.portals_url()")).to eq("http://example.com:8080#{test_routes.portals_path}")
        end
      end

      context "when default host and protocol are specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com", :protocol => "ftp"} } }

        it "uses the specified protocol and host, defaults port to 80 (leaving it blank)" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("ftp://example.com#{test_routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com#{test_routes.new_session_path}")
        end

        it "does not override host when host is specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq("ftp://sso.example.com#{test_routes.sso_path}")
        end

        it "does not override port when specified in route" do
          expect(evaljs("Routes.portals_url()")).to eq("ftp://example.com:8080#{test_routes.portals_path}")
        end
      end

      context "when default host and port are specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com", :port => 3000} } }

        it "uses the specified host and port, defaults protocol to http" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com:3000#{test_routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com:3000#{test_routes.new_session_path}")
        end

        it "does not override host, protocol, or port when host is specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq("http://sso.example.com:3000" + test_routes.sso_path)
        end

        it "does not override parts when specified in route" do
          expect(evaljs("Routes.secret_root_url()")).to eq(test_routes.secret_root_url)
        end
      end

      context "with camel_case option" do
        let(:_options) { { :camel_case => true, :url_links => true, :default_url_options => {:host => "example.com"} } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inboxUrl(1)")).to eq("http://example.com#{test_routes.inbox_path(1)}")
        end
      end

      context "with prefix option" do
        let(:_options) { { :prefix => "/api", :url_links => true, :default_url_options => {:host => 'example.com'} } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com/api#{test_routes.inbox_path(1)}")
        end
      end

      context "with compact option" do
        let(:_options) { { :compact => true, :url_links => true, :default_url_options => {:host => 'example.com'} } }
        it "does not affect url helpers" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com#{test_routes.inbox_path(1)}")
        end
      end
    end

    context 'when window.location is present' do
      let(:current_protocol) { 'http:' } # window.location.protocol includes the colon character
      let(:current_hostname) { 'current.example.com' }
      let(:current_port){ '' } # an empty string means port 80
      let(:current_host) do
        host = "#{current_hostname}"
        host += ":#{current_port}" unless current_port == ''
        host
      end

      before do
        jscontext.eval("window = {'location': {'protocol': '#{current_protocol}', 'hostname': '#{current_hostname}', 'port': '#{current_port}', 'host': '#{current_host}'}}")
      end

      context "without specifying a default host" do
        let(:_options) { { :url_links => true } }

        it "uses the current host" do
          expect(evaljs("Routes.inbox_path")).not_to be_nil
          expect(evaljs("Routes.inbox_url")).not_to be_nil
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://current.example.com#{test_routes.inbox_path(1)}")
          expect(evaljs("Routes.inbox_url(1, { test_key: \"test_val\" })")).to eq("http://current.example.com#{test_routes.inbox_path(1, :test_key => "test_val")}")
          expect(evaljs("Routes.new_session_url()")).to eq("https://current.example.com#{test_routes.new_session_path}")

        end

        it "doesn't use current when specified in the route" do
          expect(evaljs("Routes.sso_url()")).to eq(test_routes.sso_url)
        end

        it "uses host option as an argument" do
          expect(evaljs("Routes.secret_root_url({host: 'another.com'})")).to eq(test_routes.secret_root_url(host: 'another.com'))
        end

        it "uses port option as an argument" do
          expect(evaljs("Routes.secret_root_url({host: 'localhost', port: 8080})")).to eq(test_routes.secret_root_url(host: 'localhost', port: 8080))
        end

        it "uses protocol option as an argument" do
          expect(evaljs("Routes.secret_root_url({host: 'localhost', protocol: 'https'})")).to eq(test_routes.secret_root_url(protocol: 'https', host: 'localhost'))
        end

        it "uses subdomain option as an argument" do
          expect(evaljs("Routes.secret_root_url({subdomain: 'custom'})")).to eq(test_routes.secret_root_url(subdomain: 'custom'))
        end
      end
    end

    context 'when window.location is not present' do
      context 'without specifying a default host' do
        let(:_options) { { url_links: true } }

        it 'generates path' do
          expect(evaljs("Routes.inbox_url(1)")).to eq test_routes.inbox_path(1)
          expect(evaljs("Routes.new_session_url()")).to eq test_routes.new_session_path
        end
      end
    end
  end

  describe "when the compact mode is enabled" do
    let(:_options) { { :compact => true } }
    it "removes _path suffix from path helpers" do
      expect(evaljs("Routes.inbox_path")).to be_nil
      expect(evaljs("Routes.inboxes()")).to eq(test_routes.inboxes_path())
      expect(evaljs("Routes.inbox(2)")).to eq(test_routes.inbox_path(2))
    end

    context "with url_links option" do
      around(:each) do |example|
        ActiveSupport::Deprecation.silence do
          example.run
        end
      end

      let(:_options) { { :compact => true, :url_links => true, default_url_options: {host: 'localhost'} } }
      it "should not strip urls" do
        expect(evaljs("Routes.inbox(1)")).to eq(test_routes.inbox_path(1))
        expect(evaljs("Routes.inbox_url(1)")).to eq("http://localhost#{test_routes.inbox_path(1)}")
      end
    end
  end

  describe "special_options_key" do
    let(:_options) { { special_options_key: :__options__ } }
    it "can be redefined" do
      expect {
        expect(evaljs("Routes.inbox_message_path({inbox_id: 1, id: 2, _options: true})")).to eq("")
      }.to raise_error(js_error_class)
      expect(evaljs("Routes.inbox_message_path({inbox_id: 1, id: 2, __options__: true})")).to eq(test_routes.inbox_message_path(inbox_id: 1, id: 2))
    end
  end

  describe "when application is specified" do
    let(:_options) { {:application => BlogEngine::Engine} }

    it "should include specified engine route" do
      expect(evaljs("Routes.posts_path()")).not_to be_nil
    end
  end
end
