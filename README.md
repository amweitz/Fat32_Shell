# FAT32 File System Utility

## Overview

This project is a simple shell-like utility implemented in Java that interacts with FAT32 file system images. It allows users to navigate, view information, and read data from a FAT32 file system without corrupting the image. 

## Features

The utility supports the following commands:

- **`stop`**: Exits the shell-like utility.
- **`info`**: Displays information about the FAT32 file system, including:
  - Bytes per sector
  - Sectors per cluster
  - Reserved sector count
  - Number of FATs
  - FAT size
- **`stat <FILE_NAME/DIR_NAME>`**: Displays the size, attributes, and starting cluster number of a specified file or directory.
- **`ls`**: Lists the contents of the current directory, including hidden files and special entries (`.` and `..`).
- **`size <FILE_NAME>`**: Displays the size of a specified file.
- **`cd <DIR_NAME>`**: Changes the current directory to a specified subdirectory.
- **`read <FILE_NAME> <OFFSET> <NUMBYTES>`**: Reads and prints the specified number of bytes from a file, starting at a given offset.

## Compile and Run

After compiling the Java files, run the following command:

```bash
java FAT32Reader /path/to/your/fat32-image.img


