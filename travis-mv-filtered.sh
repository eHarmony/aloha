#!/bin/sh

set -e

function mv_filtered() {
  for v in 10 11; do
    for d in $(find */target/scala-2.${v}/filtered -mindepth 3 -maxdepth 3 -type d); do
      PROJECT=$(echo $d | grep -Po '^[^/]*')
      if [ $(echo $d | grep '/test/') ]; then
        CLASS_PREFIX='test-'
      else
        CLASS_PREFIX=
      fi

      # trailing slash is intentional.  See man cp
      cp -vR "$d/" "$PROJECT/target/scala-2.${v}/${CLASS_PREFIX}classes/"
      rm -rf $d
    done
  done
}
