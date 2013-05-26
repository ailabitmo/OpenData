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

package com.fluidops.iwb.api.wiki;


import org.apache.log4j.Logger;

public abstract class WikiConsumer implements Runnable
{
    private static final Logger logger = Logger.getLogger(WikiConsumer.class.getName());

    protected final XMLWikiReader reader;
    private final int consumerNumber;
    private final int prioDiff;
    private Thread thread;
    
    public WikiConsumer(XMLWikiReader reader) {
        this(reader, 0);
    }
    public WikiConsumer(XMLWikiReader reader, int prioDiff) {
        this.reader = reader;
        this.prioDiff = prioDiff;
        consumerNumber = reader.addConsumer(this);
    }
    
    void consume() {
        thread = new Thread(this);
        thread.setPriority(Thread.NORM_PRIORITY+prioDiff);
        thread.setName(getClass().getName());
        thread.start();
    }
    
    public void run() {
        
        while (!reader.isFinished() ) {
            Page page = reader.peekQueue(consumerNumber);
            if (page != null) {
                perform(page);
                reader.processed(page, consumerNumber);
            }
        }
        try {
            logger.debug(getClass().getCanonicalName()+": finalizing ...");
            finish();
            logger.debug("done");
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
   
    void join() throws InterruptedException {
        thread.join();
    }
    
    protected abstract void finish() throws Exception;
    protected abstract void perform(Page page);
}
