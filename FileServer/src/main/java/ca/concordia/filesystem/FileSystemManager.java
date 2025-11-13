package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final RandomAccessFile disk;

    private static final int BLOCK_SIZE = 128;

    private FEntry[] fileEntries;    // For file entries
    private FNode[] fileNodes;       // For file nodes
    private boolean[] freeBlockList; // For free blocks

    //constructor
    public FileSystemManager(String filename, int totalSize) throws Exception {
        this.disk = new RandomAccessFile(filename, "rw");
        initializeFileSystem();
    }

    private void initializeFileSystem() throws Exception {
        fileEntries = new FEntry[MAXFILES];
        for (int i = 0; i < MAXFILES; i++) {
            fileEntries[i] = new FEntry();
        }
        
        fileNodes = new FNode[MAXBLOCKS];
        for (int i = 0; i < MAXBLOCKS; i++) {
            fileNodes[i] = new FNode(-1);
        }
        
        freeBlockList = new boolean[MAXBLOCKS];
        for (int i = 0; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true;
        }
        
        System.out.println("File system initialized!");
    }

    //create file method
    public void createFile(String fileName) throws Exception {
        if (fileName.length() > 11) {
            throw new Exception("ERROR: filename too large");
        }
        
        for (FEntry entry : fileEntries) {
            if (fileName.equals(entry.getFilename())) {
                throw new Exception("ERROR: file already exists");
            }
        }
        
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

//delete method
public void deleteFile(String fileName) throws Exception {
    //looking for the file
    for (int i = 0; i < MAXFILES; i++) {
        if (fileName.equals(fileEntries[i].getFilename())) {
            //deleting file found by file name
            fileEntries[i].setFilename("");
            fileEntries[i].setFilesize((short)0);
            fileEntries[i].setFirstBlock((short)-1);
            System.out.println("Deleted: " + fileName);
            return;
        }
    }
    //file not found
    throw new Exception("ERROR: file " + fileName + " does not exist");
}

//read method
public byte[] readFile(String fileName) throws Exception {
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            System.out.println("Read file: " + fileName);
            return new byte[0];
        }
    }
    throw new Exception("ERROR: file " + fileName + " does not exist");
}

// write method
public void writeFile(String fileName, byte[] content) throws Exception {
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            entry.setFilesize((short)content.length);
            System.out.println("Wrote " + content.length + " bytes to: " + fileName);
            return;
        }
    }
    throw new Exception("ERROR: file " + fileName + " does not exist");
}
 }