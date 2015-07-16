#!/bin/sh
set -e

git clone https://github.com/eHarmony/aloha-proto.git
cd aloha-proto
mvn clean install
cd -
