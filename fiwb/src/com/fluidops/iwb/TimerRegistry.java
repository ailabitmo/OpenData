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

package com.fluidops.iwb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import org.apache.log4j.Logger;

import com.fluidops.iwb.api.CommunicationService;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.util.Singleton;

/**
 * Registry for IWB timers with shutdown functionality.
 * 
 * @author michaelschmidt, tm
 */
public class TimerRegistry 
{
    private static final Logger logger = Logger.getLogger(TimerRegistry.class.getName());
    
	private static Singleton<TimerRegistry> instance = new Singleton<TimerRegistry>() 
	{
		protected TimerRegistry createInstance() throws Exception 
		{ 
			return new TimerRegistry(); 
		}
	};
	
	Timer providerServiceTimer;
	
	// there might be several communication services
	Collection<Timer> communicationServiceTimers;

	/**
	 * Singleton Constructor
	 */
	private TimerRegistry()
	{
		communicationServiceTimers = new HashSet<Timer>();
	}
	
	/**
	 * instance getter
	 */
	public static TimerRegistry getInstance()
	{
		return instance.instance();
	}

	public void shutdownTimers() throws Exception
	{
		// ATTENTION: order matters !!!
		
		// cancel provider timer (no need to finish pending runs, runs in
		// progress will not result in any corruption)
		logger.info("Shutting down provider service timer");
		if (providerServiceTimer!=null)
		{
			providerServiceTimer.cancel(); // stop queueing of new providers

			// stop running provider
			for (AbstractFlexProvider provider : EndpointImpl.api().getProviderService().getProviders())
			{
				if (provider==null || provider.running==null || !provider.running)
					continue;
				
				// estimate expected waiting time based on previous run
				Long waitingTime = provider.lastDuration;
				if (waitingTime==null || waitingTime<=0)
					waitingTime = Long.valueOf(60000); // default: wait one minute
				else
					waitingTime = waitingTime * 2; // tolerance
				
				logger.info("Waiting for provider " + provider.providerID + " to finish for at most  " + (waitingTime/1000) + " seconds");
				long curWaitingTime = 0;
				while (curWaitingTime<waitingTime && provider.running)
				{
					Thread.sleep(1000);
					curWaitingTime += 1000;
				}
				
				if (provider.running)
					logger.warn("Could not regularly stop provider " + provider.providerID + "...");
			}
		}

		// cancel communication service timers and flush request queue
		logger.info("Shutting down communication service timer");
		for (Timer t : communicationServiceTimers)
			t.cancel();
		int i = 1;
		for (CommunicationService cs : EndpointImpl.api().getAllCommunicationServices())
		{
			logger.info("-> shutting down communication service " + i++);
			cs.handlePendingRequests();
		}
	}

	public void registerProviderServiceTimer(Timer providerServiceTimer) 
	{
		this.providerServiceTimer = providerServiceTimer;
	}
	
	public void registerCommunicationServiceTimer(Timer communicationServiceTimer) 
	{
		communicationServiceTimers.add(communicationServiceTimer);
	}
}
