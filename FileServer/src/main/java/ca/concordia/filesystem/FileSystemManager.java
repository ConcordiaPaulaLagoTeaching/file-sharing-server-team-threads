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
        initialize_file_system();
    }



    private void initialize_file_system() throws Exception {
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
        
        System.out.println("file system initialization done");
    }

    private void save_to_disk() throws Exception {
        disk.seek(0);
        String marker = "FS:" + MAXFILES + "files," + MAXBLOCKS + "blocks";
        disk.write(marker.getBytes());
    }

    //checking if block is free
private boolean is_block_free(int blockIndex) {
    return freeBlockList[blockIndex];
}

//marking the free block
private void mark_free_block(int blockIndex) {
    freeBlockList[blockIndex] = true;
}

//writing zeroes to memory
private void clear_block_data(int blockIndex) {
    System.out.println("clearing block " + blockIndex + " with zeros");
}

//finding free block
private int find_free_block() {
    for (int i = 0; i < MAXBLOCKS; i++) {
        if (freeBlockList[i]) {
            return i;
        }
    }
    return -1; //no free block found
}

//marking block as used
private void used_block(int blockIndex) {
    freeBlockList[blockIndex] = false;
}

//calculating blocks needed
private int calculate_blocks_needed(int dataSize) {
    int blocks = dataSize / BLOCK_SIZE;
    if (dataSize % BLOCK_SIZE != 0) {
        blocks++; 
    }
    return blocks;
}

    //create file method
    public void create_file(String fileName) throws Exception {
        if (fileName.length() > 11) {
            throw new Exception("ERR: file is too large");
        }
        
        for (FEntry entry : fileEntries) {
            if (fileName.equals(entry.getFilename())) {
                throw new Exception("ERR: file already exists");
            }
        }
        
        for (int i = 0; i < MAXFILES; i++) {
            if (fileEntries[i].getFilename().trim().isEmpty()) {
                fileEntries[i].setFilename(fileName);
                fileEntries[i].setFilesize((short)0);
                fileEntries[i].setFirstBlock((short)-1);
                System.out.println("Created: " + fileName);
                save_to_disk();
                return;
            }
        }
        
        throw new Exception("ERR: no space");
    }

    //list files method
    public String[] list_files() {
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
public void delete_file(String fileName) throws Exception {
    // Look for the file
    for (int i = 0; i < MAXFILES; i++) {
        if (fileName.equals(fileEntries[i].getFilename())) {
            short firstBlock = fileEntries[i].getFirstBlock();
            
            //freeing blocks used by this file
            if (firstBlock != -1) {
                int currentBlock = firstBlock;
                
                while (currentBlock != -1) {
                    //overwriting with zeroes
                    clear_block_data(currentBlock);
                    //freeing the block
                    mark_free_block(currentBlock);
                    
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
            
            System.out.println("Deleted: " + fileName );
            save_to_disk();
            return;
        }
    }
    
    throw new Exception("ERR: file " + fileName + " doesn't exist");
}

//read method 
public byte[] read_file(String fileName) throws Exception {
    //finding the file
    FEntry file_to_read = null;
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            file_to_read = entry;
            break;
        }
    }
    
    if (file_to_read == null) {
        throw new Exception("ERR: file " + fileName + " does not exist");
    }
    
    if (file_to_read.getFirstBlock() == -1) {
        return new byte[0];
    }
    
    int fileSize = file_to_read.getFilesize();
    int currentBlock = file_to_read.getFirstBlock();
    int bytesRead = 0;
    
    System.out.println("Reading file: " + fileName + " (" + fileSize + " bytes)");
   
    while (currentBlock != -1 && bytesRead < fileSize) {
        FNode currentNode = fileNodes[currentBlock];
        System.out.println("  Reading from block " + currentBlock);
        
        int block_bytes = Math.min(BLOCK_SIZE, fileSize - bytesRead);
        bytesRead += block_bytes;
        
        currentBlock = currentNode.getNext();
    }
    
    byte[] fileData = new byte[fileSize];
    System.out.println("Successfully read " + fileSize + " bytes from " + fileName);
    
    return fileData;
}

// write method
public void write_file(String fileName, byte[] content) throws Exception {
    FEntry file_to_write = null;
    for (FEntry entry : fileEntries) {
        if (fileName.equals(entry.getFilename())) {
            file_to_write = entry;
            break;
        }
    }
    if (file_to_write == null) {
        throw new Exception("ERR: file " + fileName + " does not exist");
    }
    
    int blocksNeeded = calculate_blocks_needed(content.length);
    int freeBlocks = 0;
    for (boolean free : freeBlockList) {
        if (free) freeBlocks++;
    }
    
    if (blocksNeeded > freeBlocks) {
        throw new Exception("ERR: not enough free blocks");
    }
    
    //deleting the already exisiting data
    if (file_to_write.getFirstBlock() != -1) {
        delete_file(fileName);
        
        for (FEntry entry : fileEntries) {
            if (fileName.equals(entry.getFilename())) {
                file_to_write = entry;
                break;
            }
        }
    }
    
    //allocating block for new content
    int firstBlock = -1;
    int previousBlock = -1;
    
    for (int i = 0; i < blocksNeeded; i++) {
        int freeBlock = find_free_block();
        if (freeBlock == -1) {
            throw new Exception("ERR: no free blocks");
        }
        
        used_block(freeBlock);
        
        fileNodes[freeBlock].setBlockIndex(freeBlock);
       
        if (firstBlock == -1) {
            firstBlock = freeBlock;
            file_to_write.setFirstBlock((short)freeBlock);
        } else {
            fileNodes[previousBlock].setNext(freeBlock);
        }
        
        previousBlock = freeBlock;
    }
    
    //updating file size
    file_to_write.setFilesize((short)content.length);
    
    System.out.println("Written " + content.length + " bytes to " + fileName + 
                      " using " + blocksNeeded + " blocks");

    save_to_disk();
}


 }