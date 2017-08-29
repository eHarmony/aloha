#!/bin/bash

rvm use 2.2.3 --install --fuzzy
gem update --system
gem install sass
gem install jekyll -v 3.2.1
