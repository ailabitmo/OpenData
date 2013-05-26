<%@page trimDirectiveWhitespaces="true" %>
<%@page import="com.fluidops.util.StringUtil"%>
<%@page import="java.net.URI"%>
<%@page import="com.fluidops.config.Config"%>
<%!
	/** above we trim white spaces caused by HTML/JSP tags, so that there are no (leading) empty lines in the output,
		as this might confuse the client application, e.g. a VNC viewer. See bug 6596 for details. 
		Comments there have the same effect, so therefore the comment is here.
	**/
    /** helpers ***/
    boolean isNoE(String value)
    {
        return StringUtil.isNullOrEmpty(value);
    }

	String s(boolean secureMode)
	{
		return secureMode?"s":"";
	}
    
    /** Methods **/
    String getNWBCBaseURL(String host, String instnr, String client, String language, String user, String pass, String port, boolean secureMode)
    {
    	
        // failure cases
        if (isNoE(host) || (isNoE(instnr) && isNoE(port)) )
            return null; // TODO => FAIL !
        // first get the correct port
        String p = port;
       	if (!isNoE(instnr))
       		p = "5"+instnr+"8"+(secureMode?"1":"0");
       	// then build the URL
        String url = "http"+s(secureMode)+"://"+host+":"+p+"/sap/bc/nwbc?";
        if (!isNoE(client))
            url += "sap-client="+client+"&";
        if (!isNoE(language))
            url += "sap-language="+language+"&";
        if (!isNoE(user))
            url += "sap-user="+user+"&";
        if (!isNoE(pass))
            url += "sap-password="+pass+"&";
        return url;
    }
    
    String getMobileMimURL(String host, String port, String user, String pass, boolean secureMode)
    {
    	// failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/webdynpro/dispatcher/sap.com/is~isr~srs~men~app/MainApp?";
        if (!isNoE(user))
            url += "j_user="+user+"&";
        if (!isNoE(pass))
            url += "j_password="+pass+"&";
        return url;
    }

    String getRetailstoreURL(String host, String port, String user, String pass, boolean secureMode)
    {
    	// failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/sap/bc/gui/sap/its/retailstore?";
        if (!isNoE(user))
            url += "sap-user="+user+"&";
        if (!isNoE(pass))
            url += "sap-password="+pass+"&";
        return url;
    }

    String getB2CWebShopURL(String host, String port, String language, String scenario, boolean secureMode)
    {
    	// failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/b2c/init.do?sap-accessibility=&portal=NO&umelogin=YES&";
        if (!isNoE(language))
            url += "language="+language+"&";
        if (!isNoE(scenario))
            url += "scenario.xcm="+scenario+"&";
        return url;
    }    

    String getCRMWebUiURL(String host, String instancenr, String client, String user, String pass, String port, boolean secureMode)
    {
    	// failure cases
        if (isNoE(host) || (isNoE(instancenr) && isNoE(port)))
            return null; // TODO => FAIL !
        // first get the correct port
        String p = port;
        if (!isNoE(instancenr))
        	p = "5"+instancenr+"8"+(secureMode?"1":"0");
        // then build the URL
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/sap/bc/bsp/sap/crm_ui_start/default.htm?";
        if (!isNoE(client))
            url += "sap-client="+client+"&";
        if (!isNoE(user))
            url += "sap-user="+user+"&";
        if (!isNoE(pass))
            url += "sap-password="+pass+"&";
        return url;
    }    
    
    String getSAPWebguiURL(String host, String port, boolean secureMode)
    {
        //failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        return "http"+s(secureMode)+"://"+host+":"+port+"/scripts/wgate/webgui/!";
    }
    
    String getSapShcutURL(String shortcut, String client, String user, String pass, String language)
    {
        //failure cases
        if (isNoE(shortcut))
            return null; // TODO => FAIL !
        String url = "sapshcut.exe -sysname="+shortcut+" -maxgui -type=transaction"; // -command=SM50 -title="Process Overview
        if (!isNoE(client))
            url += " -client="+client;
        if (!isNoE(user))
            url += " -user="+user;
        if (!isNoE(pass))
            url += " -pw="+pass;
        if (!isNoE(language))
            url += " -language="+language;
        
        // TODO: sapshcut.exe should be in PATH !!
        return "C:\\Program Files (x86)\\SAP\\FrontEnd\\SAPgui\\"+url;
    }
    
    String getSapGuiURL(String host, String instnr)
    {
        //failure cases
        if (isNoE(host) || isNoE(instnr))
            return null; // TODO => FAIL !
        return "sapgui.exe "+host+" "+instnr;
    }

    String getPortalURL(String host, String user, String pass, boolean ajax, String port, boolean secureMode)
    {
        //failure cases
        if (isNoE(host))
            return null; // TODO => FAIL !
        // first get the correct port
        if (isNoE(port))
        	// TODO: is this port influenced by the instancenr ?
        	port = "108"+(secureMode?"1":"0");
        // then build the URL
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/irj/portal"+(ajax?"/ajax":"")+"?";
        if (!isNoE(user))
            url += "j_user="+user+"&";
        if (!isNoE(pass))
            url += "j_password="+pass+"&";      
        return url;
    }

    String getBPCUrl(String host, String port, String client, String user, String pass, boolean secureMode)
    {
        //failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        String url = "http"+s(secureMode)+"://"+host+":"+port+"/sap/bpc/web?re=1";
        if (!isNoE(client))
            url += "&sap-client="+client;
        if (!isNoE(user))
            url += "&user_user="+user;
        if (!isNoE(pass))
            url += "&sap-password="+pass;       
        return url;
    }
    
    String getBOEUrl(String host, String port, boolean secureMode)
    {
        //failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        return "http"+s(secureMode)+"://"+host+":"+port+"/BOE/BI";
    }

    String getBOexplorer(String host, String port, boolean secureMode)
    {
        //failure cases
        if (isNoE(host) || isNoE(port))
            return null; // TODO => FAIL !
        return "http"+s(secureMode)+"://"+host+":"+port+"/explorer";
    }
%>

<%
	/** TODO: get user context **/
	/** TODO: check if user has access to the VM he requests (check IP address if user is noadmin / only SES user) **/
	
    /** Protocol **/
    String pro_ = request.getParameter("protocol");
    if (pro_ == null)
    {
        response.sendError( 500, "No valid protocol specified.");
        return;
    }
    String protocol = pro_.toLowerCase();
    
    /** Additional Options **/
    String options = request.getParameter("options");
    
    /** Remote Server **/
    String host = request.getParameter("host");
    if (host == null)
    {
        response.sendError( 500, "No valid host specified.");
        return;
    }
    String port = request.getParameter("port");
    String user = request.getParameter("user");
    String pass = request.getParameter("pass");
    String domain = request.getParameter("domain");
    
    boolean secureMode = false;
    String secure = request.getParameter("secure");
    if (!isNoE(secure))
    {
    	// TODO: check against some specific value??
    	secureMode = true;
    }
    
    String extraparams = request.getParameter("extraparam");
    
    String urlModeSwitch = request.getParameter("urlMode");
    // defaults to false
    boolean urlMode = false;
    if (!isNoE(urlModeSwitch))
    {
    	// if it is set, we use the urlMode
    	// TODO: check against some specific value??
    	urlMode = true;
    }
    
    // citrix has sometimes issues with the windows logon screen (shows a black / blank screen) in case no credentials are provided within the ICA file
    // if citrix XenApp 6.5 is used there is no solution at all at this time
    boolean useCitrixBlankScreenFix = false;
    // use the urlmode in case blankscreenfix is necessary
    if ( useCitrixBlankScreenFix && isNoE(user) )
    	urlMode = true;
    
    
    /** Remote Application) **/
    String sub_protocol = null;
    if (request.getParameter("subprotocol") != null)
        sub_protocol = request.getParameter("subprotocol").toLowerCase();
    // remote host is a URI: //<hostname>:<instace nr>/<SID>
//	String remote_host = request.getParameter("remotehost");
	String remotehost = request.getParameter("remotehost");
	// the sid (e.g. EC3; special case sapshortcut: "EC3 (ECC)")
	String sid = request.getParameter("sid");
	// the instance nr. (e.g. 00)
	String instancenr = request.getParameter("sysnr");
    
    String client = request.getParameter("client");
    String language = request.getParameter("language");
    String app_user = request.getParameter("app_user");
    String app_pass = request.getParameter("app_pass");
    String fixed_url = request.getParameter("fixed_url");

    /** get User Agent **/
    String userAgent = request.getHeader("user-agent");
    boolean isIOS = (userAgent != null) && (userAgent.contains("iPad") || userAgent.contains("iPhone"));
    
    if(isIOS && "JUMPDESKTOP".equals(Config.getConfig().getIOSDesktopIntegration()))
    {
        response.sendRedirect("jump://?host=" + host + "&protocol="+protocol);
        return;
    }
    else if ( "rdp".equals(protocol) )
    {
        response.setContentType( "application/x-rdp" );
        response.setHeader("Content-Disposition", "attachment; filename=\"connect.rdp\"");
        
        // specify port when set
        port = StringUtil.isNullOrEmpty(port) ? "" : ":"+port;

        out.println( "full address:s:"+host+port );
        if( Config.getConfig().clientClipboardRedirect() )
        	out.println( "redirectclipboard:i:1" );
        if( !StringUtil.isEmpty(Config.getConfig().clientDrivesRedirect()) )
        	out.println( "drivestoredirect:s:" + Config.getConfig().clientDrivesRedirect() );
        return;
    }
    else if ( "ica".equals(protocol) && !urlMode )
    {
        response.setContentType( "application/x-ica" );
        // commented out to enable autostart
//        response.setHeader("Content-Disposition", "attachment; filename=\"connect.ica\"");

        out.println( "[Encoding]");
        out.println( "InputEncoding=UTF8");

        
        out.println( "[WFClient]");
        out.println( "ConnectionBar=0");
        out.println( "ProxyFavorIEConnectionSetting=Yes");
        out.println( "ProxyTimeout=30000");
        out.println( "ProxyType=Auto");
        out.println( "ProxyUseFQDN=Off");
        out.println( "RemoveICAFile=yes");
        out.println( "TransportReconnectEnabled=On");
        out.println( "VSLAllowed=On");
        out.println( "Version=2");
        out.println( "VirtualCOMPortEmulation=Off");

        // client drive mapping
        out.println( "CDMAllowed=On");
        // parallel port mapping
        out.println( "CPMAllowed=On");
        
        out.println( "[ApplicationServers]");
        out.println( "OneClickApp=");
        
        out.println( "[OneClickApp]");      
        out.println( "Address="+host); 
        if (!StringUtil.isNullOrEmpty(user))
            out.println( "Username="+user); 
        if (!StringUtil.isNullOrEmpty(domain))
            out.println( "Domain="+domain);
        // TODO: get encrypted passwords (through some API)
        // TODO: even better, use tickets (through XML Broker of WebInterface)
        // Password= 
        // Specifies the password for the user account. This is an optional field. 
        // The password, if used, must be encrypted. To enter an encrypted password into the ICA file, 
        // use the Citrix ICA Client Remote Application Manager New Entry Wizard to create a remote 
        // application entry. When you are prompted for the username and password, enter the password that 
        // you want to use in the ICA file. Finish the New Entry wizard. Open the file APPSRV.INI in the 
        // Windows directory and locate the entry you just created. Copy the password value and paste it into 
        // your ICA file. 
        if (!StringUtil.isNullOrEmpty(pass))
            out.println( "ClearPassword="+pass);
        out.println( "DesiredColor=8");
        out.println( "Compress=On");
        out.println( "TransportDriver=TCP/IP");
        out.println( "WinStationDriver=ICA 3.0");
        out.println( "BrowserProtocol=HTTPonTCP");
        if (sub_protocol == null || sub_protocol.isEmpty() || sub_protocol.equals("desktop")){
        	// desktop mode
        	out.println( "TransparentKeyPassthrough=FullScreenOnly");
        	out.println( "DesiredHRES=4294967295" );
        	out.println( "DesiredVRES=4294967295" );
        	out.println( "TWIMODE=OFF" );
        } else {
        	// seamless app mode
        	out.println( "TransparentKeyPassthrough=Remote");
        	// TODO: still testing which option is best
        	out.println( "ScreenPercent=100");
//        	out.println( "DesiredHRES=4294967295" );
//        	out.println( "DesiredVRES=4294967295" );
        	out.println( "TWIMODE=ON" );       	
        }
        out.println( "ClientAudio=On");
        out.println( "AudioBandwidthLimit=1");
        out.println( "DesiredWinType=8" );
        
        // TODO: what are the following params good for?
        out.println( "AutologonAllowed=ON" );
//        out.println( "CGPAddress=*:2598" );
//        out.println( "DoNotUseDefaultCSL=On" );
        out.println( "FontSmoothingType=0" );
		// REWD (Reconnect Web Duration): The time taken to enumerate a user's existing sessions as part of the reconnect functionality of Workspace Control. That is, the time from when Web Interface asks the XML service to enumerate the sessions until the response is received.
		// NRWD (Name Resolution Web Duration): The time taken to convert an application name to a server address, including load balancing decisions and so on. This is the time taken in the XML service from when Web Interface requests a name resolution to when the response is received.
		// TRWD (Ticket Response Web Duration): The time taken to request a logon ticket from the selected server. Again, this is the time taken in the XML service from when Web Interface requests a logon ticket to when the response is received. This includes IMA communications to the target server.
		// LPWD (Launch Page Web Duration): The time taken by Web Interface to process the launch.aspx page, including the cost of NRWD and TRWD (so this should always be at least as big as NRWD plus TRWD). This includes all other processing in Web Interface.
        out.println( "LPWD=5" );
        out.println( "NRWD=115" );
        out.println( "TRWD=2" );
        
        out.println( "LocHttpBrowserAddress=!" );
        // TODO not inherited from WFClient configuration?
        out.println( "ProxyTimeout=30000" );
        out.println( "ProxyType=Auto" );

      	// FR redirects the logged on user's document and desktop folders to client’s document and desktop folders respectively
        out.println( "SFRAllowed=Off" );
        out.println( "SSLEnable=Off" );       

        ///
        String connectString = "";
        if (sub_protocol == null || sub_protocol.isEmpty() || sub_protocol.equals("desktop"))
            // no setting in case of desktop mode !!
            connectString = null;
        else if (sub_protocol.equals("nwbc-web"))
            connectString = "iexplore.exe "+getNWBCBaseURL(remotehost,instancenr,client,language,app_user,app_pass, port, secureMode);
        else if (sub_protocol.equals("nwbc-thick"))
            connectString = "iexplore.exe sap-nwbc://"+getNWBCBaseURL(remotehost,instancenr,client,language,app_user,app_pass, port, secureMode);
        else if (sub_protocol.equals("sap-webgui"))
            connectString = "iexplore.exe "+getSAPWebguiURL(remotehost,port, secureMode);
        else if (sub_protocol.equals("sapgui-shcut"))
            connectString = getSapShcutURL(sid,client,app_user,app_pass,language);    
        else if (sub_protocol.equals("sapgui-thick"))
            connectString = getSapGuiURL(remotehost,instancenr);
        else if (sub_protocol.equals("portal"))
            connectString = "iexplore.exe "+getPortalURL(remotehost,app_user,app_pass,false, port, secureMode);
        else if (sub_protocol.equals("portal_ajax"))
            connectString = "iexplore.exe "+getPortalURL(remotehost,app_user,app_pass,true, port, secureMode);
        else if (sub_protocol.equals("fixed_url"))
            connectString = "iexplore.exe "+fixed_url;
        else if (sub_protocol.equals("BPC"))
            connectString = "iexplore.exe "+getBPCUrl(remotehost,port,client,app_user,app_pass, secureMode);
        else if (sub_protocol.equals("BOE"))
            connectString = "iexplore.exe "+getBOEUrl(remotehost,port, secureMode);
        else if (sub_protocol.equals("BO-explorer"))
            connectString = "iexplore.exe "+getBOexplorer(remotehost,port, secureMode);
        else if (sub_protocol.equals("CRM-webui"))
            connectString = "iexplore.exe "+getCRMWebUiURL(remotehost,instancenr,client,app_user,app_pass, port, secureMode);
        else if (sub_protocol.equals("B2C-webshop"))
        	// TODO: some params are missing currently
        	// default port = 50000
            connectString = "iexplore.exe "+getB2CWebShopURL(remotehost,port,language,"UNKNOWN", secureMode);
        else if (sub_protocol.equals("mobile-mim"))
            // default port = 50100
            connectString = "iexplore.exe "+getMobileMimURL(remotehost,port,app_user,app_pass, secureMode);
        else if (sub_protocol.equals("retailstore"))
            // default port = 55380
            connectString = "iexplore.exe "+getRetailstoreURL(remotehost,port,app_user,app_pass, secureMode);
        
        // check and add for extraparams (only if connectString is set at all)
        if  (connectString != null && extraparams != null)
            connectString += extraparams;
        
        // set the initial program
        if (connectString != null)
            out.println( "InitialProgram="+connectString);
        out.println( "WorkDirectory=c:\\Temp"); 
        out.println( "UseAlternateAddress=0");
        out.println( "KeyboardTimer=100");
        out.println( "MouseTimer=50"); 

        out.println( "[Compress]");
//        out.println( "DriverName= PDCOMP.DLL");
        out.println( "DriverNameWin16=PDCOMPW.DLL");
        out.println( "DriverNameWin32=PDCOMPN.DLL");
        
        out.println( "[EncRC5-0]");
        out.println( "DriverNameWin16=pdc0w.dll");
        out.println( "DriverNameWin32=pdc0n.dll");

        out.println( "[EncRC5-128]");
        out.println( "DriverNameWin16=pdc128w.dll");
        out.println( "DriverNameWin32=pdc128n.dll");

        out.println( "[EncRC5-40]");
        out.println( "DriverNameWin16=pdc40w.dll");
        out.println( "DriverNameWin32=pdc40n.dll");

        out.println( "[EncRC5-56]");
        out.println( "DriverNameWin16=pdc56w.dll");
        out.println( "DriverNameWin32=pdc56n.dll");
//      PersistentCacheEnabled=On 
//      PersistentCacheSize=42935633 
//      PersistentCacheMinBitmap=8192 
//      PersistentCachePath=C:\WINNT\Profiles\amitb\Application Data\ICAClient\Cache
        return;
    }
    else if ( "ica".equals(protocol) && urlMode )
    {
    	// here we are using the Citrix WebInterface instead of ICA files
    	String webInterfaceLink = "http://"+host+":1080";
        String baseString = webInterfaceLink+"/Citrix/XenApp/site/launcher.aspx?CTX_Application=Citrix.MPS.App.CTA.";
        // TODO: do auth here !!
        
        // TODO: make published app names configurable
        if (sub_protocol == null || sub_protocol.isEmpty())
        	// open the webinterface if user did not specify what to open
        	response.sendRedirect( webInterfaceLink );
        else if (sub_protocol.equals("desktop"))
            //TOOD: need the correct name of the published desktop !!
            response.sendRedirect( baseString+"Desktop" );
        else if (sub_protocol.equals("nwbc-web"))           
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine=http:"+getNWBCBaseURL(remotehost,instancenr,client,language,app_user,app_pass,port, secureMode));
        else if (sub_protocol.equals("nwbc-thick"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine=sap-nwbc:"+getNWBCBaseURL(remotehost,instancenr,client,language,app_user,app_pass,port, secureMode));
        else if (sub_protocol.equals("sap-webgui"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getSAPWebguiURL(remotehost,port, secureMode));
        else if (sub_protocol.equals("sapgui-shcut"))
            // TODO: how to provide the parameters??
            response.sendError( 500, "SAP Gui not supported in this mode.");      
        else if (sub_protocol.equals("sapgui-thick"))
            // TODO: how to provide the parameters??
            response.sendError( 500, "SAP Gui not supported in this mode.");
        else if (sub_protocol.equals("portal"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getPortalURL(remotehost,app_user,app_pass,false,port, secureMode));
        else if (sub_protocol.equals("portal_ajax"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getPortalURL(remotehost,app_user,app_pass,true,port, secureMode));
        else if (sub_protocol.equals("fixed_url"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+fixed_url);
        else if (sub_protocol.equals("BPC"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getBPCUrl(remotehost,port,client,app_user,app_pass, secureMode));
        else if (sub_protocol.equals("BOE"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getBOEUrl(remotehost,port, secureMode));
        else if (sub_protocol.equals("BO-explorer"))
        	response.sendRedirect( baseString+"Internet%20Explorer&NFuse_AppCommandLine="+getBOexplorer(remotehost,port, secureMode));
    	return;
    }
    else if ( "vnc".equals(protocol) )
    {
        //default port if port not set
        if(StringUtil.isNullOrEmpty(port))
            port = "5900";
            
        response.setHeader("Content-Disposition", "attachment; filename=\"connect.vnc\"");
        response.setContentType( /*"application/x-vnc"*/ "VncViewer/Config" );
        out.println("[connection]");
        out.println("host="+host);
        out.println("port="+port);
        return;
    }

    response.sendError( 500, "Unknown protocol: "+protocol );
%>
