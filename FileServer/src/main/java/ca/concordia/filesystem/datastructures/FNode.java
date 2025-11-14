package ca.concordia.filesystem.datastructures;

public class FNode { 

    private int blockIndex;
    private int next;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    // getter and setters
    public int getBlockIndex() {
        return blockIndex;
    }
    
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }
    
    public int getNext() {
        return next;
    }
    
    public void setNext(int next) {
        this.next = next;
    }
    
    //helper method to check if block is in use
    public boolean is_in_use() {
        return blockIndex >= 0;  
    }
}
