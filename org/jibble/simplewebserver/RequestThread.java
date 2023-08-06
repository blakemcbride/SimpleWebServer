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
public class RequestThread extends Thread {

    private File _rootDir;
    private Socket _socket;
    private BufferedReader in;

    RequestThread(Socket socket, File rootDir) {
        _socket = socket;
        _rootDir = rootDir;
    }

    private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException {
        out.write(("HTTP/1.1 " + code + " OK\r\n" +
                "Date: " + new Date().toString() + "\r\n" +
                "Cache-Control: private\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Server: SimpleWebServer/1.1\r\n" +
                "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
                ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
                "Last-modified: " + new Date(lastModified).toString() + "\r\n" +
                "\r\n").getBytes());
    }

    private static void sendLocationHeader(BufferedOutputStream out, int code, String location) throws IOException {
        out.write(("HTTP/1.1 " + code + " OK\r\n" +
                "Cache-Control: private\r\n" +
                "Location: " + location + "\r\n" +
                "Content-Length: 0\r\n" +
                "Server: SimpleWebServer/1.1\r\n" +
                "Date: " + new Date().toString() + "\r\n" +
                "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
                "\r\n").getBytes());
    }

    private void sendError(BufferedOutputStream out, int code, String message) throws IOException {
        message = message + "<hr>" + SimpleWebServer.VERSION;
        sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
        out.write(message.getBytes());
        out.flush();
        out.close();
        in.close();
    }

    public void run() {
        InputStream reader = null;
        try {
            _socket.setSoTimeout(30000);
            in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(_socket.getOutputStream());

            String request = in.readLine();
            if (request == null || !request.startsWith("GET ") || !(request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                // Invalid request type (no "GET")
                sendError(out, 500, "Invalid method: " + (request == null ? "" : request));
                return;
            }
            String path = request.substring(4, request.length() - 9);
            int idx = path.indexOf('?');
            if (idx > 0)
                path = path.substring(0, idx);
            File file = new File(_rootDir, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();

            if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    sendLocationHeader(out, 302, path + "/");
                    out.flush();
                    out.close();
                    in.close();
                    return;
                }
                // Check to see if there is an index file in the directory.
                File indexFile = new File(file, "index.html");
                if (indexFile.exists() && !indexFile.isDirectory())
                    file = indexFile;
            }

            if (!file.toString().startsWith(_rootDir.toString())) {
                // Uh-oh, it looks like some lamer is trying to take a peek
                // outside of our web root directory.
                sendError(out, 403, "Permission denied.");
            } else if (!file.exists()) {
                // The file was not found.
                sendError(out, 404, "File \"" + path + "\" not found.");
            } else if (file.isDirectory()) {
                // print directory listing
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                File[] files = file.listFiles();
                sendHeader(out, 200, "text/html", -1, System.currentTimeMillis());
                String title = "Index of " + path;
                out.write(("<html><head><title>" + title + "</title></head><body><h3>Index of " + path + "</h3><p>\n").getBytes());
                for (File value : files) {
                    file = value;
                    String filename = file.getName();
                    String description = "";
                    if (file.isDirectory()) {
                        description = "&lt;DIR&gt;";
                    }
                    out.write(("<a href=\"" + path + filename + "\">" + filename + "</a> " + description + "<br>\n").getBytes());
                }
                out.write(("</p><hr><p>" + SimpleWebServer.VERSION + "</p></body><html>").getBytes());
            } else {
                reader = new BufferedInputStream(new FileInputStream(file));

                String contentType = SimpleWebServer.MIME_TYPES.get(SimpleWebServer.getExtension(file));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                sendHeader(out, 200, contentType, file.length(), file.lastModified());

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                reader.close();
            }
            out.flush();
            out.close();
            in.close();
        } catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception anye) {
                    // Do nothing.
                }
            }
        }
    }

}
