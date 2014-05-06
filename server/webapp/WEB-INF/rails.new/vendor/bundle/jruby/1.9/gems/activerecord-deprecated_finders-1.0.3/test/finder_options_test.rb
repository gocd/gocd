require 'helper'

describe 'apply_finder_options' do
  before do
    @deprecation_behavior = ActiveSupport::Deprecation.behavior
    ActiveSupport::Deprecation.behavior = :silence
  end

  after do
    ActiveSupport::Deprecation.behavior = @deprecation_behavior
  end

  it 'is deprecated' do
    assert_deprecated { Post.scoped.apply_finder_options(:conditions => 'foo') }
  end

  it 'supports :conditions' do
    scope = Post.scoped.apply_finder_options(:conditions => 'foo')
    scope.where_values.must_equal ['foo']
  end

  it 'supports :include' do
    scope = Post.scoped.apply_finder_options(:include => :foo)
    scope.includes_values.must_equal [:foo]
  end

  it 'supports :joins' do
    scope = Post.scoped.apply_finder_options(:joins => :foo)
    scope.joins_values.must_equal [:foo]
  end

  it 'supports :limit' do
    scope = Post.scoped.apply_finder_options(:limit => 5)
    scope.limit_value.must_equal 5
  end

  it 'supports :offset' do
    scope = Post.scoped.apply_finder_options(:offset => 5)
    scope.offset_value.must_equal 5
  end

  it "does not support :references (as it's new in 4.0)" do
    lambda { Post.scoped.apply_finder_options(references: :foo) }.must_raise ArgumentError
  end

  it 'supports :order' do
    scope = Post.scoped.apply_finder_options(:order => 'foo')
    scope.order_values.must_equal ['foo']
  end

  it 'supports :select' do
    scope = Post.scoped.apply_finder_options(:select => :foo)
    scope.select_values.must_equal [:foo]
  end

  it 'supports :readonly' do
    scope = Post.scoped.apply_finder_options(:readonly => :foo)
    scope.readonly_value.must_equal :foo
  end

  it 'supports :group' do
    scope = Post.scoped.apply_finder_options(:group => :foo)
    scope.group_values.must_equal [:foo]
  end

  it 'supports :having' do
    scope = Post.scoped.apply_finder_options(:having => :foo)
    scope.having_values.must_equal [:foo]
  end

  it 'supports :from' do
    scope = Post.scoped.apply_finder_options(:from => :foo)
    scope.from_value.must_equal [:foo, nil]
  end

  it 'supports :lock' do
    scope = Post.scoped.apply_finder_options(:lock => true)
    scope.lock_value.must_equal true
  end
end
