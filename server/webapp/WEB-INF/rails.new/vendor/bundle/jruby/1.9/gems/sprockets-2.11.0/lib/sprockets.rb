require 'sprockets/version'

module Sprockets
  # Environment
  autoload :Base,                    "sprockets/base"
  autoload :Environment,             "sprockets/environment"
  autoload :Index,                   "sprockets/index"
  autoload :Manifest,                "sprockets/manifest"

  # Assets
  autoload :Asset,                   "sprockets/asset"
  autoload :BundledAsset,            "sprockets/bundled_asset"
  autoload :ProcessedAsset,          "sprockets/processed_asset"
  autoload :StaticAsset,             "sprockets/static_asset"

  # Processing
  autoload :Context,                 "sprockets/context"
  autoload :EcoTemplate,             "sprockets/eco_template"
  autoload :EjsTemplate,             "sprockets/ejs_template"
  autoload :JstProcessor,            "sprockets/jst_processor"
  autoload :Processor,               "sprockets/processor"
  autoload :SassCacheStore,          "sprockets/sass_cache_store"
  autoload :SassFunctions,           "sprockets/sass_functions"
  autoload :SassImporter,            "sprockets/sass_importer"
  autoload :SassTemplate,            "sprockets/sass_template"
  autoload :ScssTemplate,            "sprockets/scss_template"

  # Internal utilities
  autoload :ArgumentError,           "sprockets/errors"
  autoload :AssetAttributes,         "sprockets/asset_attributes"
  autoload :CircularDependencyError, "sprockets/errors"
  autoload :ContentTypeMismatch,     "sprockets/errors"
  autoload :EngineError,             "sprockets/errors"
  autoload :Error,                   "sprockets/errors"
  autoload :FileNotFound,            "sprockets/errors"
  autoload :Utils,                   "sprockets/utils"

  module Cache
    autoload :FileStore, "sprockets/cache/file_store"
  end

  # Extend Sprockets module to provide global registry
  require 'hike'
  require 'sprockets/engines'
  require 'sprockets/mime'
  require 'sprockets/processing'
  require 'sprockets/compressing'
  require 'sprockets/paths'
  extend Engines, Mime, Processing, Compressing, Paths

  @trail             = Hike::Trail.new(File.expand_path('..', __FILE__))
  @mime_types        = {}
  @engines           = {}
  @preprocessors     = Hash.new { |h, k| h[k] = [] }
  @postprocessors    = Hash.new { |h, k| h[k] = [] }
  @bundle_processors = Hash.new { |h, k| h[k] = [] }
  @compressors       = Hash.new { |h, k| h[k] = {} }

  register_mime_type 'text/css', '.css'
  register_mime_type 'application/javascript', '.js'

  require 'sprockets/directive_processor'
  register_preprocessor 'text/css',               DirectiveProcessor
  register_preprocessor 'application/javascript', DirectiveProcessor

  require 'sprockets/safety_colons'
  register_postprocessor 'application/javascript', SafetyColons

  require 'sprockets/charset_normalizer'
  register_bundle_processor 'text/css', CharsetNormalizer

  require 'sprockets/sass_compressor'
  register_compressor 'text/css', :sass, SassCompressor
  register_compressor 'text/css', :scss, SassCompressor

  require 'sprockets/yui_compressor'
  register_compressor 'text/css', :yui, YUICompressor

  require 'sprockets/closure_compressor'
  register_compressor 'application/javascript', :closure, ClosureCompressor

  require 'sprockets/uglifier_compressor'
  register_compressor 'application/javascript', :uglifier, UglifierCompressor
  register_compressor 'application/javascript', :uglify, UglifierCompressor

  require 'sprockets/yui_compressor'
  register_compressor 'application/javascript', :yui, YUICompressor

  # Cherry pick the default Tilt engines that make sense for
  # Sprockets. We don't need ones that only generate html like HAML.

  # Mmm, CoffeeScript
  register_engine '.coffee', Tilt::CoffeeScriptTemplate

  # JST engines
  register_engine '.jst',    JstProcessor
  register_engine '.eco',    EcoTemplate
  register_engine '.ejs',    EjsTemplate

  # CSS engines
  register_engine '.less',   Tilt::LessTemplate
  register_engine '.sass',   SassTemplate
  register_engine '.scss',   ScssTemplate

  # Other
  register_engine '.erb',    Tilt::ERBTemplate
  register_engine '.str',    Tilt::StringTemplate
end
