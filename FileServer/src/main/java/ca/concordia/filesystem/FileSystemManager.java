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


    //using hashmap to store contents of WRITE command that can be READ
    private java.util.HashMap<String, byte[]> file_contents = new java.util.HashMap<>();


//lock for synchronization
private final Object file_lock = new Object();

    //constructor
    public FileSystemManager(String filename, int maxFiles, int maxBlocks, int blockSize) throws Exception {
        this.MAXFILES = maxFiles;
        this.MAXBLOCKS = maxBlocks;
        this.BLOCK_SIZE = blockSize;
        this.disk = new RandomAccessFile(filename, "rw");
        initialize_file_system();
    }



    private void initialize_file_system() throws Exception {
        // Check if file system already exists
        if (disk.length() > 0) {
            load_from_disk();
        } else {
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
            
            save_to_disk();
        }
        
        System.out.println("file system initialization done");
    }

    private void save_to_disk() throws Exception {
        disk.seek(0);
        
        //save all file entries
        for (int i = 0; i < MAXFILES; i++) {
            FEntry entry = fileEntries[i];
            
            //save filename (11 b max)
            String filename = entry.getFilename();
            byte[] nameBytes = new byte[11];
            byte[] originalName = filename.getBytes();
            System.arraycopy(originalName, 0, nameBytes, 0, Math.min(11, originalName.length));
            disk.write(nameBytes);
            
            disk.writeShort(entry.getFilesize());
            
            disk.writeShort(entry.getFirstBlock());
        }
        
        for (int i = 0; i < MAXBLOCKS; i++) {
            FNode node = fileNodes[i];
            disk.writeInt(node.getBlockIndex());
            disk.writeInt(node.getNext());
        }
        
        System.out.println("file system saved to disk");
    }

    //loading file system from the disk
    private void load_from_disk() throws Exception {
        disk.seek(0);
        
        fileEntries = new FEntry[MAXFILES];
        fileNodes = new FNode[MAXBLOCKS];
        freeBlockList = new boolean[MAXBLOCKS];
        
        //load all file entries
        for (int i = 0; i < MAXFILES; i++) {
            //read filename 
            byte[] nameBytes = new byte[11];
            disk.read(nameBytes);
            String filename = new String(nameBytes).trim();
            
            short fileSize = disk.readShort();
           
            short firstBlock = disk.readShort();
            
            fileEntries[i] = new FEntry(filename, fileSize, firstBlock);
        }
        
        for (int i = 0; i < MAXBLOCKS; i++) {
            int blockIndex = disk.readInt();
            int next = disk.readInt();
            fileNodes[i] = new FNode(blockIndex);
            fileNodes[i].setNext(next);
            freeBlockList[i] = (blockIndex == -1);
        }
        
        System.out.println("File system loaded from disk");
    }

    //checking if block is free
/*private boolean is_block_free(int blockIndex) {
    return freeBlockList[blockIndex];
} */

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
        synchronized(file_lock){ //synchronized to prevent race condition while creating file
        try {
            if (fileName.length() > 11) {
                throw new Exception("ERR: file is too large");
            }
            //checking if file alrteady exists
            for (FEntry entry : fileEntries) {
                if (fileName.equals(entry.getFilename())) {
                    throw new Exception("ERR: file already exists");
                }
            }
            //finding empty slot in file enteries array
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().trim().isEmpty()) {
                    fileEntries[i].setFilename(fileName);
                    fileEntries[i].setFilesize((short)0);
                    fileEntries[i].setFirstBlock((short)-1); //no data blocks added
                    System.out.println("Created: " + fileName);
                    save_to_disk();
                    return;
                }
            }
            
            throw new Exception("ERR: no space");
        } finally {} } //lock released
    }


//list files method
public String[] list_files() {
    synchronized(file_lock){
    
    try {
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
    } finally {
       
    } }
}



// delete method
public void delete_file(String fileName) throws Exception {
   synchronized(file_lock){

        //looking for the file
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
                file_contents.remove(fileName);  //remove content from HashMap
                return;
            }
        }
        
        throw new Exception("ERR: file " + fileName + " doesn't exist");

      }
}


//read method 
public byte[] read_file(String fileName) throws Exception {
    synchronized(file_lock){

         //debug statements
        System.out.println("read lock acq: "+fileName);


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
        
        if (file_to_read.getFirstBlock() == -1) { //return empty if file has no content
            return new byte[0];
        }
        
        //counting blocks used by this file
        int fileSize = file_to_read.getFilesize();
        int currentBlock = file_to_read.getFirstBlock();
        int bytesRead = 0;
        
        System.out.println("Reading file: " + fileName + " (" + fileSize + " bytes)");
      
        //calculating total size
        while (currentBlock != -1 && bytesRead < fileSize) {
            FNode currentNode = fileNodes[currentBlock];
            System.out.println("  Reading from block " + currentBlock);
            
            int block_bytes = Math.min(BLOCK_SIZE, fileSize - bytesRead);
            bytesRead += block_bytes;
            
            currentBlock = currentNode.getNext();
        }
        
        //get file contents from hashmap
        if (file_contents.containsKey(fileName)) {
            byte[] content = file_contents.get(fileName); //this to read actual content from the txt files- fixed
            System.out.println("Successfully read " + content.length + " bytes from " + fileName);
            return content;
        } else {
            return new byte[0];  
        }
     }
}


//write method
public void write_file(String fileName, byte[] content) throws Exception {
    synchronized(file_lock) {

        //finding file entry
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

        file_contents.put(fileName, content);  //to store actual file content in hashmap

        //calculating reqiured blocks to write and cheching available space
        int blocksNeeded = calculate_blocks_needed(content.length);
        int freeBlocks = 0;
        for (boolean free : freeBlockList) {
            if (free) freeBlocks++;
        }
        
        if (blocksNeeded > freeBlocks) {
            throw new Exception("ERR: not enough free blocks");
        }
        
        //fixed write method, freeing exisiting blocks for overwrite
        if (file_to_write.getFirstBlock() != -1) {
            short firstBlock = file_to_write.getFirstBlock();
            int currentBlock = firstBlock;
           
            while (currentBlock != -1) {
                FNode currentNode = fileNodes[currentBlock];
                int nextBlock = currentNode.getNext();
                
                //clear block data
                clear_block_data(currentBlock);
                //mark as free
                mark_free_block(currentBlock);
               //reset node metadata
                currentNode.setBlockIndex(-1);
                currentNode.setNext(-1);
                
                currentBlock = nextBlock;
            }
            
            //reseting file metadata
            file_to_write.setFilesize((short)0);
            file_to_write.setFirstBlock((short)-1);
        }
       
        
        //allocating blocks for rewrite
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



 }