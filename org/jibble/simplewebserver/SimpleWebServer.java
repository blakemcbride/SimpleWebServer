/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of Mini Wegb Server / SimpleWebServer.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ServerSideScriptEngine.java,v 1.4 2004/02/01 13:37:35 pjm2 Exp $

*/

package org.jibble.simplewebserver;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Copyright Paul Mutton
 * http://www.jibble.org/
 *
 */
public class SimpleWebServer extends Thread {

    static final String VERSION = "SimpleWebServer  http://www.jibble.org/";
    static final Hashtable<String, String> MIME_TYPES = new Hashtable<String, String>();
    
    static {
        String image = "image/";
        MIME_TYPES.put(".gif", image + "gif");
        MIME_TYPES.put(".jpg", image + "jpeg");
        MIME_TYPES.put(".jpeg", image + "jpeg");
        MIME_TYPES.put(".png", image + "png");
        MIME_TYPES.put(".bmp", image + "bmp");
        MIME_TYPES.put(".svg", image + "svg+xml");
        MIME_TYPES.put(".ico", image + "x-icon");
        MIME_TYPES.put(".tif", image + "tiff");
        MIME_TYPES.put(".tiff", image + "tiff");
        MIME_TYPES.put(".webp", image + "webp");
        String text = "text/";
        MIME_TYPES.put(".html", text + "html");
        MIME_TYPES.put(".htm", text + "html");
        MIME_TYPES.put(".css", text + "css");
        MIME_TYPES.put(".txt", text + "plain");
        MIME_TYPES.put(".csv", text + "csv");
        MIME_TYPES.put(".ics", text + "calendar");
        String application = "application/";
        MIME_TYPES.put(".js", application + "javascript");
        MIME_TYPES.put(".json", application + "json");
        MIME_TYPES.put(".pdf", application + "pdf");
        MIME_TYPES.put(".azw", application + "vnd.amazon.ebook");
        MIME_TYPES.put(".bin", application + "octet-stream");
        MIME_TYPES.put(".doc", application + "msword");
        MIME_TYPES.put(".docx", application + "vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put(".epub", application + "epub+zip");
        MIME_TYPES.put(".jar", application + "java-archive");
        MIME_TYPES.put(".odp", application + "vnd.oasis.opendocument.presentation");
        MIME_TYPES.put(".ods", application + "vnd.oasis.opendocument.spreadsheet");
        MIME_TYPES.put(".odt", application + "vnd.oasis.opendocument.text");
        MIME_TYPES.put(".ogx", application + "ogg");
        MIME_TYPES.put(".ppt", application + "vnd.ms-powerpoint");
        MIME_TYPES.put(".pptx", application + "vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPES.put(".rtf", application + "rtf");
        MIME_TYPES.put(".sh", application + "x-sh");
        MIME_TYPES.put(".swf", application + "x-shockwave-flash");
        MIME_TYPES.put(".tar", application + "x-tar");
        MIME_TYPES.put(".xhtml", application + "xhtml+xml");
        MIME_TYPES.put(".xml", application + "xml");
        String audio = "audio/";
        MIME_TYPES.put(".aac", audio + "aac");
        MIME_TYPES.put(".midi", audio + "midi");
        MIME_TYPES.put(".mid", audio + "midi");
        MIME_TYPES.put(".ogg", audio + "ogg");
        MIME_TYPES.put(".oga", audio + "ogg");
        MIME_TYPES.put(".wav", audio + "wav");
        MIME_TYPES.put(".weba", audio + "webm");
        String video = "video/";
        MIME_TYPES.put(".avi", video + "x-msvideo");
        MIME_TYPES.put(".mpeg", video + "mpeg");
        MIME_TYPES.put(".mpg", video + "mpeg");
        MIME_TYPES.put(".ogv", video + "ogg");
        MIME_TYPES.put(".webm", video + "webm");
    }
    
    private SimpleWebServer(File rootDir, int port) throws IOException {
        _rootDir = rootDir.getCanonicalFile();
        if (!_rootDir.isDirectory()) {
            throw new IOException("Not a directory.");
        }
        _serverSocket = new ServerSocket(port);
        start();
    }
    
    public void run() {
        while (_running) {
            try {
                Socket socket = _serverSocket.accept();
                RequestThread requestThread = new RequestThread(socket, _rootDir);
                requestThread.start();
            }
            catch (IOException e) {
                System.exit(1);
            }
        }
    }
    
    // Work out the filename extension.  If there isn't one, we keep
    // it as the empty string ("").
    static String getExtension(java.io.File file) {
        String extension = "";
        String filename = file.getName();
        int dotPos = filename.lastIndexOf(".");
        if (dotPos >= 0) {
            extension = filename.substring(dotPos);
        }
        return extension.toLowerCase();
    }

    private static void help(int port) {
        System.err.println("Usage: java -jar SimpleWebServer.jar [-d root-directory] [-p port-number]");
        System.err.println("Default = current directory on port " + port);
        System.exit(-1);
    }
    
    public static void main(String[] args) {
        int port = 8000;
        String path = ".";
        int i = 0;
        while (args.length-i >= 2)
            if (args[i].equals("-p")) {
                port = Integer.parseInt(args[i+1]);
                i += 2;
            } else if (args[i].equals("-d")) {
                path = args[i+1];
                i += 2;
            } else
                help(port);
        if (i != args.length)
            help(port);

        System.out.println("Serving " + (new File(path)).getAbsolutePath() + " on URL http://localhost:" + port);
        System.out.println("Use ^C to exit.");
        try {
            SimpleWebServer server = new SimpleWebServer(new File(path), port);
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }
    
    private File _rootDir;
    private ServerSocket _serverSocket;
    private boolean _running = true;

}
