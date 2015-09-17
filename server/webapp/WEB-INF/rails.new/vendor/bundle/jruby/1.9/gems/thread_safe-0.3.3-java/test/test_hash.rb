require 'test/unit'
require 'thread_safe'

class TestHash < Test::Unit::TestCase
  def test_concurrency
    hsh = ThreadSafe::Hash.new
    assert_nothing_raised do
      (1..100).map do |i|
        Thread.new do
          1000.times do |j|
            hsh[i*1000+j] = i
            hsh[i*1000+j]
            hsh.delete(i*1000+j)
          end
        end
      end.map(&:join)
    end
  end
end
