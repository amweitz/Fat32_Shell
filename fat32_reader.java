import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class fat32_reader {
    private static int BPB_BytesPerSec;
    private static int BPB_SecPerClus;
    private static int BPB_RsvdSecCnt;
    private static int BPB_NumFATs;
    private static long BPB_FATSz32;
    private static long BPB_RootClus;
    private static long firstCluster;
    private static int fatStart;
    private static String filePath;
    private static Stack < Node > stack;
    private static StringBuilder builder;

    public static void main(String[] args) {
        filePath = args[0];
        loader();
        builder = new StringBuilder();
        builder.append("/");
        stack = new Stack < > ();
        HashMap < String, byte[] > map = clusterGetter(BPB_RootClus);
        Node temp = new Node(map, builder.toString());
        stack.push(temp);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(builder.toString() + "] ");
            if (scanner.hasNextLine()) {
                String c = scanner.nextLine();
                String[] split = c.split(" ");
                String b = split[0].toLowerCase();
                if (System.console() == null) {
                    System.out.println("");
                }
                if (b.equals("stop")) {
                    scanner.close();
                    return;
                }
                if (b.equals("info")) {
                    info();
                }
                if (b.equals("ls")) {
                    ls();
                }
                if (split[0].equals("stat")) {
                    if (split.length == 2) {
                        stat(split[1]);
                    }
                }
                if (split[0].equals("size")) {
                    if (split.length == 2) {
                        size(split[1]);
                    }
                }
                if (split[0].equals("cd")) {
                    if (split.length == 2) {
                        cd(split[1]);
                    }
                }
                if (split[0].equals("read")) {
                    if (split.length == 4) {
                        read(split[1], split[2], split[3]);
                    }
                }
            } else {
                scanner.close();
                return;
            }
        }
    }
    private static void read(String file, String fset, String lg) {
        int offset = -1;
        int length = -1;
        try {
            offset = Integer.parseInt(fset);
            length = Integer.parseInt(lg);
        } catch (IllegalArgumentException a) {
            System.err.println("Error reading file: " + a.getMessage());
            System.exit(1);
        }
        if (offset < 0) {
            System.out.println("Error: OFFSET must be a positive value");
            return;
        }
        if (length <= 0) {
            System.out.println("Error: NUM_BYTES must be greater than zero");
            return;
        }
        Node node = stack.peek();
        String g = file.toUpperCase();
        HashMap < String, byte[] > map = node.getMap();
        if (!map.keySet().contains(g)) {
            System.out.println("Error: " + file + " is not a file");
            return;
        } else {
            byte[] bytes = map.get(g);
            byte temp = bytes[11];
            if ((temp & 16) > 0) {
                System.out.println("Error: " + file + " is not a file");
                return;
            } else {
                String a = getClu(bytes);
                long longValue = Long.parseLong(a, 16);
                ArrayList < Byte > list = clusterReader(longValue);
                int check = offset + length;
                if (check > list.size()) {
                    System.out.println("Error: attempt to read data outside of file bounds");
                    return;
                }
                for (int x = offset; x < (offset + length); x++) {
                    byte byt = list.get(x);
                    int e = byt;
                    if ((e > 6 && e < 14) || (e > 31 && e < 127)) {
                        char t = (char) byt;
                        System.out.print(t);
                    } else {
                        String hexString = String.format("%02X", byt);
                        System.out.print(hexString);
                    }

                }
            }
            System.out.println("");
        }
    }
    private static void info() {
        //printing BPB_BytesPerSec
        String act = Integer.toHexString(BPB_BytesPerSec);
        System.out.printf("BPB_BytesPerSec is 0x%s, %d\n", act, BPB_BytesPerSec);

        //printing BPB_SecPerClus
        String hexValue3 = String.format("%X", BPB_SecPerClus);
        System.out.printf("BPB_SecPerClus is 0x%s, %d\n", hexValue3, BPB_SecPerClus);

        //printing BPB_RsvdSecCnt
        String hexValue2 = Integer.toHexString(BPB_RsvdSecCnt);
        System.out.printf("BPB_RsvdSecCnt is 0x%s, %d\n", hexValue2, BPB_RsvdSecCnt);

        //printing BPB_NumFATs
        String hexValue = String.format("%X", BPB_NumFATs);
        System.out.printf("BPB_NumFATs is 0x%s, %d\n", hexValue, BPB_NumFATs);

        //printing BPB_FATSz32
        String hexString = Long.toHexString(BPB_FATSz32);
        System.out.printf("BPB_FATSz32 is 0x%s, %d%n", hexString, BPB_FATSz32);
    }
    private static void ls() {
        Node temp = stack.peek();
        HashMap < String, byte[] > map = temp.getMap();
        ArrayList < String > list = new ArrayList < > ();
        for (String k: map.keySet()) {
            list.add(k);
        }
        Collections.sort(list);
        if (stack.size() == 1) {
            System.out.print(". .. ");
        }
        for (String h: list) {
            System.out.print(h + " ");
        }
        System.out.println("");
    }
    private static void stat(String s) {
        if (s.equals(".")) {
            if (stack.size() > 1) {
                Node nd = stack.pop();
                String tr = nd.getName();
                stat(tr);
                stack.push(nd);
            }
            return;
        }
        if (s.equals("..")) {
            if (stack.size() > 2) {
                Node nd = stack.pop();
                Node r = stack.pop();
                String tr = r.getName();
                stat(tr);
                stack.push(r);
                stack.push(nd);
            }
            return;
        }
        String g = s.toUpperCase();
        Node node = stack.peek();
        HashMap < String, byte[] > map = node.getMap();
        if (!map.keySet().contains(g)) {
            System.out.println("Error: file/directory does not exist");
        } else {
            byte[] bytes = map.get(g);
            byte temp = bytes[11];
            long size = getSize(bytes);
            System.out.println("Size is " + size);
            ArrayList < String > attr = getStats(temp);
            System.out.print("Attributes ");
            for (int x = 0; x < attr.size(); x++) {
                if (x == attr.size() - 1) {
                    System.out.println(attr.get(x));
                } else {
                    System.out.print(attr.get(x) + " ");
                }
            }
            String next = getClu(bytes);
            System.out.println("Next cluster number is 0x" + next);
        }
    }
    private static void size(String s) {
        String g = s.toUpperCase();
        Node node = stack.peek();
        HashMap < String, byte[] > map = node.getMap();
        if (!map.keySet().contains(g)) {
            System.out.println("Error: " + s + " is not a file");
        } else {
            byte[] bytes = map.get(g);
            byte temp = bytes[11];
            if ((temp & 16) > 0) {
                System.out.println("Error: " + s + " is not a file");
            } else {
                long size = getSize(bytes);
                System.out.println("Size of " + s + " is " + size + " bytes");
            }
        }
    }
    private static void cd(String s) {
        if (s.equals(".")) {
            return;
        }
        if (s.equals("..")) {
            if (stack.size() > 1) {
                Node node = stack.pop();
                String a = node.getName();
                if (stack.size() == 1) {
                    builder.delete((builder.length() - a.length()), builder.length());
                } else {
                    builder.delete((builder.length() - (a.length() + 1)), builder.length());
                }
            }
            return;
        }
        String g = s.toUpperCase();
        Node node = stack.peek();
        HashMap < String, byte[] > map = node.getMap();
        if (!map.keySet().contains(g)) {
            System.out.println("Error: " + s + " is not a directory");
        } else {
            byte[] bytes = map.get(g);
            if ((bytes[11] & 16) == 0) {
                System.out.println("Error: " + s + " is not a directory");
            } else {
                if (stack.size() == 1) {
                    builder.append(s.toUpperCase());
                } else {
                    builder.append("/");
                    builder.append(s.toUpperCase());
                }
                String tr = getClu(bytes);
                long actual = Long.parseLong(tr, 16);
                HashMap < String, byte[] > clus = clusterGetter(actual);
                Node ne = new Node(clus, s);
                stack.push(ne);
            }
        }
    }
    private static void loader() {
        try {
            // Open the file in read-only mode
            RandomAccessFile file = new RandomAccessFile(filePath, "r");
            // Read the first 512 bytes
            byte[] buffer = new byte[64];
            file.read(buffer);
            // Close the file
            file.close();

            //printing BPB_BytesPerSec
            String res = String.format("%02X", buffer[12]) + String.format("%02X", buffer[11]);
            int inte = Integer.parseInt(res, 16);
            BPB_BytesPerSec = inte;

            //printing BPB_SecPerClus
            BPB_SecPerClus = buffer[13];

            //printing BPB_RsvdSecCnt
            String result = String.format("%02X", buffer[15]) + String.format("%02X", buffer[14]);
            int c = Integer.parseInt(result, 16);
            BPB_RsvdSecCnt = c;

            //printing BPB_NumFATs
            BPB_NumFATs = buffer[16];

            //printing BPB_FATSz32
            String fatsz = String.format("%02X", buffer[39]) + String.format("%02X", buffer[38]) + String.format("%02X", buffer[37]) + String.format("%02X", buffer[36]);
            long actual = Long.parseLong(fatsz, 16);
            BPB_FATSz32 = actual;

            String clus = String.format("%02X", buffer[47]) + String.format("%02X", buffer[46]) + String.format("%02X", buffer[45]) + String.format("%02X", buffer[44]);
            long cl = Long.parseLong(clus, 16);
            BPB_RootClus = cl;

            firstCluster = ((BPB_NumFATs * BPB_FATSz32 * BPB_BytesPerSec) + (BPB_BytesPerSec * BPB_RsvdSecCnt));

            fatStart = BPB_RsvdSecCnt * BPB_BytesPerSec;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
    private static HashMap < String, byte[] > clusterGetter(long numberCluster) {
        HashMap < String, byte[] > map = new HashMap < > ();
        long clus = numberCluster;
        do {
            try {
                long clusByte = firstCluster + ((BPB_SecPerClus * BPB_BytesPerSec) * (clus - BPB_RootClus));
                RandomAccessFile file = new RandomAccessFile(filePath, "r");
                byte[] buffer = new byte[BPB_SecPerClus * BPB_BytesPerSec];
                file.seek(clusByte);
                file.read(buffer);
                file.close();
                for (int x = 0; x < (BPB_SecPerClus * BPB_BytesPerSec); x = x + 32) {
                    int a = buffer[x] & 0xFF;
                    if (a == 0) {
                        break;
                    }
                    if (a != 229 && (buffer[x + 11] != 15)) {
                        boolean set = false;
                        byte[] temp = new byte[32];
                        int z = 0;
                        StringBuilder name = new StringBuilder();
                        StringBuilder addOn = new StringBuilder();
                        for (int y = x; y < 31 + x; y++) {
                            temp[z] = buffer[y];
                            if (z < 8 && buffer[y] != 32) {
                                name.append((char) buffer[y]);
                            }
                            if (z >= 8 && z < 11 && buffer[y] != 32) {
                                addOn.append((char) buffer[y]);
                            }
                            if (z == 11 && ((buffer[y] & 48) == 0)) {
                                set = true;
                            }
                            z++;
                        }
                        if (set == false) {
                            if (!addOn.isEmpty()) {
                                name.append(".");
                                name.append(addOn);
                            }
                            map.put(name.toString(), temp);
                        } else {
                            set = false;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(1);
            }
            clus = getNextClus(clus);
        } while (!((clus >= 268435448) && (clus <= 268435455)));
        return map;
    }

    private static ArrayList < Byte > clusterReader(long numberCluster) {
        ArrayList < Byte > list = new ArrayList < > ();
        long clus = numberCluster;
        do {
            try {
                long clusByte = firstCluster + ((BPB_SecPerClus * BPB_BytesPerSec) * (clus - BPB_RootClus));
                RandomAccessFile file = new RandomAccessFile(filePath, "r");
                byte[] buffer = new byte[BPB_SecPerClus * BPB_BytesPerSec];
                file.seek(clusByte);
                file.read(buffer);
                file.close();
                for (byte b: buffer) {
                    list.add(b);
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(1);
            }
            clus = getNextClus(clus);
        } while (!((clus >= 268435448) && (clus <= 268435455)));
        return list;
    }
    private static long getSize(byte[] bt) {
        String size = String.format("%02X", bt[31]) + String.format("%02X", bt[30]) + String.format("%02X", bt[29]) + String.format("%02X", bt[28]);
        long actual = Long.parseLong(size, 16);
        return actual;
    }

    private static ArrayList < String > getStats(byte bt) {
        ArrayList < String > list = new ArrayList < > ();
        if ((bt & 32) > 0) {
            list.add("ATTR_ARCHIVE");
        }
        if ((bt & 16) > 0) {
            list.add("ATTR_DIRECTORY");
        }
        if ((bt & 8) > 0) {
            list.add("ATTR_VOLUME_ID");
        }
        if ((bt & 4) > 0) {
            list.add("ATTR_SYSTEM");
        }
        if ((bt & 2) > 0) {
            list.add("ATTR_HIDDEN");
        }
        if ((bt & 1) > 0) {
            list.add("ATTR_READ_ONLY");
        }
        return list;
    }
    private static String getClu(byte[] bt) {
        String next = String.format("%02X", bt[21]) + String.format("%02X", bt[20]) + String.format("%02X", bt[27]) + String.format("%02X", bt[26]);
        return next;
    }
    private static long getNextClus(long cluster) {
        long start = fatStart + (4 * cluster);
        byte[] buffer = new byte[4];
        try {
            RandomAccessFile file = new RandomAccessFile(filePath, "r");
            file.seek(start);
            file.read(buffer);
            file.close();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
        String clus = String.format("%02X", buffer[3]) + String.format("%02X", buffer[2]) + String.format("%02X", buffer[1]) + String.format("%02X", buffer[0]);
        long actual = Long.parseLong(clus, 16);
        return actual;

    }

    public static class Node {
        private HashMap < String, byte[] > map = new HashMap < > ();
        private String name;
        public Node(HashMap < String, byte[] > map, String name) {
            this.map = map;
            this.name = name;
        }
        public HashMap < String, byte[] > getMap() {
            return this.map;
        }
        public String getName() {
            return this.name;
        }
    }
}