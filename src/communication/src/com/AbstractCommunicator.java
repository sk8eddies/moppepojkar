package com;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


/**
 * An abstracting class for the two different Communicators.
 * Contains basic functionality and utility.
 */
public abstract class AbstractCommunicator implements Communicator {

    protected final long UPDATE_INTERVAL = 10;
    protected final int port;
    protected Socket socket;
    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;
    protected Thread mainThread;
    private Queue<MopedStates> queuedMopedStates;
    private final ArrayList<CommunicationListener> listeners;

    /**
     * @param port Port for the socket to use.
     */
    protected AbstractCommunicator(int port) {
        this.port = port;
        listeners = new ArrayList<>();
        queuedMopedStates = new LinkedList<MopedStates>();
    }

    @Override
    public void setState(MopedStates state) {
        queuedMopedStates.add(state);
    }

    @Override
    public void addListener(CommunicationListener cl) {
        listeners.add(cl);
    }

    @Override
    public void start() {
        queuedMopedStates.clear();
        mainThread.start();
    }

    @Override
    public void stop() {
        mainThread.interrupt();
    }

    /**
     * Fetches and sends new information from the connected socket.
     */
    protected void update() {
        try {

            //Send all queued state changes through socket link
            while (queuedMopedStates.size() > 0) {
                outputStream.writeUTF(Integer.toString(queuedMopedStates.poll().toInt()));
            }

            // Get all state changes from other sender in socket link
            while (inputStream.available() > 0) {
                String input = inputStream.readUTF();
                int i = Integer.parseInt(input);
                notifyStateChange(MopedStates.parseInt(i));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Notifies all listeners that a connection has been established.
     */
    protected void notifyConnected() {
        for (CommunicationListener cl : listeners) {
            cl.onConnection();
        }
    }

    /**
     * Notifies all listeners that a state change from the sender has been received.
     */
    protected void notifyStateChange(MopedStates mopedState) {
        for (CommunicationListener cl : listeners) {
            cl.onStateChange(mopedState);
        }
    }

    /**
     * Closes the socket, nullifies it and nullifies the in/output-streams.
     */
    protected void clearConnection() {
        try {
            socket.close();
            socket = null;
            inputStream = null;
            outputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}