#!/usr/bin/env bash

# Run this script to update repo with the latest weka-trunk

set -x

git checkout weka-trunk && \
    git svn rebase && \
    git checkout master && \
    git rebase weka-trunk && \
    git push origin master -f && \
    git checkout weka-trunk && \
    git push origin weka-trunk && \
    git checkout master
