/*
 * Copyright (C) 2008-2012, fluid Operations AG
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.fluidops.iwb.servicedaemon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.tanukisoftware.wrapper.*;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;


/**
 * wrapper to expose IWB as a windows service
 */
public class Main implements WrapperListener, Runnable
{
//    private MonitorFrame _monitor;
    
    /**
     * Application's main method
     */
    private Method m_mainMethod;
    
    /**
     * Command line arguments to be passed on to the application
     */
    private String[] m_appArgs;
    
    /**
     * Gets set to true when the thread used to launch the application completes.
     */
    private boolean m_mainComplete;
    
    /**
     * Exit code to be returned if the application fails to start.
     */
    private Integer m_mainExitCode;
    
    /**
     * Flag used to signify that the start method is done waiting for the application to start.
     */
    private boolean m_waitTimedOut;
    
  /*---------------------------------------------------------------
   * Constructors
   *-------------------------------------------------------------*/
    /**
     * Creates an instance of a WrapperSimpleApp.
     */
    private Main(Method mainMethod)
    {
        m_mainMethod = mainMethod;
    }
    
  /*---------------------------------------------------------------
   * Runnable Methods
   *-------------------------------------------------------------*/
    /**
   * Used to launch the application in a separate thread.
   */
    @SuppressWarnings(value="IS2_INCONSISTENT_SYNC", justification="Checked. Service only invoked at startup")
    public void run()
    {
        Throwable t = null;
        try
        {
            if (WrapperManager.isDebugEnabled())
            {
                System.out.println("serviceMain: invoking main method");
            }
            m_mainMethod.invoke(null, new Object[]
            { m_appArgs });
            if (WrapperManager.isDebugEnabled())
            {
                System.out.println("serviceMain: main method completed");
            }
            
            synchronized (this)
            {
                // Let the start() method know that the main method returned, in case it is
                //  still waiting.
                m_mainComplete = true;
                this.notifyAll();
            }
            
            return;
        }
        catch (IllegalAccessException e)
        {
            t = e;
        }
        catch (IllegalArgumentException e)
        {
            t = e;
        }
        catch (InvocationTargetException e)
        {
            t = e.getTargetException();
            if (t == null)
            {
                t = e;
            }
        }
        
        // If we get here, then an error was thrown.  If this happened quickly
        // enough, the start method should be allowed to shut things down.
        System.out.println();
        System.out.println("serviceMain: Encountered an error running main: " + t);
        
        // We should print a stack trace here, because in the case of an
        // InvocationTargetException, the user needs to know what exception
        // their app threw.
        t.printStackTrace();
        
        synchronized (this)
        {
            if (m_waitTimedOut)
            {
                // Shut down here.
                WrapperManager.stop(1);
                return; // Will not get here.
            }
            else
            {
                // Let start method handle shutdown.
                m_mainComplete = true;
                m_mainExitCode = 1;
                this.notifyAll();
                return;
            }
        }
    }
    
  /*---------------------------------------------------------------
   * WrapperListener Methods
   *-------------------------------------------------------------*/
    /**
   * The start method is called when the WrapperManager is signalled by the
   * native wrapper code that it can start its application.  This
   * method call is expected to return, so a new thread should be launched
   * if necessary.
   * If there are any problems, then an Integer should be returned, set to
   * the desired exit code.  If the application should continue,
   * return null.
   * 
   * @param args the args
   * 
   * @return the integer
   */
    @SuppressWarnings(value="EI_EXPOSE_REP2", justification="Checked. Only secure invocation")
    public Integer start(String[] args)
    {
        if (WrapperManager.isDebugEnabled())
        {
            System.out.println("serviceMain: start(args)");
//            try
//            {
//                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
//                _monitor = Monitor.getMonitorFrame(Monitor.DEBUG_MODE, "Service Monitor");
//                _monitor.show();
//            }
//            catch(Throwable e)
//            {
//                System.err.println("Probably no Screen, no start of Monitor");
//            }
        }
        
        Thread mainThread = new Thread(this, "serviceMain");
        synchronized (this)
        {
            m_appArgs = args;
            mainThread.start();
            // Wait for two seconds to give the application a chance to have failed.
            try
            {
                this.wait(2000);
            }
            catch (InterruptedException e)
            {
            }
            m_waitTimedOut = true;
            
            if (WrapperManager.isDebugEnabled())
            {
                System.out.println(
                "serviceMain: start(args) end.  Main Completed=" + m_mainComplete + ", exitCode=" + m_mainExitCode);
            }
            return m_mainExitCode;
        }
    }
    
    /**
     * Restart.
     */
    public static void restart()
    {
        WrapperManager.restart();
    }
    
    /**
     * Shutdown.
     * 
     * @param exitCode the exit code
     */
    public static void shutdown(int exitCode)
    {
        if(WrapperManager.isControlledByNativeWrapper())
            WrapperManager.stop(exitCode);
        else
            System.exit(exitCode);
    }
    
    /**
     * Log event.
     * 
     * @param wrapperLogLevel the wrapper log level
     * @param message the message
     */
    public static void logEvent(int wrapperLogLevel, String message)
    {
        WrapperManager.log(wrapperLogLevel, message);
    }
    
    /**
     * Checks if is debug enabled.
     * 
     * @return true, if is debug enabled
     */
    public static boolean isDebugEnabled()
    {
        return WrapperManager.isDebugEnabled();
    }
    
    /**
     * Called when the application is shutting down.
     * 
     * @param exitCode the exit code
     * 
     * @return the int
     */
    public int stop(int exitCode)
    {
        if (WrapperManager.isDebugEnabled())
        {
            System.out.println("serviceMain: stop(" + exitCode + ")");
//            if (_monitor != null)
//            {
//                if (!WrapperManager.hasShutdownHookBeenTriggered())
//                {
//                    _monitor.close();
//                }
//                _monitor = null;
//            }
        }
        
        // Normally an application will be asked to shutdown here.  Standard Java applications do
        //  not have shutdown hooks, so do nothing here.  It will be as if the user hit CTRL-C to
        //  kill the application.
        return exitCode;
    }
    
    /**
     * Called whenever the native wrapper code traps a system control signal
     * against the Java process.  It is up to the callback to take any actions
     * necessary.  Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
     * WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
     * WRAPPER_CTRL_SHUTDOWN_EVENT
     * 
     * @param event the event
     */
    public void controlEvent(int event)
    {
        if (WrapperManager.isControlledByNativeWrapper())
        {
            if (WrapperManager.isDebugEnabled())
            {
                System.out.println("serviceMain: controlEvent(" + event + ") Ignored");
            }
            // Ignore the event as the native wrapper will handle it.
        }
        else
        {
            if (WrapperManager.isDebugEnabled())
            {
                System.out.println("serviceMain: controlEvent(" + event + ") Stopping");
            }
            
            // Not being run under a wrapper, so this isn't an NT service and should always exit.
            //  Handle the event here.
            WrapperManager.stop(0);
            // Will not get here.
        }
    }
    
  /*---------------------------------------------------------------
   * Methods
   *-------------------------------------------------------------*/
    /**
     * Displays application usage
     */
    private static void showUsage()
    {
        System.out.println();
        System.out.println("serviceMain Usage:");
        System.out.println("  java com.fluidops.coremgmt.common.servicedaemon.Main {app_class} [app_parameters]");
        System.out.println();
        System.out.println("where:");
        System.out.println("  app_class:      The fully qualified class name of the application to run.");
        System.out.println("  app_parameters: The parameters that would normally be passed to the");
        System.out.println("                  application.");
    }
    
    /**
     * Checks if is controlled by native wrapper.
     * 
     * @return true, if is controlled by native wrapper
     */
    public static boolean isControlledByNativeWrapper()
    {
        return WrapperManager.isControlledByNativeWrapper();
    }
    
  /*---------------------------------------------------------------
   * Main Method
   *-------------------------------------------------------------*/
    /**
   * Used to Wrapper enable a standard Java application.  This main
   * expects the first argument to be the class name of the application
   * to launch.  All remaining arguments will be wrapped into a new
   * argument list and passed to the main method of the specified
   * application.
   * 
   * @param args the args
   */
    public static void main(String args[])
    {
        // Get the class name of the application
        if (args.length < 1)
        {
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        
        // Look for the specified class by name
        Class<?> mainClass;
        try
        {
            mainClass = Class.forName(args[0]);
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("serviceMain: Unable to locate the class " + args[0] + ": " + e);
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        catch (LinkageError e)
        {
            System.out.println("serviceMain: Unable to locate the class " + args[0] + ": " + e);
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        
        // Look for the main method
        Method mainMethod;
        try
        {
            mainMethod = mainClass.getDeclaredMethod("main", new Class[]
            { String[].class });
        }
        catch (NoSuchMethodException e)
        {
            System.out.println("serviceMain: Unable to locate a static main method in class " + args[0] + ": " + e);
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        catch (SecurityException e)
        {
            System.out.println("serviceMain: Unable to locate a static main method in class " + args[0] + ": " + e);
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        
        // Make sure that the method is public and static
        int modifiers = mainMethod.getModifiers();
        if (!(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)))
        {
            System.out.println("serviceMain: The main method in class " + args[0] + " must be declared public and static.");
            showUsage();
            WrapperManager.stop(1);
            return; // Will not get here
        }
        
        // Build the application args array
        String[] appArgs = new String[args.length - 1];
        System.arraycopy(args, 1, appArgs, 0, appArgs.length);
        
        // Create the WrapperSimpleApp
        Main app = new Main(mainMethod);
        
        // Start the application.  If the JVM was launched from the native
        //  Wrapper then the application will wait for the native Wrapper to
        //  call the application's start method.  Otherwise the start method
        //  will be called immediately.
        WrapperManager.start(app, appArgs);
        
        // This thread ends, the WrapperManager will start the application after the Wrapper has
        //  been propperly initialized by calling the start method above.
    }
}
