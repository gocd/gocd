require File.join(Rails.root, 'lib', 'go_cache_store.rb')
ActionController::Base.cache_store = GoCacheStore.new
