/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.tests.util;

import com.novell.ldapchai.tests.TestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple java TcpProxy class.
 * <p/>
 * Used by Chai Tester to simulate multiple LDAP servers for failover testing.
 *
 * @author Jason D. Rivard
 */
public class TcpProxy {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final int TIMING_LOOP = 10;

    private static boolean doOutput = true;

    private static final int BACKLOG = 0;

    private static volatile int proxyCounter = 0;

    private InetSocketAddress listenInfo;
    private InetSocketAddress destinationInfo;

    private boolean running = false;
    private ServerThread serverThread;
    private Set<ClientThread> clientThreads = new HashSet<ClientThread>();
    private int proxyCount = 0;
    private volatile int connectionCounter = 0;

// -------------------------- STATIC METHODS --------------------------

    public static boolean isDoOutput()
    {
        return doOutput;
    }

    public static void setDoOutput(final boolean doOutput)
    {
        TcpProxy.doOutput = doOutput;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public TcpProxy(final int listenPort, final InetSocketAddress destinationInfo)
    {
        this(new InetSocketAddress(listenPort), destinationInfo);
    }

    public TcpProxy(final InetSocketAddress listenInfo, final InetSocketAddress destinationInfo)
    {
        this.proxyCount = proxyCounter++;
        this.listenInfo = listenInfo;
        this.destinationInfo = destinationInfo;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public InetSocketAddress getDestinationInfo()
    {
        return destinationInfo;
    }

    public InetSocketAddress getListenInfo()
    {
        return listenInfo;
    }

// -------------------------- OTHER METHODS --------------------------

    private void output(final CharSequence str)
    {
        if (doOutput) {
            System.out.println("TcpProxy[" + proxyCount + "]: " + str);
        }
    }

// -------------------------- INNER CLASSES --------------------------

    private class ServerThread implements Runnable {
        private ServerSocket serverSocket;

        private ServerThread()
                throws IOException
        {
            serverSocket = new ServerSocket(listenInfo.getPort(), BACKLOG, listenInfo.getAddress());
        }

        public void run()
        {
            output("begining proxy listen on " + serverSocket.getInetAddress().toString() + ", forwarding to " + destinationInfo.toString());
            while (running && serverSocket != null && !serverSocket.isClosed()) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    output("accepted connecton #" + connectionCounter + " from " + clientSocket.getInetAddress().toString());
                    final Socket destinationSocket = new Socket(destinationInfo.getAddress(), destinationInfo.getPort());
                    final ClientThread ct = new ClientThread(clientSocket, destinationSocket, connectionCounter);
                    new Thread(ct, "ClientThread #" + connectionCounter).start();
                    clientThreads.add(ct);
                } catch (Exception e) {
                    if (running) {
                        output("error during proxy listen: " + e.getMessage());
                    }
                }
                connectionCounter++;
            }
            output("closing proxy on " + serverSocket.getInetAddress().toString());
        }

        private void close()
                throws IOException
        {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }
    }

    private class ClientThread implements Runnable {
        private InputStream clientInput;
        private OutputStream clientOutput;

        private OutputStream serverOutput;
        private InputStream serverInput;

        private Socket clientSocket;
        private Socket serverSocket;

        private boolean running = true;
        private int connectionCounter;

        private byte[] byteBuffer = new byte[1024];

        private ClientThread(final Socket clientSocket, final Socket serverSocket, final int connectionCounter)
                throws IOException
        {
            this.clientSocket = clientSocket;
            this.serverSocket = serverSocket;

            clientInput = clientSocket.getInputStream();
            clientOutput = clientSocket.getOutputStream();

            serverInput = serverSocket.getInputStream();
            serverOutput = serverSocket.getOutputStream();
            this.connectionCounter = connectionCounter;
        }

        public void run()
        {
            while (running && clientSocket.isConnected() && serverSocket.isConnected()) {
                try {
                    copyData(clientInput, serverOutput);
                    copyData(serverInput, clientOutput);
                    TestHelper.pause(TIMING_LOOP);
                } catch (Exception e) {
                    this.close();
                }
            }
            output("closing connecton #" + connectionCounter);
        }

        private void close()
        {
            running = false;
            try {
                clientInput.close();
            } catch (Exception e) { /* doesn't matter */ }
            try {
                clientOutput.close();
            } catch (Exception e) { /* doesn't matter */ }
            try {
                serverInput.close();
            } catch (Exception e) { /* doesn't matter */ }
            try {
                serverOutput.close();
            } catch (Exception e) { /* doesn't matter */ }

            try {
                clientSocket.close();
            } catch (Exception e) { /* doesn't matter */ }
            try {
                serverSocket.close();
            } catch (Exception e) { /* doesn't matter */ }

            clientInput = null;
            clientOutput = null;
            serverInput = null;
            serverOutput = null;
        }

        private void copyData(final InputStream is, final OutputStream os)
                throws Exception
        {
            int counter = 0;
            while (is.available() > 0 && counter < 1000) {
                final int bytesRead = is.read(byteBuffer);
                if (bytesRead == -1) {
                    throw new Exception("input stream closed");
                }
                os.write(byteBuffer, 0, bytesRead);
                counter++;
            }
        }
    }

// --------------------------- main() method ---------------------------

    public static void main(final String[] args)
            throws IOException
    {
        //    final TcpProxy proxy0 = new TcpProxy(new InetSocketAddress("localhost",23),new InetSocketAddress("vert.synchro.net",23));
        final TcpProxy proxy1 = new TcpProxy(new InetSocketAddress(23), new InetSocketAddress("vert.synchro.net", 23));
        proxy1.start();
        proxy1.stop();
        proxy1.start();
        System.out.println("proxy started");
        TestHelper.pause(60 * 1000);
        proxy1.stop();
        System.out.println("proxy stopped");
        TestHelper.pause(5 * 1000);
        System.out.println("exiting");
    }

    public synchronized void start()
            throws IOException
    {
        if (running) {
            throw new IllegalStateException("already running");
        }

        if (serverThread == null) {
            running = true;
            serverThread = new ServerThread();
            new Thread(serverThread).start();
        }
    }

    public synchronized void stop()
            throws IOException
    {
        if (!running) {
            throw new IllegalStateException("already running");
        }

        running = false;

        if (serverThread != null) {
            serverThread.close();
        }

        serverThread = null;

        for (final ClientThread cThread : clientThreads) {
            cThread.close();
        }

        clientThreads.clear();
    }
}
