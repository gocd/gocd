#!/bin/sh

# 30 day retention
echo "deleting the following artifact files older than 30 days:"
find /godata/artifacts/pipelines/ -type f -mtime +30 -print -delete

find /godata/artifacts/pipelines/ -type d -empty -delete
