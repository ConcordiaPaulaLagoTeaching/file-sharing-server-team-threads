package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    private FEntry[] fileEntries;    // For file entries
    private FNode[] fileNodes;       // For file nodes

    public FileSystemManager(String filename, int totalSize) {
        if(instance == null) {
            //TODO Initialize the file system
            try {
                initializeFileSystem();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    private void initializeFileSystem() throws Exception {
        fileEntries = new FEntry[MAXFILES]; //file enteries array
        for (int i = 0; i < MAXFILES; i++) {
            fileEntries[i] = new FEntry(); //empty file
        }
        
        fileNodes = new FNode[MAXBLOCKS]; //file nodes array
        for (int i = 0; i < MAXBLOCKS; i++) {
            fileNodes[i] = new FNode(-1); //free nodes
        }
        
        freeBlockList = new boolean[MAXBLOCKS]; //free blocks array
        for (int i = 0; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true; //allblocks free
        }
        
        System.out.println("File system initialized!");
    }

    //create file method
    public void createFile(String fileName) throws Exception {
        //checking file name lenght
        if (fileName.length() > 11) {
            throw new Exception("ERROR: filename too large");
        }
        
        //checking if file exists
        for (FEntry entry : fileEntries) {
            if (fileName.equals(entry.getFilename())) {
                throw new Exception("ERROR: file already exists");
            }
        }
        
        //finding empty slot and creating file there
        for (int i = 0; i < MAXFILES; i++) {
            if (fileEntries[i].getFilename().trim().isEmpty()) {
                fileEntries[i].setFilename(fileName);
                fileEntries[i].setFilesize((short)0);
                fileEntries[i].setFirstBlock((short)-1);
                System.out.println("Created: " + fileName);
                return;
            }
        }
        
        throw new Exception("ERROR: no space");
    }

    //list files method
    public String[] listFiles() {
        int count = 0;
        for (FEntry entry : fileEntries) {
            if (!entry.getFilename().trim().isEmpty()) {
                count++;
            }
        }
        
        //array of filenames
        String[] files = new String[count];
        int index = 0;
        
        for (FEntry entry : fileEntries) {
            if (!entry.getFilename().trim().isEmpty()) {
                files[index] = entry.getFilename();
                index++;
            }
        }
        
        return files;
    }

    //TODO- add delete, read and write methods
}