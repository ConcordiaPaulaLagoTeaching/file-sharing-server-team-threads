package ca.concordia.filesystem.datastructures;

public class FEntry { //

    private String filename;
    private short filesize;
    private short firstBlock; // Pointers to data blocks

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException{
        //Check filename is max 11 bytes long
        if (filename.length() > 11) {
            throw new IllegalArgumentException("file name cannot be longer than 11 strings.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    //default constructorbloading frm disk
    public FEntry(){

        this.filename =" ";
        this.filesize = 0;
        this.firstBlock = -1;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("file name cannot be longer than 11 strings.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException(" file size cannot be -ve.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }
    public void setFirstBlock (short firstBlock){
        this.firstBlock =firstBlock;
    }
    //helper method to check if this FEntry is in use

    public boolean is_in_use() {
        return filename != null && !filename.isEmpty();
    }
    
    //method to mark the entry as free
    public void mark_free() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }
    
    //calculating size
    public static int get_disk_size() {
        return 15; // 11b from filename + 2b of size + 2b of firstBlock = 15b
    }




}
