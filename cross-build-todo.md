Need to create an SBT that does the following and is called after
`editsource:edit`.  This plugin needs to be included in the projects that
need it.  Additionally, this plugin should be called as part of the release
process so that com/eharmony/aloha/version.properties has a **release** version
and not a **SNAPSHOT** version.

```zsh
# To be run in project root directory.
function mv_filtered() {
  for v in 10 11; do
    for d in $(find */target/scala-2.${v}/filtered -name "*" -d 3); do
      PROJECT=$(echo $d | grep -Po '^[^/]*')
      if [ $(echo $d | grep '/test/') ]; then
        CLASS_PREFIX='test-'
      else
        CLASS_PREFIX=
      fi

      # trailing slash is intentional.  See man cp
      cp -vR "$d/" "$PROJECT/target/scala-2.${v}/${CLASS_PREFIX}classes/"
    done
  done
}
```
