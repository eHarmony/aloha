#!/bin/sh
set -e

# git clone https://github.com/JohnLangford/vowpal_wabbit.git
# cd vowpal_wabbit
# make java
# echo "put the JNI lib in one of these:"
for F in $((echo -e 'public class Main{public static void main(String[] args){ System.out.println(System.getProperty("java.library.path"));}}' > Main.java && javac Main.java && java Main) | tr : '\n'); do
  echo -e "\t$F"
done
exit 1
cd -
