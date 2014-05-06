require 'helper'

describe 'with_scope' do
  before do
    Post.create(id: 1, title: 'foo')
    Post.create(id: 2, title: 'bar')
  end

  after do
    Post.delete_all
  end

  it 'applies a scoping' do
    assert_deprecated do
      Post.with_scope(find: { conditions: { title: 'foo' } }) do
        assert_equal [1], Post.all.map(&:id)
      end
    end
  end

  it 'applies an exclusive scoping' do
    ActiveSupport::Deprecation.silence do
      Post.with_scope(find: { conditions: { title: 'foo' } }) do
        Post.send(:with_exclusive_scope, find: { conditions: { title: 'bar' } }) do
          assert_equal [2], Post.all.map(&:id)
        end
      end
    end
  end

  it 'gives a deprecation for #with_exclusive_scope' do
    assert_deprecated do
      Post.send(:with_exclusive_scope) {}
    end
  end
end
