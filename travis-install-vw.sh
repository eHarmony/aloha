#!/bin/sh
set -e

# This is the git hash for the JNI 8.4.1 release
# For more info, see: https://github.com/JohnLangford/vowpal_wabbit/tree/master/java
#
VW_RELEASE_HASH=10bd09ab06f59291e04ad7805e88fd3e693b7159
# JAVA_LIB_PATH_DIR=/usr/java/packages/lib/amd64
JAVA_LIB_PATH_DIR=$HOME/lib
git clone https://github.com/JohnLangford/vowpal_wabbit.git
cd vowpal_wabbit
git fetch
git checkout $VW_RELEASE_HASH
make java
mkdir -p $JAVA_LIB_PATH_DIR
cp java/target/libvw_jni.so $JAVA_LIB_PATH_DIR
echo "Files in $JAVA_LIB_PATH_DIR"
ls -halF $JAVA_LIB_PATH_DIR
# VW's libvw_jni.so needs to go in one of the following:
#   /usr/java/packages/lib/amd64
#   /usr/lib64
#   /lib64
#   /lib
#   /usr/lib
#
# based on the following code:
#
#   for F in $((echo -e 'public class Main{public static void main(String[] args){System.out.println(System.getProperty("java.library.path"));}}' > Main.java && javac Main.java && java Main) | tr : '\n'); do
#     echo -e "\t$F"
#   done
#

# exit 1
cd -
