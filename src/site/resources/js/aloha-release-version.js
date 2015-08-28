/**
 * Attempt to determine the latest Aloha version from Maven Central via 
 * scraping the version.
 *
 * To use: <script type="text/javascript">
 *           $(document).ready(function(){ alohaReleaseVersion('#version'); });
 *         </script>
 *
 * @param selectors jquery selectors where the version will be appended.
 * @return void
 */
function alohaReleaseVersion(selectors) {
  defVersion = '[ Aloha Version Here ]';
  releaseNumber = /\d+(\.\d+)+/;
  baseUrl = "http://repo1.maven.org/maven2/com/eharmony/aloha/";
  $.ajax({
    url: baseUrl,
    type: "get",
    dataType: "",
    success: function(data) {
      var links = $(data).find('a')
                         .filter(function(idx, item) {
                           return releaseNumber.test($(item).text());
                         }).map(function(idx, item) { 
                           return $(item).text().replace('/', '');
                         });
            
      // Insert into selected content
      var version = 0 <= links.length ? links[links.length - 1] : defVersion;
      $(selectors).append(version);
    },
    error: function(status) {
      $(selectors).append(defVersion);
    }
  });
}
