module ActionView
  module RoutingUrlFor

    # Returns the URL for the set of +options+ provided. This takes the
    # same options as +url_for+ in Action Controller (see the
    # documentation for <tt>ActionController::Base#url_for</tt>). Note that by default
    # <tt>:only_path</tt> is <tt>true</tt> so you'll get the relative "/controller/action"
    # instead of the fully qualified URL like "http://example.com/controller/action".
    #
    # ==== Options
    # * <tt>:anchor</tt> - Specifies the anchor name to be appended to the path.
    # * <tt>:only_path</tt> - If true, returns the relative URL (omitting the protocol, host name, and port) (<tt>true</tt> by default unless <tt>:host</tt> is specified).
    # * <tt>:trailing_slash</tt> - If true, adds a trailing slash, as in "/archive/2005/". Note that this
    #   is currently not recommended since it breaks caching.
    # * <tt>:host</tt> - Overrides the default (current) host if provided.
    # * <tt>:protocol</tt> - Overrides the default (current) protocol if provided.
    # * <tt>:user</tt> - Inline HTTP authentication (only plucked out if <tt>:password</tt> is also present).
    # * <tt>:password</tt> - Inline HTTP authentication (only plucked out if <tt>:user</tt> is also present).
    #
    # ==== Relying on named routes
    #
    # Passing a record (like an Active Record) instead of a hash as the options parameter will
    # trigger the named route for that record. The lookup will happen on the name of the class. So passing a
    # Workshop object will attempt to use the +workshop_path+ route. If you have a nested route, such as
    # +admin_workshop_path+ you'll have to call that explicitly (it's impossible for +url_for+ to guess that route).
    #
    # ==== Implicit Controller Namespacing
    #
    # Controllers passed in using the +:controller+ option will retain their namespace unless it is an absolute one.
    #
    # ==== Examples
    #   <%= url_for(action: 'index') %>
    #   # => /blog/
    #
    #   <%= url_for(action: 'find', controller: 'books') %>
    #   # => /books/find
    #
    #   <%= url_for(action: 'login', controller: 'members', only_path: false, protocol: 'https') %>
    #   # => https://www.example.com/members/login/
    #
    #   <%= url_for(action: 'play', anchor: 'player') %>
    #   # => /messages/play/#player
    #
    #   <%= url_for(action: 'jump', anchor: 'tax&ship') %>
    #   # => /testing/jump/#tax&ship
    #
    #   <%= url_for(Workshop.new) %>
    #   # relies on Workshop answering a persisted? call (and in this case returning false)
    #   # => /workshops
    #
    #   <%= url_for(@workshop) %>
    #   # calls @workshop.to_param which by default returns the id
    #   # => /workshops/5
    #
    #   # to_param can be re-defined in a model to provide different URL names:
    #   # => /workshops/1-workshop-name
    #
    #   <%= url_for("http://www.example.com") %>
    #   # => http://www.example.com
    #
    #   <%= url_for(:back) %>
    #   # if request.env["HTTP_REFERER"] is set to "http://www.example.com"
    #   # => http://www.example.com
    #
    #   <%= url_for(:back) %>
    #   # if request.env["HTTP_REFERER"] is not set or is blank
    #   # => javascript:history.back()
    #
    #   <%= url_for(action: 'index', controller: 'users') %>
    #   # Assuming an "admin" namespace
    #   # => /admin/users
    #
    #   <%= url_for(action: 'index', controller: '/users') %>
    #   # Specify absolute path with beginning slash
    #   # => /users
    def url_for(options = nil)
      case options
      when String
        options
      when nil, Hash
        options ||= {}
        options = { :only_path => options[:host].nil? }.merge!(options.symbolize_keys)
        super
      when :back
        _back_url
      else
        polymorphic_path(options)
      end
    end

    def url_options #:nodoc:
      return super unless controller.respond_to?(:url_options)
      controller.url_options
    end

    def _routes_context #:nodoc:
      controller
    end
    protected :_routes_context

    def optimize_routes_generation? #:nodoc:
      controller.respond_to?(:optimize_routes_generation?, true) ?
        controller.optimize_routes_generation? : super
    end
    protected :optimize_routes_generation?
  end
end
