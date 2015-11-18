require 'spec_helper'

describe JsRoutes, "options" do

  before(:each) do
    evaljs(_presetup)
    with_warnings(_warnings) do
      evaljs(JsRoutes.generate(_options))
    end
  end

  let(:_presetup) { "this;" }
  let(:_options) { {} }
  let(:_warnings) { true }

  context "when serializer is specified" do
    let(:_options) { {:serializer => "myCustomSerializer"} }

    it "should set configurable serializer" do
      # define custom serializer
      # this is a nonsense serializer, which always returns foo=bar
      # for all inputs
      evaljs(%q(function myCustomSerializer(object, prefix) { return "foo=bar"; }))

      # expect the nonsense serializer above to have appened foo=bar
      # to the end of the path
      expect(evaljs(%q(Routes.inboxes_path()))).to eql("/inboxes?foo=bar")
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
        expect(evaljs("Routes.inbox_path(1)")).to eq("/myprefix#{routes.inbox_path(1)}")
    end

    it "should render routing with prefix set in JavaScript" do
      evaljs("Routes.options.prefix = '/newprefix/'")
      expect(evaljs("Routes.inbox_path(1)")).to eq("/newprefix#{routes.inbox_path(1)}")
    end

  end

  context "when prefix with http:// is specified" do

    let(:_options) { {:prefix => "http://localhost:3000" } }

    it "should render routing with prefix" do
      expect(evaljs("Routes.inbox_path(1)")).to eq(_options[:prefix] + routes.inbox_path(1))
    end
  end

  context "when prefix without trailing slash is specified" do

    let(:_options) { {:prefix => "/myprefix" } }

    it "should render routing with prefix" do
      expect(evaljs("Routes.inbox_path(1)")).to eq("/myprefix#{routes.inbox_path(1)}")
    end

    it "should render routing with prefix set in JavaScript" do
      evaljs("Routes.options.prefix = '/newprefix'")
      expect(evaljs("Routes.inbox_path(1)")).to eq("/newprefix#{routes.inbox_path(1)}")
    end

  end

  context "when default_format is specified" do
    let(:_options) { {:default_format => "json"} }
    let(:_warnings) { nil }

    it "should render routing with default_format" do
      expect(evaljs("Routes.inbox_path(1)")).to eq(routes.inbox_path(1, :format => "json"))
    end

    it "should render routing with default_format and zero object" do
      expect(evaljs("Routes.inbox_path(0)")).to eq(routes.inbox_path(0, :format => "json"))
    end

    it "should override default_format when spefified implicitly" do
      expect(evaljs("Routes.inbox_path(1, {format: 'xml'})")).to eq(routes.inbox_path(1, :format => "xml"))
    end

    it "should override nullify implicitly when specified implicitly" do
      expect(evaljs("Routes.inbox_path(1, {format: null})")).to eq(routes.inbox_path(1))
    end

    it "shouldn't include the format when {:format => false} is specified" do
      expect(evaljs("Routes.no_format_path()")).to eq(routes.no_format_path)
    end

    it "shouldn't require the format" do
      expect(evaljs("Routes.json_only_path()")).to eq(routes.json_only_path(:format => 'json'))
    end
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
      let(:_options) { {:default_url_options => {:optional_id => "12", :format => "json"}}}
      it "should use this opions to fill optional parameters" do
        expect(evaljs("Routes.things_path()")).to eq(routes.things_path(:optional_id => 12, :format => "json"))
      end
    end

    context "with required route parts" do
      let(:_options) { {:default_url_options => {:inbox_id => "12"}} }
      it "should use this opions to fill optional parameters" do
        pending
        expect(evaljs("Routes.inbox_messages_path()")).to eq(routes.inbox_messages_path(:inbox_id => 12))
      end
    end
  end

  describe "trailing_slash" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should working in params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true})")).to eq(routes.inbox_path(1, :trailing_slash => true))
      end

      it "should working with additional params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true, test: 'params'})")).to eq(routes.inbox_path(1, :trailing_slash => true, :test => 'params'))
      end
    end

    context "with default_url_options option" do
      let(:_options) { {:default_url_options => {:trailing_slash => true}} }
      it "should working" do
        expect(evaljs("Routes.inbox_path(1, {test: 'params'})")).to eq(routes.inbox_path(1, :trailing_slash => true, :test => 'params'))
      end

      it "should remove it by params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: false})")).to eq(routes.inbox_path(1))
      end
    end

    context "with disabled default_url_options option" do
      let(:_options) { {:default_url_options => {:trailing_slash => false}} }
      it "should not use trailing_slash" do
        expect(evaljs("Routes.inbox_path(1, {test: 'params'})")).to eq(routes.inbox_path(1, :test => 'params'))
      end

      it "should use it by params" do
        expect(evaljs("Routes.inbox_path(1, {trailing_slash: true})")).to eq(routes.inbox_path(1, :trailing_slash => true))
      end
    end
  end

  describe "camel_case" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should use snake case routes" do
        expect(evaljs("Routes.inbox_path(1)")).to eq(routes.inbox_path(1))
        expect(evaljs("Routes.inboxPath")).to be_nil
      end
    end

    context "with true" do
      let(:_options) { { :camel_case => true } }
      it "should generate camel case routes" do
        expect(evaljs("Routes.inbox_path")).to be_nil
        expect(evaljs("Routes.inboxPath")).not_to be_nil
        expect(evaljs("Routes.inboxPath(1)")).to eq(routes.inbox_path(1))
        expect(evaljs("Routes.inboxMessagesPath(10)")).to eq(routes.inbox_messages_path(:inbox_id => 10))
      end
    end
  end

  describe "url_links" do
    context "with default option" do
      let(:_options) { Hash.new }
      it "should generate only path links" do
        expect(evaljs("Routes.inbox_path(1)")).to eq(routes.inbox_path(1))
        expect(evaljs("Routes.inbox_url")).to be_nil
      end
    end

    context 'with deprecated, non-boolean config value' do
      around(:each) do |example|
        ActiveSupport::Deprecation.silence do
          example.run
        end
      end

      context "with host" do
        let(:_options) { { :url_links => "http://localhost" } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inbox_path")).not_to be_nil
          expect(evaljs("Routes.inbox_url")).not_to be_nil
          expect(evaljs("Routes.inbox_path(1)")).to eq(routes.inbox_path(1))
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://localhost#{routes.inbox_path(1)}")
          expect(evaljs("Routes.inbox_url(1, { test_key: \"test_val\" })")).to eq("http://localhost#{routes.inbox_path(1, :test_key => "test_val")}")
        end
      end

      context "with invalid host" do
        it "should raise error" do
          expect { JsRoutes.generate({ :url_links => "localhost" }) }.to raise_error RuntimeError
        end
      end

      context "with host and camel_case" do
        let(:_options) { { :camel_case => true, :url_links => "http://localhost" } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inboxPath")).not_to be_nil
          expect(evaljs("Routes.inboxUrl")).not_to be_nil
          expect(evaljs("Routes.inboxPath(1)")).to eq(routes.inbox_path(1))
          expect(evaljs("Routes.inboxUrl(1)")).to eq("http://localhost#{routes.inbox_path(1)}")
        end
      end

      context "with host and prefix" do
        let(:_options) { { :prefix => "/api", :url_links => "https://example.com" } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inbox_path")).not_to be_nil
          expect(evaljs("Routes.inbox_url")).not_to be_nil
          expect(evaljs("Routes.inbox_path(1)")).to eq("/api#{routes.inbox_path(1)}")
          expect(evaljs("Routes.inbox_url(1)")).to eq("https://example.com/api#{routes.inbox_path(1)}")
        end
      end
    end

    context "when configuring with default_url_options" do
      context "when default host is not specified" do
        it "raises an error" do
          expect { JsRoutes.generate({ :url_links => true }) }.to raise_error RuntimeError
        end
      end

      context "when only host option is specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com"} } }

        it "uses the specified host, defaults protocol to http, defaults port to 80 (leaving it blank)" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com#{routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com#{routes.new_session_path}")
        end

        it "does not override host when specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq(routes.sso_url)
        end

        it "does not override port when specified in route" do
          expect(evaljs("Routes.portals_url()")).to eq("http://example.com:8080#{routes.portals_path}")
        end
      end

      context "when default host and protocol are specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com", :protocol => "ftp"} } }

        it "uses the specified protocol and host, defaults port to 80 (leaving it blank)" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("ftp://example.com#{routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com#{routes.new_session_path}")
        end

        it "does not override host when host is specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq("ftp://sso.example.com#{routes.sso_path}")
        end

        it "does not override port when specified in route" do
          expect(evaljs("Routes.portals_url()")).to eq("ftp://example.com:8080#{routes.portals_path}")
        end
      end

      context "when default host and port are specified" do
        let(:_options) { { :url_links => true, :default_url_options => {:host => "example.com", :port => 3000} } }

        it "uses the specified host and port, defaults protocol to http" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com:3000#{routes.inbox_path(1)}")
        end

        it "does not override protocol when specified in route" do
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com:3000#{routes.new_session_path}")
        end

        it "does not override host, protocol, or port when host is specified in route" do
          expect(evaljs("Routes.sso_url()")).to eq(routes.sso_url)
        end

        it "does not override port when specified in route" do
          expect(evaljs("Routes.portals_url()")).to eq("http://example.com:8080#{routes.portals_path}")
        end
      end

      context "with camel_case option" do
        let(:_options) { { :camel_case => true, :url_links => true, :default_url_options => {:host => "example.com"} } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inboxUrl(1)")).to eq("http://example.com#{routes.inbox_path(1)}")
          expect(evaljs("Routes.newSessionUrl()")).to eq("https://example.com#{routes.new_session_path}")
          expect(evaljs("Routes.ssoUrl()")).to eq(routes.sso_url)
          expect(evaljs("Routes.portalsUrl()")).to eq("http://example.com:8080#{routes.portals_path}")
        end
      end

      context "with prefix option" do
        let(:_options) { { :prefix => "/api", :url_links => true, :default_url_options => {:host => 'example.com'} } }
        it "should generate path and url links" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com/api#{routes.inbox_path(1)}")
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com/api#{routes.new_session_path}")
          expect(evaljs("Routes.sso_url()")).to eq("http://sso.example.com/api#{routes.sso_path}")
          expect(evaljs("Routes.portals_url()")).to eq("http://example.com:8080/api#{routes.portals_path}")
        end
      end

      context "with compact option" do
        let(:_options) { { :compact => true, :url_links => true, :default_url_options => {:host => 'example.com'} } }
        it "does not affect url helpers" do
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://example.com#{routes.inbox_path(1)}")
          expect(evaljs("Routes.new_session_url()")).to eq("https://example.com#{routes.new_session_path}")
          expect(evaljs("Routes.sso_url()")).to eq(routes.sso_url)
          expect(evaljs("Routes.portals_url()")).to eq("http://example.com:8080#{routes.portals_path}")
        end
      end
    end
  end

  describe "when the compact mode is enabled" do
    let(:_options) { { :compact => true } }
    it "removes _path suffix from path helpers" do
      expect(evaljs("Routes.inbox_path")).to be_nil
      expect(evaljs("Routes.inboxes()")).to eq(routes.inboxes_path())
      expect(evaljs("Routes.inbox(2)")).to eq(routes.inbox_path(2))
    end

    context "with url_links option" do
      context "with deprecated url_links config value" do
        around(:each) do |example|
          ActiveSupport::Deprecation.silence do
            example.run
          end
        end

        let(:_options) { { :compact => true, :url_links => "http://localhost" } }
        it "should not strip urls" do
          expect(evaljs("Routes.inbox(1)")).to eq(routes.inbox_path(1))
          expect(evaljs("Routes.inbox_url(1)")).to eq("http://localhost#{routes.inbox_path(1)}")
        end
      end
    end
  end
end
