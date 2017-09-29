# coding: utf-8

class Benchmarking
  class << self
    attr_accessor :warm_up
    alias_method :warm_up?, :warm_up
    attr_writer :trial_count, :loop_count_in_trial

    def trial_count
      @trial_count ||= 3
    end

    def loop_count_in_trial
      @loop_count_in_trial ||= 100
    end

    def pretty_time(time)
      format('%0.05f sec', time)
    end
  end

  attr_reader :name

  def initialize(name, &block)
    @name = name
    @process = block
  end

  def average_time
    measure if times.empty?
    times.reduce(:+) / self.class.trial_count
  end

  alias_method :time, :average_time

  def pretty_time
    self.class.pretty_time(time)
  end

  def times
    @times ||= []
  end

  def inspect
    "#{name} (#{pretty_time})"
  end

  alias_method :to_s, :inspect

  private

  def measure
    fail 'Already measured!' unless times.empty?

    self.class.loop_count.times { run } if self.class.warm_up?

    self.class.trial_count.times do
      GC.start # https://github.com/ruby/ruby/blob/v2_1_2/lib/benchmark.rb#L265

      beginning = Time.now
      self.class.loop_count_in_trial.times { run }
      ending = Time.now

      times << (ending - beginning)
    end
  end

  def run
    @process.call
  end
end

RSpec::Matchers.define :be_faster_than do |other|
  match do |subject|
    if @times
      subject.time < (other.time / @times)
    else
      subject.time < other.time
    end
  end

  chain :at_least do |times|
    @times = times
  end

  # Just a syntax sugar.
  chain :times do
  end

  failure_message do |subject|
    other_label = other.name
    other_label << " / #{@times}" if @times

    label_width = [subject.name, other_label].map { |label| label.length }.max

    message = "#{subject.name.rjust(label_width)}: #{subject.pretty_time}\n"

    if @times
      message << "#{other_label.rjust(label_width)}: "
      shortened_other_time = Benchmarking.pretty_time(other.time / @times)
      message << "#{shortened_other_time} (#{other.pretty_time} / #{@times})"
    else
      message << "#{other_label.rjust(label_width)}: #{other.pretty_time}"
    end
  end
end

RSpec::Matchers.define :be_as_fast_as do |other|
  margin = 1.2

  match do |subject|
    subject.time < (other.time * margin)
  end

  failure_message do |subject|
    label_width = [subject, other].map { |b| b.name.length }.max

    [subject, other].map do |benchmark|
      "#{benchmark.name.rjust(label_width)}: #{benchmark.pretty_time}"
    end.join("\n")
  end
end
