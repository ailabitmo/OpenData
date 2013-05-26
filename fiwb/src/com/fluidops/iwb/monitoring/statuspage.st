$!
  HTML code for the Status Page
  
  This HTML page is rendered by the StatusServlet
   
  @param stateRows a list of state features as key value pairs
  @param contextPath the contextpath of the web application
  
  @author as
!$
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html version="-//W3C//DTD XHTML 1.1//EN"
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<title>Information Workbench status page</title>
<meta name="Copyright"
	content="Copyright &copy; 2012 fluid Operations AG" />
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />

 	<style type="text/css">
	<!--
	.statusOK { width: 24px; height: 24px; background-image: url($contextPath$/images/common/status_okay.png); }
	.statusError { width: 24px; height: 24px; background-image: url($contextPath$/images/common/status_error.png); }
	table tr > td { padding-right: 20px; }
	-->
	</style>

</head>

<body>

<h3>Information Workbench status page</h3>
	
<table>
$stateRows:{ stateRow |
	<tr>
		<td>$stateRow.key$</td>
		<td>$stateRow.value$</td>
	</tr>
}$
</table>

</body>
</html>