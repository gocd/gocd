require 'test/unit'
require 'thread_safe'

class TestArray < Test::Unit::TestCase
  def test_concurrency
    ary = ThreadSafe::Array.new
    assert_nothing_raised do
      (1..100).map do |i|
        Thread.new do
          1000.times do
            ary << i
            ary.each {|x| x * 2}
            ary.shift
            ary.last
          end
        end
      end.map(&:join)
    end
  end
end
