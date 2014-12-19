require 'helper'

describe 'finders' do
  before do
    @posts = 3.times.map { |i| Post.create id: i + 1 }
  end

  after do
    Post.delete_all
  end

  it 'supports find(:all) with no options' do
    assert_deprecated { Post.find(:all) }.must_equal @posts
  end

  it 'supports find(:all) with options' do
    assert_deprecated { Post.find(:all, conditions: 'id >= 2') }.must_equal [@posts[1], @posts[2]]
  end

  it 'supports all with options' do
    assert_deprecated { Post.all(conditions: 'id >= 2') }.must_equal [@posts[1], @posts[2]]
  end

  it 'returns an array from all when there are options' do
    posts = ActiveSupport::Deprecation.silence { Post.all(conditions: 'id >= 2') }
    posts.class.must_equal Array
  end

  it 'returns a relation from all when there are no options' do
    posts = Post.all
    posts.is_a?(ActiveRecord::Relation).must_equal true
  end

  it 'deprecates #all on a relation' do
    relation = Post.where('id >= 2')
    assert_deprecated { relation.all }.must_equal [@posts[1], @posts[2]]
  end

  it 'supports find(:first) with no options' do
    assert_deprecated { Post.order(:id).find(:first) }.must_equal @posts.first
  end

  it 'supports find(:first) with options' do
    assert_deprecated { Post.order(:id).find(:first, conditions: 'id >= 2') }.must_equal @posts[1]
  end

  it 'supports first with options' do
    assert_deprecated { Post.order(:id).first(conditions: 'id >= 2') }.must_equal @posts[1]
  end

  it 'supports find(:last) with no options' do
    assert_deprecated { Post.order(:id).find(:last) }.must_equal @posts.last
  end

  it 'supports find(:last) with options' do
    assert_deprecated { Post.order(:id).find(:last, conditions: 'id <= 2') }.must_equal @posts[1]
  end

  it 'supports last with options' do
    assert_deprecated { Post.order(:id).last(conditions: 'id <= 2') }.must_equal @posts[1]
  end

  it 'support find(1) etc with options' do
    assert_deprecated do
      Post.find(1, conditions: '1=1').must_equal Post.find(1)
      lambda { Post.find(1, conditions: '0=1') }.must_raise(ActiveRecord::RecordNotFound)
    end
  end
end
