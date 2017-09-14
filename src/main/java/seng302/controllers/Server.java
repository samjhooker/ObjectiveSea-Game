package seng302.controllers;

import seng302.data.*;
import seng302.data.registration.RegistrationResponse;
import seng302.data.registration.RegistrationResponseStatus;
import seng302.data.registration.RegistrationType;
import seng302.models.*;

import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.util.*;

import static seng302.data.AC35StreamXMLMessage.BOAT_XML_MESSAGE;
import static seng302.data.AC35StreamXMLMessage.RACE_XML_MESSAGE;
import static seng302.data.AC35StreamXMLMessage.REGATTA_XML_MESSAGE;
import static seng302.data.registration.RegistrationResponseStatus.OUT_OF_SLOTS;
import static seng302.data.registration.RegistrationResponseStatus.RACE_UNAVAILABLE;
import static seng302.data.registration.RegistrationResponseStatus.SPECTATOR_SUCCESS;

/**
 * Created by mjt169 on 18/07/17.
 */
public class Server implements Runnable, Observer {

    private final double SECONDS_PER_UPDATE = 0.2;
    private final int MAX_SPECTATORS = 100; //mostly because our boats sourceIDs start at 101

    private Map<AC35StreamXMLMessage, Integer> xmlSequenceNumber = new HashMap<>();
    private Map<Boat, Integer> boatSequenceNumbers = new HashMap<>();
    private Map<Boat, Integer> lastMarkRoundingSent = new HashMap<>();
    private int nextViewerID = 0;

    private RaceUpdater raceUpdater;
    private Thread raceUpdaterThread;
    private ConnectionManager connectionManager;
    private ServerPacketBuilder packetBuilder;
    private CollisionManager collisionManager;
    private ServerOptions options;



    public Server(ServerOptions options) throws IOException {
        this.options = options;
        packetBuilder = new ServerPacketBuilder();
        connectionManager = new ConnectionManager(options.getPort());
        connectionManager.addObserver(this);
        setupNewRaceUpdater(options);
    }

    /**
     * Initializes a new raceUpdater with settings provided in options
     * @param options for the race
     */
    private void setupNewRaceUpdater(ServerOptions options) {
        raceUpdater = new RaceUpdater(options.getRaceXML());
        if(options.isTutorial()) raceUpdater.skipPrerace();
        raceUpdater.setScaleFactor(options.getSpeedScale());
        if(options.getAIDifficulty() != AIDifficulty.NO_AI) raceUpdater.addAICompetitor(options.getAIDifficulty());
        raceUpdaterThread = new Thread(raceUpdater);
        raceUpdaterThread.setName("Race Updater");
        collisionManager = raceUpdater.getCollisionManager();
    }

    /**
     * Initializes the sequence numbers for the boats and xml messages
     * @throws IOException
     */
    private void initialize() throws IOException, NullPointerException  {
        xmlSequenceNumber.put(REGATTA_XML_MESSAGE, 0);
        xmlSequenceNumber.put(RACE_XML_MESSAGE, 0);
        xmlSequenceNumber.put(BOAT_XML_MESSAGE, 0);
        for (Boat boat: raceUpdater.getRace().getCompetitors()){
            boatSequenceNumbers.put(boat, boat.getId());
            lastMarkRoundingSent.put(boat, -1);
        }
    }
    /**
     * Sends all the data to the socket while the boats have not all finished.
     */
    @Override
    public void run() throws NullPointerException {
        Integer timesRun = 0;
        while (options.alwaysRerun() || timesRun < options.getNumRacesToRun()) {
            setupNewRaceUpdater(options);
            System.out.println("Server: Ready to Run New Race");
            try {
                initialize();
                sendInitialRaceMessages();
                Thread managerThread = new Thread(connectionManager);
                managerThread.setName("Connection Manager");
                managerThread.start();
                while (!raceUpdater.raceHasEnded()) {
                    if (!raceUpdater.getRace().getCompetitors().isEmpty()) {
                        sendRaceUpdates();
                    }
                    Thread.sleep((long) (SECONDS_PER_UPDATE * 1000 / options.getSpeedScale()));
                }
                sendRaceUpdates(); //send one last message block with ending data
                connectionManager.closeClientConnections();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            timesRun++;
        }
        System.out.println("Server: Shutting Down");
        connectionManager.closeAllConnections();
    }

    /**
     * Sends a header, body then generates and sends a CRC for that header and body
     * @param packet the packet to send
     * @throws IOException
     */
    private void sendPacket(byte[] packet) {
        connectionManager.sendToClients(packet);
    }

    /**
     * Exit out of run
     */
    public void stop(){
        raceUpdater.getRace().updateRaceStatus(RaceStatus.TERMINATED);
    }

    /**
     * Sends the XML messages when the client has connected
     */
    private void sendInitialRaceMessages() {
        sendXmlMessage(RACE_XML_MESSAGE, options.getRaceXML());
        sendXmlMessage(BOAT_XML_MESSAGE, "Boat.xml");
        sendXmlMessage(REGATTA_XML_MESSAGE, "Regatta.xml");
    }

    private void sendAllBoatStates() {
        for(Boat boat : raceUpdater.getRace().getCompetitors()){
            sendBoatStateMessage(boat);
        }
    }

    private void sendBoatStateMessage(Boat boat) {
        sendPacket(packetBuilder.createBoatStateMessagePacket(boat));
    }

    /**
     * Sends Race Status, Boat Location and Mark Rounding messages that are currently necessary
     * @throws IOException
     */
    private void sendRaceUpdates() throws IOException {
        try {
            byte[] raceUpdateMessage = packetBuilder.createRaceUpdateMessage(raceUpdater.getRace());
            sendPacket(raceUpdateMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendBoatMessagesForAllBoats();
        sendYachtEventMessages();
    }

    private void sendYachtEventMessages() throws IOException {
        for (Collision collision : collisionManager.getCollisions()) {
            for (Integer boatId : collision.getInvolvedBoats()) {
                Boat boat = raceUpdater.getRace().getBoatById(boatId);
                sendYachtEventMessage(boat, raceUpdater.getRace(), collision.getIncidentId(), YachtEventCode.COLLISION);
                if (collision.isOutOfBounds()){
                    sendYachtEventMessage(boat, raceUpdater.getRace(), collision.getIncidentId(), YachtEventCode.OUT_OF_BOUNDS);
                }else{
                    if (collision.boatIsAtFault(boatId)) {
                        sendYachtEventMessage(boat, raceUpdater.getRace(), collision.getIncidentId(), YachtEventCode.COLLISION_PENALTY);
                    }
                    if(collision.getInvolvedBoats().size() == 1) {
                        sendYachtEventMessage(boat, raceUpdater.getRace(), collision.getIncidentId(), YachtEventCode.COLLISION_MARK);
                    }
                }
                sendBoatStateMessage(boat);
            }
            collisionManager.removeCollision(collision);
        }
    }

    private void sendBoatMessagesForAllBoats() throws IOException {
        for (Boat boat : raceUpdater.getRace().getCompetitors()) {
            if (!boat.isFinished()) {
                sendBoatMessages(boat);
            }
        }
    }

    /**
     * Sends a Boat Location Message and possible a Mark Rounding message for a boat
     * @param boat the boat to send messages for
     * @throws IOException
     */
    private void sendBoatMessages(Boat boat) throws IOException {
        Integer currentSequenceNumber = boatSequenceNumbers.get(boat);
        if (currentSequenceNumber != null) { //check required as a race condition can sometimes cause a NullPointerException
            boatSequenceNumbers.put(boat, currentSequenceNumber + 1);

            sendPacket(packetBuilder.createBoatLocationMessage(boat, raceUpdater.getRace(), currentSequenceNumber));
            if (lastMarkRoundingSent.get(boat) != boat.getLastRoundedMarkIndex()) {
                lastMarkRoundingSent.put(boat, boat.getLastRoundedMarkIndex());
                sendPacket(packetBuilder.createMarkRoundingMessage(boat, raceUpdater.getRace()));
            }
        }
    }

    /**
     * sends a Yacht Event message
     * @param boat
     * @param race
     * @throws IOException
     */
    private void sendYachtEventMessage(Boat boat, Race race, int incidentID, YachtEventCode eventCode) throws IOException {
        sendPacket(packetBuilder.createYachtEventMessage(boat, race, incidentID, eventCode));
    }

    /**
     * Sends an xml message type to the socket including the header, body and CRC
     * @param type subtype of the xml message
     * @param fileName name of the file to send
     */
    private void sendXmlMessage(AC35StreamXMLMessage type, String fileName) {
        int sequenceNo = xmlSequenceNumber.get(type) + 1;
        xmlSequenceNumber.put(type, sequenceNo);
        byte[] packet = packetBuilder.buildXmlMessage(type, fileName, sequenceNo, raceUpdater.getRace(), options.getRaceXML());
        connectionManager.setXmlMessage(type, packet);
    }

    /**
     * Starts a new server listener on new thread for which listens to a client
     * @param socket the socket for the client
     */
    private void startServerListener(Socket socket){
        ServerListener serverListener = new ServerListener(socket);
        serverListener.setRace(raceUpdater.getRace());
        Thread serverListenerThread = new Thread(serverListener);
        serverListenerThread.setName("Server Listener");
        serverListenerThread.start();
        serverListener.addObserver(this);
    }

    /**
     * Method that gets called when Server is notified as an observer
     * If the observable is a ConnectionManager then a new client has connected and a server listener
     * is started for the client
     * If the observable is a ServerListener then a registration message is received
     * @param observable The observable either a ConnectionManager or ServerListener
     * @param arg A Socket or Boat ID if observable is a ConnectionManager else it is the registration type of the client
     */
    @Override
    public void update(Observable observable, Object arg) {
        if (observable.equals(connectionManager)) {
            if(arg instanceof Socket){
                startServerListener((Socket) arg);
            } else {
                setBoatToDNF((int) arg);
            }
        } else if(observable instanceof ServerListener){
            manageRegistration((ServerListener) observable, (RegistrationType) arg);
        }
    }

    /**
     * Deal with the different types of registrations clients can attempt to connect with
     * Currently ignores GHOST or TUTORIAL connection attempts.
     * @param serverListener the serverListener with the clients socket
     * @param registrationType the type of registration being attempted
     */
    private void manageRegistration(ServerListener serverListener, RegistrationType registrationType) {
        switch (registrationType) {
            case PLAYER:
                addClientToRace(serverListener);
                break;
            case SPECTATOR:
                addSpectatorToRace(serverListener);
                break;
            case GHOST:
                System.out.println("Server: Client attempted to connect as ghost, ignoring.");
                break;
            case TUTORIAL:
                System.out.println("Server: Client attempted to connect as control tutorial, ignoring.");
                break;
        }
    }

    /**
     * Adds a competing client to the race model and sends new xml messages out to all clients
     * @param serverListener the listener for the client
     */
    private void addClientToRace(ServerListener serverListener){
        int newId = raceUpdater.addCompetitor();
        boolean success = newId != -1;
        byte[] packet;
        if (success) {
            Boat boat = raceUpdater.getRace().getBoatById(newId);
            boatSequenceNumbers.put(boat, newId);
            lastMarkRoundingSent.put(boat, -1);
            packet = packetBuilder.createRegistrationResponsePacket(newId, RegistrationResponseStatus.PLAYER_SUCCESS);
            if (!raceUpdaterThread.isAlive()){
                int numCompetitors = raceUpdater.getRace().getCompetitors().size();
                if (numCompetitors >= options.getMinParticipants()) {
                    raceUpdaterThread.start();
                }
            }
        } else {
            packet = packetBuilder.createRegistrationResponsePacket(newId, RegistrationResponseStatus.OUT_OF_SLOTS);
        }
        connectionManager.addConnection(newId, serverListener.getSocket());
        serverListener.setClientId(newId);
        connectionManager.sendToClient(newId, packet);

        if(success){
            sendXmlMessage(RACE_XML_MESSAGE, options.getRaceXML());
            sendAllBoatStates();
        }
    }

    /** Responds to a Spectator requesting to join
     * Can fail due to the race not being started, otherwise we let them join
     * @param serverListener
     */
    private void addSpectatorToRace(ServerListener serverListener) {
        RegistrationResponseStatus response = SPECTATOR_SUCCESS;
        if (!raceUpdaterThread.isAlive()) {
            response = RACE_UNAVAILABLE;
        } else if (nextViewerID >= MAX_SPECTATORS) {
            response = OUT_OF_SLOTS;
        }
        byte[] packet = packetBuilder.createRegistrationResponsePacket(0, response);
        connectionManager.addConnection(nextViewerID, serverListener.getSocket());
        connectionManager.sendToClient(nextViewerID, packet);
        if (response.equals(SPECTATOR_SUCCESS)) {
            sendAllBoatStates();
            nextViewerID++;
        } else {
            connectionManager.removeConnection(nextViewerID);
        }
    }

    public void initiateServerDisconnect() {
        connectionManager.closeAllConnections();
        raceUpdater.stopRunning();
    }

    private void setBoatToDNF(int arg){
        for(Boat boat : raceUpdater.getRace().getCompetitors()){
            if(boat.getId().equals(arg)){
                if(!boat.getStatus().equals(BoatStatus.FINISHED)){
                    boat.setStatus(BoatStatus.DNF);
                    boat.changeSails();
                }
            }
        }
    }
}
