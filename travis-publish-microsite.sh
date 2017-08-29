#!/bin/bash
set -e

git config --global user.email "r.m.deak@gmail.com"
git config --global user.name "deaktator"
git config --global push.default simple

sbt -d docs/publishMicrosite
