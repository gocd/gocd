require 'helper'

describe 'calculate' do
  after do
    Post.destroy_all
  end

  it 'supports finder options' do
    Post.create id: 1
    Post.create id: 2, title: 'foo'
    Post.create id: 3, title: 'foo'

    assert_deprecated { Post.calculate(:sum, :id, conditions: { title: 'foo' }) }.must_equal 5
  end
end
