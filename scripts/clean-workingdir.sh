#!/bin/bash

set -e

WORKING_DIR=$(cd `dirname $0`/.. && pwd)

cd $WORKING_DIR

ls -A -1 | grep -vF .idea | grep -vF .git | xargs git clean -fdx --

