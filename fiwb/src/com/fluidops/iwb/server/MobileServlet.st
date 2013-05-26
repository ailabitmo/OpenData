$!
    Main template for the WikiServlet page.
    
    This template renders the whole screen, top contains infos about viewed
    entity, menu to log in users, plus the "desktop" with Wiki widgets and
    all other optional widgets rendered.
    
    (HTML back ported from the out.println-mess)


    Arguments of this template:
        
    @param term The name of the entity (term) being displayed
    @param refer The name of the referring URL
    @param user The user name (or null if no user logged in)
    @param capitalizedUser The capitalized user name (or null)
    @param uriString The URI of the term
    @param abbreviatedURI The URI of the term (abbreviated form)
    @param path The root path of the web app
    @param body The body of the content area
    @param loggedIn indicates if IWB-user is logged into the system
	@param user the user currently logged in
	@param usermanagement is usermanagement active
	@param admin is current user admin
	@param tracking google analytics tracking code
	@param toolBarButtons additional toolbar buttons

    @author uli
!$
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
         <title>$term$</title>
 
 	 <!-- Sencha Touch CSS -->
	 <link rel="stylesheet" href="$path$/css/sencha-touch.css" type="text/css">

	 <!-- Custom CSS -->
	 <!--<link rel="stylesheet" href="$path$/css/guide.css" type="text/css">-->


	 <!-- Sencha Touch JS -->
<!--	 <script type="text/javascript" src="$path$/sencha/sencha-touch-debug.js"></script>-->
	 <script type="text/javascript" src="$path$/sencha/sencha-touch.js"></script>
<!--<script type="text/javascript" src="$path$/sencha.js"></script>-->
<!-- <script type="text/javascript" src="https://getfirebug.com/firebug-lite.js"></script>-->
 	 <!-- Google Maps JS -->
<!--	 <script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=true"></script>-->
 
 
        <script type='text/javascript' src='$path$/ajax/ajax.js'></script>
        <!--<script type='text/javascript' src='/amchart/swfobject.js'></script>-->       
        <link rel='alternate' type='application/rss+xml' title='New info on $term$' href='$path$/rss.jsp?uri=$uriString$'/>

        <link rel='stylesheet' href='$path$/ajax/stylesheet_fajax.css' type='text/css' />
        $head$
        <link rel='stylesheet' href='$path$/stylesheet_fiwb.css' type='text/css' />
        <link rel='stylesheet' href='$path$/css/semwiki.css' type='text/css' />
        
    <style>
        .refreshBtn {
            margin: 0 !important;
        }
    </style>
        
    </head>

<body>
    <body onload='javascript:fluInit(); $onLoad$'>

    </body>
</html>
