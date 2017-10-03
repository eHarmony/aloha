#!/bin/bash
set -e

git config --global user.email "opensource@eharmony.com"
git config --global user.name "eHarmony"
git config --global push.default simple

sbt -J-Xss8m docs/publishMicrosite
