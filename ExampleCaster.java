/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malamakasanda
 */
//import java and mcgui classes
import mcgui.*;
import java.util.*;

public class ExampleCaster extends Multicaster {
     
     //various sequence_id's for message propagation
     int seq; 
     int node_seq_id;
     int actual_sequence; 
     int seq_id;
     //collections to hold messages
     ArrayList<ExampleMessage> pending_queue; 
     ArrayList<ExampleMessage> delivered; 
     ArrayList<ExampleMessage> not_delivered; 
     ArrayList<ExampleMessage> sequencer_hist; 
     //variables for checking node availability
     int[] next; 
     boolean[] active; 
     int[] nodestatus;
     int ALIVE;
     int DEAD ;

    /**
     * main constructor to initialize the ExampleCaster class
     */
    @SuppressWarnings("Convert2Diamond")
    public void init() {
        seq_id = hosts - 1;

        seq = 0;
        ALIVE = 0;
        DEAD = 1;
        node_seq_id = 0;
        actual_sequence = 1;
        pending_queue = new ArrayList<ExampleMessage>();
        delivered = new ArrayList<ExampleMessage>();
        not_delivered = new ArrayList<ExampleMessage>();
        sequencer_hist = new ArrayList<ExampleMessage>();
        next = new int[hosts];
        nodestatus = new int[hosts];
        active = new boolean[hosts];
        for(int i=0; i<hosts; i++) {
            next[i] = 1;
            active[i] = true;
        }
        
        mcui.debug("The network has " + hosts + " hosts!\n");
        //determine who the sequencer is
        if(is_sequencer()){
            mcui.debug("This is the sequencer");
        }else{
            mcui.debug("The sequencer is:"+seq_id);
        }
        
    }
        
    /**
     * The GUI calls this module to multicast a message
     */
    public void cast(String messagetext) {
        // Creating message with its own id as sender and its local sequence number attached
        ExampleMessage msg = new ExampleMessage(id, ++node_seq_id, messagetext);
        not_delivered.add(msg);
        // Send message to sequencer first
        bcom.basicsend(seq_id, msg);
    }
    
    /**
     * Receive a basic message
     * @param peer  unused
     * @param message  The message received
     */
    public void basicreceive(int peer, Message message) {
        //Check if I am not sequencer and if I received a message without global sequence number
        if (((ExampleMessage)message).getGlobalSeq() == -1) {
            if (id == seq_id) {
                broadcast_id((ExampleMessage) message);
            } else {
                sequencer_hist.add((ExampleMessage) message);
            }
        } else {
            deliver((ExampleMessage) message);
        }        
    }

    /**
     * Used to check if node is active before sending message
     */
    private void confirm_status(int peer, ExampleMessage msg) {
        if (active[peer]) {
            bcom.basicsend(peer, msg);
        }
    }
    
    /**
     * Used to check if node is active before sending message
     */
    private boolean is_sequencer(){
        boolean sequencer = false;
        if (id == seq_id) {
            sequencer = true;
        }else{
            sequencer = false;
            
        }
        return sequencer;
    }
    /**
     * Method to check messages pending a sequence number from 
     * the sequencer
     */
    private void no_sequence_num() {
        for(int i=0; i<hosts; i++) {
            if (active[i]) {
                confirm_local_msg_id(i);
            }
        }
    }

    /**
     * Method to add sequence numbers by the sequencer and broadcast to all other 
     * nodes including itself
     */
    private void broadcast_id(ExampleMessage message) {
        //check the orginator of this message
        if (next[message.getOriginalSender()] == message.getLocalSeq()) {
            ExampleMessage globalMsg = new ExampleMessage(message, id, ++seq);
            next[message.getOriginalSender()]++;
            for (int i=0; i<hosts; i++) {
                if (i != id) {
                    confirm_status(i, globalMsg);
                }
            }
            sequencer_hist.remove(message);
            deliver(globalMsg);
        } else {
            sequencer_hist.add(message);
        }
        confirm_local_msg_id(message.getOriginalSender());
    }


    
    /**
     * Method to set new Sequencer if the old one dies
     * 
     */
    private void set_new_sequencer(){
        for (int i = hosts - 1; i >= 0; i--){
            //grab only active and alive nodes to take part in election process
            if(nodestatus[i] == ALIVE && active[i]== true){
                seq_id = i;
            }
        }
        if(is_sequencer()){
            mcui.debug("I'm the new sequencer.");
        }else{
            mcui.debug("The sequencer is:"+seq_id);
        }
    }

    /**
     * Method used to enforce causal ordering, by checking through any msg
     * without a sequence_num from the sequencer
     */
    private void confirm_local_msg_id(int peer) {
        for (int i=0; i<sequencer_hist.size(); i++) {
            ExampleMessage msg = sequencer_hist.get(i);
            if (msg.getOriginalSender() == peer && msg.getLocalSeq() == next[peer]) {
                broadcast_id(msg);
                break;
            }
        }
    }

    /**
     * Method to reliably broadcast a msg and used for re-transmits incase 
     * of a node crash
     */
    private void r_broadcaster(ExampleMessage msg) {
        if (id != msg.getSender()) {
            ExampleMessage globalMsg = new ExampleMessage(msg, id);
            for (int i=0; i<hosts; i++) {
                if (i != id && i != msg.getSender()) {
                    confirm_status(i, globalMsg);
                }
            }
        }
    }

    /**
     * Method to deliver a msg if it has not been delivered yet.
     */
    private void deliver(ExampleMessage msg) {
        if (!isDelivered(msg)) {
            queue_deliver(msg);
            r_broadcaster(msg);
        }
    }

    /**
     * Method to dequeue the pending queue
     */
    private void queue_deliver(ExampleMessage msg) {
        int sender = msg.getSender();
        pending_queue.add(msg);
        int i = 0;
        while (i < pending_queue.size()) {
            ExampleMessage msgInBag = pending_queue.get(i);
            i++;
            if (msgInBag.getGlobalSeq() == actual_sequence) {
                mcui.deliver(msg.getOriginalSender(), msg.getText());
                actual_sequence++;
                
                // Updating non-sequensers local number in case they become sequencers
                if (id != seq_id) {
                   next[msg.getOriginalSender()] = msg.getLocalSeq() + 1;
                }

                delivered.add(msgInBag);
                pending_queue.remove(msgInBag);
                not_delivered.remove(msgInBag);

                // Since message was delivered, go through entire bag again in search for next expected
                i = 0;
            }
        }
    }

    /**
     * Method to check if a message has been delivered to a node
     * @param msg  The message to check
     */
    private boolean isDelivered(ExampleMessage msg) {
        return delivered.contains(msg);
    }

    /**
     * Signals that a peer is down and has been down for a while to
     * allow for messages taking different paths from this peer to
     * arrive.
     * @param peer	The dead peer
     */
    public void basicpeerdown(int peer) {
        active[peer] = false;
        nodestatus[peer] = DEAD;
        mcui.debug("Peer " + peer + " has been dead for a while now!");
        if (peer == seq_id) {
            //choose another node if the sequencer was the one that died
            set_new_sequencer();
            for(int i=seq_id+1; i<hosts; i++) {
                if (active[i]) {
                    seq_id = i;
                    if (id == i) {
                        seq = actual_sequence-1;
                        no_sequence_num();
                    }
                    break;
                }
            }

            for(int i=0; i<not_delivered.size(); i++) {
                confirm_status(seq_id, not_delivered.get(i));
            }
        }
    }

}
