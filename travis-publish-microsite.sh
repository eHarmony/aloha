#!/bin/bash
set -e

git config --global user.email "opensource@eharmony.com"
git config --global user.name "eHarmony"
git config --global push.default simple

# green assumes sourcing travis-bash-fns.sh
green "publishing docs"
sbt docs/publishMicrosite
