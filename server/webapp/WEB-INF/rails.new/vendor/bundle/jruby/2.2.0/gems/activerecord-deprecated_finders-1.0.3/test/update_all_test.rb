require 'helper'

describe 'update_all' do
  after do
    Post.destroy_all
  end

  it 'supports conditions' do
    foo = Post.create title: 'foo'
    bar = Post.create title: 'bar'

    assert_deprecated { Post.update_all({ title: 'omg' }, title: 'foo') }

    foo.reload.title.must_equal 'omg'
    bar.reload.title.must_equal 'bar'
  end

  it 'supports limit and order' do
    posts = 3.times.map { Post.create }
    assert_deprecated { Post.update_all({ title: 'omg' }, nil, limit: 2, order: :id) }

    posts.each(&:reload).map(&:title).must_equal ['omg', 'omg', nil]
  end
end
