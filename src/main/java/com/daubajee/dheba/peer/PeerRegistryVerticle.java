package com.daubajee.dheba.peer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.Topic;
import com.daubajee.dheba.peer.msg.GetPeerList;
import com.daubajee.dheba.peer.msg.HandShake;
import com.daubajee.dheba.peer.msg.PeerList;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class PeerRegistryVerticle extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(PeerRegistryVerticle.class);

    private Config config;

    private EventBus eventBus;

    private Map<String, Peer> registry = new HashMap<>();

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();

        config = new Config(vertx);

        vertx.setTimer(1000, this::initPeerList);

        vertx.setPeriodic(1000, this::cycle);

        eventBus.consumer(Topic.PEER_REGISTRY, this::onPeerRegistryMsg);

        eventBus.consumer(Topic.REMOTE_PEER_EVENTS, this::onRemotePeerEvent);

    }

    private void initPeerList(Long tick) {

        Set<AddressPort> peerSeeds = config.getPeerSeeds();

        peerSeeds.stream().forEach(addrPort -> {
            PeerList peerList = new PeerList(Arrays.asList(addrPort.toString()));
            PeerRegistryMessage registryMsg = new PeerRegistryMessage(PeerRegistryMessage.LIST, peerList.toJson());
            eventBus.publish(Topic.PEER_REGISTRY, registryMsg.toJson());
        });
        
    }

    private void cycle(Long tick) {
        LOGGER.info("Running cycle");

        addNewRemotePeer();
        
        askPeerList();
    }

    private void addNewRemotePeer() {
        long activeCount = registry
            .values()
            .stream()
            .filter(peer -> peer.isOutgoing() && peer.isActive())
            .count();

        if (activeCount <= config.getMaxPeerConnections()) {
        	registry
            	.values()
            	.stream()
            	.filter(peer -> peer.isOutgoingUnconnected())
            	.limit(1)
            	.forEach(peer -> {
            		String address = peer.getAddress();
            		int port = peer.getOutgoingPort();
            		peer.setActiveNow();
            		RemotePeerEvent event = new RemotePeerEvent(address, port, RemotePeerEvent.NEW_PEER);
            		eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
            	});
        }
    }

    private void askPeerList() {
        long peerCount = registry.values().stream().map(peer -> peer.isOutgoing()).count();
        if (peerCount < 100) {
            registry.values()
                .stream()
                .filter(peer -> peer.isOutgoing() && peer.isActive() && peer.hasHandshaked()
                            && peer.canBeAttempted())
                .forEach(peer -> {
                    String cmdTopic = Topic.getRemotePeerCommandTopic(peer.getAddress(), peer.getOutgoingPort());
                    GetPeerList getPeerList = new GetPeerList(100, Collections.emptyList());
                    Command cmd = new Command(Command.ASK_PEER_LIST, getPeerList.toJson());
                    eventBus.publish(cmdTopic, cmd.toJson());
                    peer.setActiveNow();
                });
        }
    }

    private void debugRegistry() {
        Collection<Peer> peers = registry.values();
        String peerStr = peers.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
        LOGGER.info("Registry : " + peerStr);
        PeerList list = getList(new JsonObject());
        LOGGER.info("PeerList : " + list.getPeers());
    }

    private void onPeerRegistryMsg(Message<JsonObject> msg) {

        JsonObject body = msg.body();
        PeerRegistryMessage registryMessage = PeerRegistryMessage.from(body);
        if (!registryMessage.isValid()) {
            LOGGER.warn("{} message invalid content: {}", PeerRegistryMessage.class.getSimpleName(), body);
            return;
        }
        
        String type = registryMessage.getType();
        LOGGER.info("Message of type {} received on {}", type, Topic.PEER_REGISTRY);

        switch (type) {
            case PeerRegistryMessage.GET_LIST :
                PeerList plist = getList(registryMessage.getContent());
                JsonObject reply = new PeerRegistryMessage(PeerRegistryMessage.LIST, plist.toJson()).toJson();
                msg.reply(reply);
                break;
            case PeerRegistryMessage.LIST :
                PeerList list = PeerList.from(registryMessage.getContent());
                createEntry(list);
                break;

            default :
                break;
        }
    }

    private void onRemotePeerEvent(Message<JsonObject> msg) {
        JsonObject body = msg.body();

        RemotePeerEvent remotePeerEvent = RemotePeerEvent.from(body);

        if (!remotePeerEvent.isValid()) {
            LOGGER.warn("Invalid {} event : {}", Topic.REMOTE_PEER_EVENTS, remotePeerEvent);
            return;
        }
        LOGGER.info("Message of type {} received on {}", remotePeerEvent.getType(), Topic.REMOTE_PEER_EVENTS);

        String type = remotePeerEvent.getType();
        switch (type) {
            case RemotePeerEvent.CONNECTED :
                createEntry(remotePeerEvent);
                break;
            case RemotePeerEvent.DISCONNECTED :
                updateEntryInstant(remotePeerEvent);
                break;

            case RemotePeerEvent.HANDSHAKED :
                updateRegistryOnHandShake(remotePeerEvent);
                break;

            default :
                break;
        }

    }

    private void createEntry(PeerList list) {

        long nbPeers = list.getPeers()
            .stream()
            .filter(adrPortStr -> !isSelfAddress(adrPortStr))
            .mapToInt(adrPortStr -> {
                Optional<AddressPort> op = AddressPort.from(adrPortStr);
                if (!op.isPresent()) {
                    LOGGER.error("Invalid address:pot {}", adrPortStr);
                    return 0;
                }
                AddressPort addressPort = op.get();
                String address = addressPort.getAddress();
                Peer peer = registry.computeIfAbsent(address, k -> new Peer(k));
                peer.setOutgoingPort(addressPort.getPort());
                return 1;
            })
            .count();
        LOGGER.info("{} peers added to internal registry", nbPeers);
        debugRegistry();
    }

    private void createEntry(RemotePeerEvent remotePeerEvent) {
        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        Peer peer = registry.computeIfAbsent(remoteHostAddress, k -> new Peer(remoteHostAddress));
        peer.setIncomingPort(remotePeerEvent.getRemoteHostPort());
        debugRegistry();
    }

    private boolean isSelfAddress(String adrPortStr) {
        int p2pPort = config.getP2PPort();
        boolean present = registry.values()
            .stream()
            .filter(peer -> peer.hasHandshaked())
            .map(peer -> peer.getHandshake().getAddrYou())
            .map(selfAddrStr -> AddressPort.from(selfAddrStr).get())
            .map(selfAddr -> new AddressPort(selfAddr.getAddress(), p2pPort))
            .anyMatch(selfAddr -> selfAddr.toString().equals(adrPortStr));
        if (present) {
            LOGGER.info("{} is local address", adrPortStr);
        } else {
            LOGGER.info("{} is NOT local address", adrPortStr);
        }
        return present;
    }

    private void updateEntryInstant(RemotePeerEvent remotePeerEvent) {
        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        Peer peer = registry.get(remoteHostAddress);
        if (peer == null) {
            return;
        }
        peer.deactivate();
        debugRegistry();
    }

    private void updateRegistryOnHandShake(RemotePeerEvent remotePeerEvent) {
        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        Peer peer = registry.get(remoteHostAddress);
        if (peer == null) {
            LOGGER.warn("No Peer with address {} exists in internal registry!", remoteHostAddress);
            return;
        }
        peer.setActiveNow();
        HandShake handshake = HandShake.fromJson(remotePeerEvent.getContent());
        if (handshake.isValid()) {
            // change the remote hostname in the Handshake with the hostname in
            // the RemotePeerEvent object as this is the address this node sees the
            // remote Peer
            Optional<AddressPort> containerAdrPort = AddressPort.from(handshake.getAddrMe());
            containerAdrPort.ifPresent(adrPort -> {
            	Integer port = containerAdrPort.get().getPort();
            	handshake.setAddrMe(new AddressPort(remoteHostAddress, port).toString());
            	peer.setHandshake(handshake);
            });
        } else {
        	LOGGER.warn("Invalide HandShake msg : {}", remotePeerEvent.getContent());
        }
        debugRegistry();
    }

    private PeerList getList(JsonObject getPeerListJson) {
        GetPeerList getPeerList = GetPeerList.from(getPeerListJson);
        int max = getPeerList.getMax() > 0 ? getPeerList.getMax() : Integer.MAX_VALUE;
        List<String> excludes = getPeerList.getExclude();

        List<String> peer1List = registry
            .values()
            .stream()
            .filter(peer -> peer.isOutgoing())
            .map(peer -> peer.getAddress() + ":" + peer.getOutgoingPort())
            .filter(adrPort -> !excludes.contains(adrPort))
            .collect(Collectors.toList());
        
        List<String> peerList2 = registry
	        .values()
	        .stream()
	        .filter(peer -> peer.isOnlyIncomingHandshaked())
	        .map(peer -> peer.getHandshake().getAddrMe())
	        .filter(adrPort -> !excludes.contains(adrPort))
	        .collect(Collectors.toList());
        
        List<String> peerList = Stream.of(peer1List, peerList2)
        	.flatMap(list -> list.stream())
        	.limit(max)
        	.collect(Collectors.toList());
        
        return new PeerList(peerList);

    }

}
