SeriesList = {} || SeriesList;

SeriesList.init = function(){
  $('#series-table-container').xslt('/admin/rest/series', 'xsl/series_list.xsl', function(){
  
  });
}