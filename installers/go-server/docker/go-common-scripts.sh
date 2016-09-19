#!/bin/bash

COLOR_START="[01;34m"
COLOR_END="[00m"
MSG_TIME="${MSG_TIME:-30}"

function show_msg() {
  echo -e "${COLOR_START}${@}${COLOR_END}"
}

function wait_for_go_server() {
  local count=0
  until curl -s -o /dev/null 'http://localhost:8153'; do
    sleep 1
    count=$((count + 1))
    [[ "$((count % 30))" = "0" ]] && \
      show_msg "== $(date): Waiting for Go Server dashboard to be accessible ..."
  done
}

function wait_for_msg_time() {
  local message="$@"

  if [ "$MSG_TIME" -gt 0 ] 2>/dev/null; then
    show_msg "$message"
    sleep $MSG_TIME
  fi
}
