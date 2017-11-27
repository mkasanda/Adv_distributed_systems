/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author malamakasanda
 */
import mcgui.*;
public class ExampleMessage extends Message {
        
    private String text;
    private int originalSender;
    private int localSeq;
    private int globalSeq;


    public ExampleMessage(int sender, int localSeq, String text) {
        super(sender);
        originalSender = sender;
        this.text = text;
        this.localSeq = localSeq;
        globalSeq = -1;
    }

    /**
     * Main class constructor
     */
    public ExampleMessage(ExampleMessage msg, int sender, int globalSeq) {
        super(sender);
        text = msg.getText();
        originalSender = msg.getOriginalSender();
        localSeq = msg.getLocalSeq();
        this.globalSeq = globalSeq;
    }

    
    public ExampleMessage(ExampleMessage msg, int sender) {
        super(sender);
        text = msg.getText();
        originalSender = msg.getOriginalSender();
        localSeq = msg.getLocalSeq();
        globalSeq = msg.getGlobalSeq();
    }
    
    /**
     * Get and Set methods to access the private variables
     */
    public String getText() {
        return text;
    }

    public int getOriginalSender() {
        return originalSender;
    }

    public int getLocalSeq() {
        return localSeq;
    }

    public int getGlobalSeq() {
        return globalSeq;
    }
    
    public static final long serialVersionUID = 0;
    
}
