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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Rand;

public class XMLWikiReader extends DefaultHandler implements Runnable
{
	private static final Logger logger = Logger.getLogger(XMLWikiReader.class.getName());
	
    private final InputStream input;

    private final String namespace;

    private Page currentPage = null;

    private ParsingStep step = ParsingStep.NONE;

    private long startTime;

    private ConcurrentLinkedQueue<Page> queue;

    private ConcurrentLinkedQueue<Page> reuse;

    private final int max_queue_len;

    private List<WikiConsumer> consumers = new ArrayList<WikiConsumer>(5);

    private int consumer_mask = 0;

    private transient boolean isFinished;


    public XMLWikiReader(String filename, String namespace, int max_queue_len, int flags)
            throws FileNotFoundException
    {
        this.max_queue_len = (max_queue_len <= 0 ? 42 * 25 : max_queue_len);
        this.namespace = (namespace == null || namespace.isEmpty() ? EndpointImpl.api().getNamespaceService().defaultNamespace()
                : namespace);
        this.queue = new ConcurrentLinkedQueue<Page>();
        this.reuse = new ConcurrentLinkedQueue<Page>();

        File file = new File(filename);
        if (!file.canRead())
            throw new FileNotFoundException(filename);
        BufferedInputStream fin = new BufferedInputStream(new FileInputStream(
                filename), 20480);

        if (filename.endsWith(".bz2")) {
            try {
                fin.mark(3);
                if (fin.read() == 'B') {
                    if (fin.read() != 'Z')
                        fin.reset();
                }
                else {
                    fin.reset();
                }
            }
            catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            input = new CBZip2InputStream(fin);
        }
        else {
            input = fin;
        }
    }

    public Thread parse()
    {
        logger.info("Parsing begins...");
        /* now nobody can register anymore */
        consumers = Collections.unmodifiableList(consumers);

        /* fill up mask, e.g. */
        for (int i = 0; i < consumers.size(); i++) {
            consumer_mask |= 1 << i;
        }

        logger.debug("Consumer mask: "
                + Integer.toBinaryString(consumer_mask));

        Thread reader = new Thread(this);
        reader.setName(getClass().getName());
        reader.setPriority(Thread.NORM_PRIORITY + 2);
        reader.start();

       logger.debug("filling queue...   ");
        try {
            Thread.sleep(3000);
        }
        catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        logger.debug("done");
        
        for (WikiConsumer consumer : consumers) {
        	logger.debug("starting consumer: " + consumer.getClass());
            consumer.consume();
        }

        return reader;
    }

    @Override
    public void run()
    {
        // Parse the document
        try {
            XMLReader parser = org.xml.sax.helpers.XMLReaderFactory
                    .createXMLReader();
            // Register the content handler (part of DefaultHandler)
            parser.setContentHandler(this);
            parser.setErrorHandler(new MyErrorHandler());
            parser.parse(new InputSource(input));
            
            /* wait until queue is drained */
            while (!isFinished())
                Thread.sleep(200);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        finally {
            try {
                input.close();
            }
            catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }

        }
    }

    static class MyErrorHandler implements ErrorHandler
    {

        @Override
        public void error(SAXParseException e)
                throws SAXException
        {
            logger.error(e.getMessage(), e);
        }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException
        {
            logger.fatal(e.getMessage(), e);
        }

        @Override
        public void warning(SAXParseException e)
                throws SAXException
        {
            logger.warn(e.getMessage(), e);
        }

    }

    enum ParsingStep {
        PAGE, TITLE, TEXT, REDIRECT, NONE,
    }

    private long counter = 0;
    private long queue_full = 0;
    private long redirect_count = 0;
    private long file_count = 0;

    public void startDocument() throws SAXException
    {
        logger.info("document start");
        startTime = System.currentTimeMillis();
    }

    public void endDocument() throws SAXException
    {
        isFinished = true;
    }

    public void characters(char[] ch, int start, int end) throws SAXException
    {
        switch (step)
        {
        case TITLE:
            currentPage.title.append(ch, start, end);
            break;
        case TEXT:
            currentPage.text.append(ch, start, end);
            break;
        default: break;
        }
    }

    public void startElement(String namespaceURI, String localName,
            String rawName, Attributes atts) throws SAXException
    {
        try {
            step = ParsingStep.valueOf(localName.toUpperCase());
            
            switch (step) {
            case PAGE:
                currentPage = getEmptyPage();
                break;
                
            case REDIRECT:
                currentPage.isRedirect = true;
                break;
            default: break;	// do nothing
            }   
        }
        catch (Exception e) {
            /* thats fine */
        }
    }

    private Page getEmptyPage()
    {
        final Page page = reuse.poll();
        return page == null ? new Page() : page;
    }

    public void endElement(String namespaceURI, String localName, String rawName)
            throws SAXException
    {

        if (localName.equals("page")) {

            // The following replacement follows the procedure that maps
            // whitespace in page names to _ in URIs
            for (int i = 0; i < currentPage.title.length(); i++) {
                if (currentPage.title.charAt(i) == ' ') {
                    currentPage.title.setCharAt(i, '_');
                }
            }

            addQueue();
        }
        
        step = ParsingStep.NONE;
    }

    private void addQueue() throws SAXException
    {
        if (isFinished) {
            /* to abort the parser, throw an exception */
            throw new SAXException("Parsing aborted by user!");
        }
        while (queue.size() >= max_queue_len) {
            try {
                long sleep = 10 + Rand.getRandom().nextInt(20);
                Thread.sleep(sleep);
                queue_full ++;
            }
            catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        
        if (!currentPage.isRedirect && !currentPage.title.toString().startsWith("File:")) {
            if (counter++ % 1000 == 0) {
                float thru = (counter * 1000)
                        / (float) (System.currentTimeMillis() - startTime);
                System.out
                        .printf(
                                "imported [%s]: %d thru: %f Hz queue: %d queue full: %d redirect: %d files: %d%n ",
                                currentPage.title, counter, thru, queue.size(),
                                queue_full, redirect_count, file_count);
            }
            queue.add(currentPage);
        } else if (currentPage.isRedirect) {
            /* reusing that page */
            currentPage.reset();
            reuse.add(currentPage);
            redirect_count++;
        } else {
           
            /* reusing that page */
            currentPage.reset();
            reuse.add(currentPage);
            file_count++;
        }
        
        currentPage = null;
    }

    public boolean isFinished()
    {
        return isFinished && queue.isEmpty();
    }

    int addConsumer(WikiConsumer wikiConsumer)
    {
        int size = consumers.size();
        if (size < 31) {
            consumers.add(wikiConsumer);
            return size;
        }
        else {
            throw new IllegalArgumentException(
                    "To many consumers registered already!");
        }
    }

    Page peekQueue(int consumerNumber)
    {
        try {
            /*
             * look for unprocessed pages in the queue
             */
            int checked_count = 0;
            for (final Page check : queue) {
                checked_count++;
                if (!check.isProcessed(consumerNumber)) {
                    return check;
                }
            }
            long sleep = (long) (5 + queue.size() * 0.4);
            Thread.sleep(sleep);
            
            Page peek = queue.peek();
            logger.info(consumers.get(consumerNumber)
                    + " missed at "+(peek == null ? "/empty/" : peek.title)+" ["+counter+"], waits " + sleep + "ms queue: "
                    + queue.size());

        }
        catch (Exception e) {
            /* threading issues */
            logger.warn(e.getStackTrace());
        }

        return null;
    }

    public void processed(Page page, int consumerNumber)
    {
        page.processed(consumerNumber);
        if ((page.processed ^ consumer_mask) == 0) {
            queue.remove(page);
            page.reset();
            reuse.add(page);
        }
    }

    public String getNamespace()
    {
        return namespace;
    }

    public long getCounter()
    {
        return counter;
    }

    public void abort()
    {
        isFinished = true;
        
        for (WikiConsumer consumer : consumers) {
            try {
                consumer.join();
            }
            catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
