require 'active_support/lazy_load_hooks'
require 'active_record/deprecated_finders/version'

ActiveSupport.on_load(:active_record) do
  require 'active_record/deprecated_finders/base'
  require 'active_record/deprecated_finders/relation'
  require 'active_record/deprecated_finders/dynamic_matchers'
  require 'active_record/deprecated_finders/collection_proxy'
  require 'active_record/deprecated_finders/association_builder'
end
