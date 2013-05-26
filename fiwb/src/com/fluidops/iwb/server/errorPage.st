$!
  HTML code for the error page
   
  @param contextPath the contextpath of the web application
  @param code the status code being displayed, e.g. 404
  @param message the message being displayed
   
  @author as
!$

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html version="-//W3C//DTD XHTML 1.1//EN"
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<title>Error</title>
<meta name="Copyright"
	content="Copyright &copy; 2012 fluid Operations AG" />
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
</head>

<body style="padding:20px;">

<a href="$contextPath$/"><img style="border:none;" src="$contextPath$/images/iwb_logo.png"/></a>

<p><b>Error: $code$</b></p>

<p>$message$</p>
</body>
</html>