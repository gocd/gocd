require 'uri'
require 'js_routes/engine' if defined?(Rails)
require 'js_routes/version'

class JsRoutes

  #
  # OPTIONS
  #

  DEFAULT_PATH = File.join('app','assets','javascripts','routes.js')

  DEFAULTS = {
    namespace: "Routes",
    exclude: [],
    include: //,
    file: DEFAULT_PATH,
    prefix: nil,
    url_links: nil,
    camel_case: false,
    default_url_options: {},
    compact: false,
    serializer: nil
  }

  NODE_TYPES = {
    GROUP: 1,
    CAT: 2,
    SYMBOL: 3,
    OR: 4,
    STAR: 5,
    LITERAL: 6,
    SLASH: 7,
    DOT: 8
  }

  LAST_OPTIONS_KEY = "options".freeze

  class Options < Struct.new(*DEFAULTS.keys)
    def to_hash
      Hash[*members.zip(values).flatten(1)].symbolize_keys
    end
  end

  #
  # API
  #

  class << self
    def setup(&block)
      options.tap(&block) if block
    end

    def options
      @options ||= Options.new.tap do |opts|
        DEFAULTS.each_pair {|k,v| opts[k] = v}
      end
    end

    def generate(opts = {})
      # Ensure routes are loaded. If they're not, load them.
      if Rails.application.routes.named_routes.routes.keys.empty?
        Rails.application.reload_routes!
      end

      new(opts).generate
    end

    def generate!(file_name=nil, opts = {})
      if file_name.is_a?(Hash)
        opts = file_name
        file_name = opts[:file]
      end
      new(opts).generate!(file_name)
    end

    # Under rails 3.1.1 and higher, perform a check to ensure that the
    # full environment will be available during asset compilation.
    # This is required to ensure routes are loaded.
    def assert_usable_configuration!
      if 3 == Rails::VERSION::MAJOR && !Rails.application.config.assets.initialize_on_precompile
        raise("Cannot precompile js-routes unless environment is initialized. Please set config.assets.initialize_on_precompile to true.")
      end
      true
    end

    def json(string)
      ActiveSupport::JSON.encode(string)
    end
  end

  #
  # Implementation
  #

  def initialize(options = {})
    @options = self.class.options.to_hash.merge(options)
  end

  def generate
    js = File.read(File.dirname(__FILE__) + "/routes.js")
    js.gsub!("GEM_VERSION", JsRoutes::VERSION)
    js.gsub!("APP_CLASS", Rails.application.class.to_s)
    js.gsub!("NAMESPACE", @options[:namespace])
    js.gsub!("DEFAULT_URL_OPTIONS", json(@options[:default_url_options].merge(deprecated_default_format)))
    js.gsub!("PREFIX", @options[:prefix] || "")
    js.gsub!("NODE_TYPES", json(NODE_TYPES))
    js.gsub!("SERIALIZER", @options[:serializer] || "null")
    js.gsub!("ROUTES", js_routes)
  end

  def deprecated_default_format
    if @options.key?(:default_format)
      warn("default_format option is deprecated. Use default_url_options = { format: <format> } instead")
      { format: @options[:default_format] }
    else
      {}
    end
  end

  def generate!(file_name = nil)
    # Some libraries like Devise do not yet loaded their routes so we will wait
    # until initialization process finish
    # https://github.com/railsware/js-routes/issues/7
    Rails.configuration.after_initialize do
      file_name ||= self.class.options['file']
      File.open(Rails.root.join(file_name || DEFAULT_PATH), 'w') do |f|
        f.write generate
      end
    end
  end

  protected

  def js_routes
    js_routes = Rails.application.routes.named_routes.routes.sort_by(&:to_s).flat_map do |_, route|
      rails_engine_app = get_app_from_route(route)
      if rails_engine_app.respond_to?(:superclass) && rails_engine_app.superclass == Rails::Engine && !route.path.anchored
        rails_engine_app.routes.named_routes.map do |_, engine_route|
          build_route_if_match(engine_route, route)
        end
      else
        build_route_if_match(route)
      end
    end.compact

    "{\n" + js_routes.join(",\n") + "}\n"
  end

  def get_app_from_route(route)
    # rails engine in Rails 4.2 use additional ActionDispatch::Routing::Mapper::Constraints, which contain app
    if route.app.respond_to?(:app) && route.app.respond_to?(:constraints)
      route.app.app
    else
      route.app
    end
  end

  def build_route_if_match(route, parent_route=nil)
    if any_match?(route, parent_route, @options[:exclude]) || !any_match?(route, parent_route, @options[:include])
      nil
    else
      build_js(route, parent_route)
    end
  end

  def any_match?(route, parent_route, matchers)
    full_route = [parent_route.try(:name), route.name].compact.join('_')

    matchers = Array(matchers)
    matchers.any? {|regex| full_route =~ regex}
  end

  def build_js(route, parent_route)
    name = [parent_route.try(:name), route.name].compact
    parent_spec = parent_route.try(:path).try(:spec)
    required_parts, optional_parts = route.required_parts.clone, (route.parts-route.required_parts)
    optional_parts.push(required_parts.delete :format) if required_parts.include?(:format)
    route_name = generate_route_name(name, (:path unless @options[:compact]))
    url_link = generate_url_link(name, route_name, required_parts, route)
    _ = <<-JS.strip!
  // #{name.join('.')} => #{parent_spec}#{route.path.spec}
  // function(#{[required_parts, LAST_OPTIONS_KEY].flatten.join(', ')})
  #{route_name}: Utils.route(#{json(required_parts)}, #{json(optional_parts)}, #{json(serialize(route.path.spec, parent_spec))}, arguments)#{",\n" + url_link if url_link.length > 0}
  JS
  end

  def generate_url_link(name, route_name, required_parts, route)
    return "" unless @options[:url_links]
    _ = <<-JS.strip!
    #{generate_route_name(name, :url)}: function(#{build_params(required_parts)}) {
    return #{generate_base_url_js(route)} + this.#{route_name}(#{build_params(required_parts)});
    }
    JS
  end

  def generate_base_url_js(route)
    # preserve and deprecate previous behavior
    unless @options[:url_links] == true
      ActiveSupport::Deprecation.warn('js-routes url_links config value must be a boolean. Use default_url_options for specifying a default host.')
      raise "invalid URL format in url_links (ex: http[s]://example.com)" if @options[:url_links].match(URI::Parser.new.make_regexp(%w(http https))).nil?
      return "#{@options[:url_links].inspect}"
    else
      protocol = route.defaults[:protocol] || @options[:default_url_options][:protocol] || 'http'
      hostname = route.defaults[:host] || @options[:default_url_options][:host]
      port = route.defaults[:port] || (@options[:default_url_options][:port] unless route.defaults[:host])
      port = ":#{port}" if port

      unless hostname
        raise "A :default_url_options[:host] must be configured in order to generate *_url helpers"
      end

      return %Q|'#{protocol}://#{hostname}#{port}'|
    end
  end

  def generate_route_name(name, suffix)
    route_name = name.join('_')
    route_name << "_#{ suffix }" if suffix
    @options[:camel_case] ? route_name.camelize(:lower) : route_name
  end

  def json(string)
    self.class.json(string)
  end

  def build_params(required_parts)
    params = required_parts.map do |name|
      # prepending each parameter name with underscore
      # to prevent conflict with JS reserved words
      "_#{name}"
    end << LAST_OPTIONS_KEY
    params.join(", ")
  end

  # This function serializes Journey route into JSON structure
  # We do not use Hash for human readable serialization
  # And preffer Array serialization because it is shorter.
  # Routes.js file will be smaller.
  def serialize(spec, parent_spec=nil)
    return nil unless spec
    return spec.tr(':', '') if spec.is_a?(String)
    result = serialize_spec(spec, parent_spec)
    if parent_spec && result[1].is_a?(String)
      result = [
        # We encode node symbols as integer
        # to reduce the routes.js file size
        NODE_TYPES[:CAT],
        serialize_spec(parent_spec),
        result
      ]
    end
    result
  end

  def serialize_spec(spec, parent_spec=nil)
    [
      NODE_TYPES[spec.type],
      serialize(spec.left, parent_spec),
      spec.respond_to?(:right) && serialize(spec.right)
    ]
  end
end

