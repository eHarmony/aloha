/**
 * Attempt to determine the latest Aloha version from Maven Central via 
 * scraping the version.
 *
 * To use: <script type="text/javascript">
 *           $(document).ready(function(){ alohaReleaseVersion(); });
 *         </script>
 *
 * @param selectors jquery selectors where the version will be appended.
 * @return void
 */

function alohaReleaseVersion() {
  var re   = /^Version\s*:\s*(\d+(\.\d+)(\.\d+)?(\-SNAPSHOT)?).*$/;
  var txt  = $('.projectVersion').text();
  var grps = re.exec(txt);

  if (2 <= grps.length)
    $('#version').text(grps[1]);
  else $('#version').text("[Latest Version Here]");
}
