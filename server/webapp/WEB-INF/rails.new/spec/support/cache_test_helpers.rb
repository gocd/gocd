module CacheTestHelpers
  def check_fragment_caching(obj1, obj2, cache_key_proc)
    ActionController::Base.cache_store.clear
    ActionController::Base.perform_caching = false

    yield obj1
    obj_1_not_cached_body = response.body
    ActionController::Base.cache_store.writes.length.should == 0
    allow_double_render
    ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should be_nil
    ActionController::Base.perform_caching = true

    yield obj2
    ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should_not be_nil
    ActionController::Base.cache_store.writes.length.should == 1
    allow_double_render

    yield obj2
    ActionController::Base.cache_store.writes.length.should == 1
    allow_double_render

    ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should be_nil
    yield obj1
    ActionController::Base.cache_store.writes.length.should == 2
    ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should_not be_nil
    assert_equal obj_1_not_cached_body, response.body
  ensure
    ActionController::Base.perform_caching = false
  end

# erase_results does not exist, in Rails 3 and above.
# https://github.com/markcatley/responds_to_parent/pull/2/files
# http://www.dixis.com/?p=488
  def allow_double_render
    self.instance_variable_set(:@_response_body, nil)
  end
end