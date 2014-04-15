# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


class ProgressBar

  class << self

    def start(args, &block)
      new(args).start &block
    end

    def width
      @width ||= $terminal.output_cols || 0
    end

  end

  def initialize(args = {})
    @title = args[:title] || ''
    @total = args[:total] || 0
    @mark = args[:mark] || '.'
    @format = args[:format] || default_format
    @output = args[:output] || $stderr unless args[:hidden] || !$stdout.isatty
    clear
  end

  def start
    @start = @last_time = Time.now
    @count = 0
    @finished = false
    render
    if block_given?
      result = yield(self) if block_given?
      finish
      result
    else
      self
    end
  end

  def inc(count)
    set @count + count
  end

  def <<(bytes)
    inc bytes.size
  end

  def set(count)
    @count = [count, 0].max
    @count = [count, @total].min unless @total == 0
    render if changed?
  end

  def title
    return @title if ProgressBar.width <= 10
    @title.size > ProgressBar.width / 5 ? (@title[0, ProgressBar.width / 5 - 2] + '..') : @title
  end

  def count
    human(@count)
  end

  def total
    human(@total)
  end

  def percentage
    '%3d%%' % (@total == 0 ? 100 : (@count * 100 / @total))
  end

  def time
    @finished ? elapsed : eta
  end

  def eta
    return 'ETA:  --:--:--' if @count == 0
    elapsed = Time.now - @start
    eta = elapsed * @total / @count - elapsed
    'ETA:  %s' % duration(eta.ceil)
  end

  def elapsed
    'Time: %s' % duration(Time.now - @start)
  end

  def rate
    '%s/s' % human(@count / (Time.now - @start))
  end

  def progress(width)
    width -= 2
    marks = @total == 0 ? width : (@count * width / @total)
    "|%-#{width}s|" % (@mark * marks)
  end

  def human(bytes)
    magnitude = (0..3).find { |i| bytes < (1024 << i * 10) } || 3
    return '%dB' % bytes if magnitude == 0
    return '%.1f%s' % [ bytes.to_f / (1 << magnitude * 10), [nil, 'KB', 'MB', 'GB'][magnitude] ]
  end

  def duration(seconds)
    '%02d:%02d:%02d' % [seconds / 3600, (seconds / 60) % 60, seconds % 60]
  end

  def finish
    unless @finished
      @finished = true
      render
    end
  end

protected

  def clear
    return if @output == nil || ProgressBar.width <= 0
    @output.print "\r", " " * (ProgressBar.width - 1), "\r"
    @output.flush
  end

  def render
    return unless @output
    format, *args = @format
    line = format % args.map { |arg| send(arg) }
    if ProgressBar.width >= line.size
      @output.print line.sub('|--|') { progress(ProgressBar.width - line.size + 3) }
    else
      @output.print line.sub('|--|', '')
    end
    @output.print @finished ? "\n" : "\r"
    @output.flush
    @previous = @count
    @last_time = Time.now
  end

  def changed?
    return false unless @output && Time.now - @last_time > 0.1
    return human(@count) != human(@previous) if @total == 0
    return true if (@count - @previous) >= @total / 100
    return Time.now - @last_time > 1
  end

  def default_format
    @total == 0 ? ['%s %8s %s', :title, :count, :elapsed] : ['%s: %s |--| %8s/%s %s', :title, :percentage, :count, :total, :time]
  end

end