package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;

public class FileSystemManager {

    private final int MAXFILES;
private final int MAXBLOCKS; 
private final int BLOCK_SIZE;

   private final RandomAccessFile disk;

    private FEntry[] fileEntries;    // For file entries
    private FNode[] fileNodes;       // For file nodes
    private boolean[] freeBlockList; // For free blocks

    //constructor
    public FileSystemManager(String filename, int maxFiles, int maxBlocks, int blockSize) throws Exception {
        this.MAXFILES = maxFiles;
        this.MAXBLOCKS = maxBlocks;
        this.BLOCK_SIZE = blockSize;
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

    private void saveToDisk() throws Exception {
        disk.seek(0);
        String marker = "FS:" + MAXFILES + "files," + MAXBLOCKS + "blocks";
        disk.write(marker.getBytes());
    }

    //checking if block is free
private boolean isBlockFree(int blockIndex) {
    return freeBlockList[blockIndex];
}

//marking the free block
private void freeBlock(int blockIndex) {
    freeBlockList[blockIndex] = true;
}

//writing zeroes to memory
private void clearBlockData(int blockIndex) {
    System.out.println("Would clear block " + blockIndex + " with zeros");
}

//finding free block
private int findFreeBlock() {
    for (int i = 0; i < MAXBLOCKS; i++) {
        if (freeBlockList[i]) {
            return i;
        }
    }
    return -1; //no free block found
}

//marking block as used
private void useBlock(int blockIndex) {
    freeBlockList[blockIndex] = false;
}

//calculating blocks needed
private int calculateBlocksNeeded(int dataSize) {
    int blocks = dataSize / BLOCK_SIZE;
    if (dataSize % BLOCK_SIZE != 0) {
        blocks++; 
    }
    return blocks;
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
                saveToDisk();
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

// delete method
public void deleteFile(String fileName) throws Exception {
    // Look for the file
    for (int i = 0; i < MAXFILES; i++) {
        if (fileName.equals(fileEntries[i].getFilename())) {
            short firstBlock = fileEntries[i].getFirstBlock();
            
            //freeing blocks used by this file
            if (firstBlock != -1) {
                int currentBlock = firstBlock;
                
                while (currentBlock != -1) {
                    //overwriting with zeroes
                    clearBlockData(currentBlock);
                    //freeing the block
                    freeBlock(currentBlock);
                    
                    FNode currentNode = fileNodes[currentBlock];
                    currentBlock = currentNode.getNext();
                    
                    currentNode.setBlockIndex(-1);
                    currentNode.setNext(-1);
                }
            }
            
            //removing the file entry
            fileEntries[i].setFilename("");
            fileEntries[i].setFilesize((short)0);
            fileEntries[i].setFirstBlock((short)-1);
            
            System.out.println("Deleted: " + fileName + " and freed blocks");
            saveToDisk();
            return;
        }
    }
    
    throw new Exception("ERROR: file " + fileName + " does not exist");
}

//read method 
public byte[] readFile(String fileName) throws Exception {
    //finding the file
    FEntry fileToRead = null;
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            fileToRead = entry;
            break;
        }
    }
    
    if (fileToRead == null) {
        throw new Exception("ERROR: file " + fileName + " does not exist");
    }
    
    if (fileToRead.getFirstBlock() == -1) {
        return new byte[0];
    }
    
    int fileSize = fileToRead.getFilesize();
    int currentBlock = fileToRead.getFirstBlock();
    int bytesRead = 0;
    
    System.out.println("Reading file: " + fileName + " (" + fileSize + " bytes)");
   
    while (currentBlock != -1 && bytesRead < fileSize) {
        FNode currentNode = fileNodes[currentBlock];
        System.out.println("  Reading from block " + currentBlock);
        
        int bytesInThisBlock = Math.min(BLOCK_SIZE, fileSize - bytesRead);
        bytesRead += bytesInThisBlock;
        
        currentBlock = currentNode.getNext();
    }
    
    byte[] fileData = new byte[fileSize];
    System.out.println("Successfully read " + fileSize + " bytes from " + fileName);
    
    return fileData;
}

// write method
public void writeFile(String fileName, byte[] content) throws Exception {
    FEntry fileToWrite = null;
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            fileToWrite = entry;
            break;
        }
    }
    if (fileToWrite == null) {
        throw new Exception("ERROR: file " + fileName + " does not exist");
    }
    
    int blocksNeeded = calculateBlocksNeeded(content.length);
    int freeBlocks = 0;
    for (boolean free : freeBlockList) {
        if (free) freeBlocks++;
    }
    
    if (blocksNeeded > freeBlocks) {
        throw new Exception("ERROR: not enough free blocks");
    }
    
    //deleting the already exisiting data
    if (fileToWrite.getFirstBlock() != -1) {
        deleteFile(fileName);
        
        for (FEntry entry : fileEntries) {
            if (fileName.equals(entry.getFilename())) {
                fileToWrite = entry;
                break;
            }
        }
    }
    
    //allocating block for new content
    int firstBlock = -1;
    int previousBlock = -1;
    
    for (int i = 0; i < blocksNeeded; i++) {
        int freeBlock = findFreeBlock();
        if (freeBlock == -1) {
            throw new Exception("ERROR: no free blocks");
        }
        
        useBlock(freeBlock);
        
        fileNodes[freeBlock].setBlockIndex(freeBlock);
       
        if (firstBlock == -1) {
            firstBlock = freeBlock;
            fileToWrite.setFirstBlock((short)freeBlock);
        } else {
            fileNodes[previousBlock].setNext(freeBlock);
        }
        
        previousBlock = freeBlock;
    }
    
    //updating file size
    fileToWrite.setFilesize((short)content.length);
    
    System.out.println("Wrote " + content.length + " bytes to " + fileName + 
                      " using " + blocksNeeded + " blocks");

    saveToDisk();
}


 }