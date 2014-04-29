class IO
  def read_available_bytes(chunk_size = 16384, select_timeout = 0.02)
    buffer = []

    return "" if closed? || eof?
    # IO.select cannot be used here due to the fact that it
    # just does not work on windows
    while true
      begin
        IO.select([self], nil, nil, select_timeout)
        break if eof? # stop raising :-(
        buffer << self.readpartial(chunk_size)
      rescue(EOFError)
        break
      end
    end

    return buffer.join
  end
end
