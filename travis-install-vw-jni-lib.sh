#!/bin/sh
set -e

# This is the git hash for the JNI 8.4.1 release
# For more info, see: https://github.com/JohnLangford/vowpal_wabbit/tree/master/java
#
VW_RELEASE_HASH=10bd09ab06f59291e04ad7805e88fd3e693b7159

# Where to put VW library.  See .travis.yml.
JAVA_LIB_PATH_DIR=$HOME/lib

# git clone https://github.com/JohnLangford/vowpal_wabbit.git
git fetch https://github.com/JohnLangford/vowpal_wabbit.git $VW_RELEASE_HASH
cd vowpal_wabbit
#git fetch
#git checkout $VW_RELEASE_HASH
make java
mkdir -p $JAVA_LIB_PATH_DIR
cp java/target/libvw_jni.so $JAVA_LIB_PATH_DIR
cd -
