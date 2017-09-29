require 'helper'

class TestSourceRange < Minitest::Test
  def setup
    @buf = Parser::Source::Buffer.new('(string)')
    @buf.source = "foobar\nbaz"
  end

  def test_initialize
    sr = Parser::Source::Range.new(@buf, 1, 2)
    assert_equal 1, sr.begin_pos
    assert_equal 2, sr.end_pos
    assert sr.frozen?
  end

  def test_size
    sr = Parser::Source::Range.new(@buf, 1, 3)
    assert_equal 2, sr.size
  end

  def test_bad_size
    assert_raises ArgumentError do
      Parser::Source::Range.new(@buf, 2, 1)
    end
  end

  def test_join
    sr1 = Parser::Source::Range.new(@buf, 1, 2)
    sr2 = Parser::Source::Range.new(@buf, 5, 8)
    sr = sr1.join(sr2)

    assert_equal 1, sr.begin_pos
    assert_equal 8, sr.end_pos
  end

  def test_intersect
    sr1 = Parser::Source::Range.new(@buf, 1, 3)
    sr2 = Parser::Source::Range.new(@buf, 2, 6)
    sr3 = Parser::Source::Range.new(@buf, 5, 8)

    assert_equal 2, sr1.intersect(sr2).begin_pos
    assert_equal 3, sr1.intersect(sr2).end_pos
    assert_equal 5, sr2.intersect(sr3).begin_pos
    assert_equal 6, sr2.intersect(sr3).end_pos
    assert sr1.intersect(sr3) == nil
  end

  def test_disjoint
    sr1 = Parser::Source::Range.new(@buf, 1, 3)
    sr2 = Parser::Source::Range.new(@buf, 2, 6)
    sr3 = Parser::Source::Range.new(@buf, 5, 8)

    assert sr1.disjoint?(sr3)
    assert !sr1.disjoint?(sr2)
    assert !sr2.disjoint?(sr3)
  end

  def test_overlaps
    sr1 = Parser::Source::Range.new(@buf, 1, 3)
    sr2 = Parser::Source::Range.new(@buf, 2, 6)
    sr3 = Parser::Source::Range.new(@buf, 5, 8)

    assert !sr1.overlaps?(sr3)
    assert sr1.overlaps?(sr2)
    assert sr2.overlaps?(sr3)
  end

  def test_empty
    sr1 = Parser::Source::Range.new(@buf, 1, 3)
    sr2 = Parser::Source::Range.new(@buf, 2, 2)
    sr3 = Parser::Source::Range.new(@buf, 7, 8)

    assert !sr1.empty?
    assert sr2.empty?
    assert !sr3.empty?
  end

  def test_line
    sr = Parser::Source::Range.new(@buf, 7, 8)
    assert_equal 2, sr.line
  end

  def test_source_line
    sr = Parser::Source::Range.new(@buf, 7, 8)
    assert_equal 'baz', sr.source_line
  end

  def test_columns
    sr = Parser::Source::Range.new(@buf, 7, 8)
    assert_equal 0, sr.begin.column
    assert_equal 1, sr.end.column
    assert_equal 0...1, sr.column_range
  end

  def test_begin_end
    sr = Parser::Source::Range.new(@buf, 1, 5)

    sr_beg = sr.begin
    assert_equal 1, sr_beg.begin_pos
    assert_equal 1, sr_beg.end_pos

    sr_end = sr.end
    assert_equal 5, sr_end.begin_pos
    assert_equal 5, sr_end.end_pos
  end

  def test_source
    sr = Parser::Source::Range.new(@buf, 0, 3)
    assert_equal 'foo', sr.source

    sr_multi = Parser::Source::Range.new(@buf, 0, 10)
    assert_equal "foobar\nbaz", sr_multi.source
  end

  def test_is?
    sr = Parser::Source::Range.new(@buf, 0, 3)
    assert sr.is?('foo')
    refute sr.is?('bar')
  end

  def test_to_s
    sr = Parser::Source::Range.new(@buf, 8, 9)
    assert_equal '(string):2:2', sr.to_s
  end
end
