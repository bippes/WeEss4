package weess4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class DecoderMainV {
	private static final int AVI_XOR_MASK = 170;
	
	private static final Map<Integer, FileStructure> FILE_STRUCTURES = new HashMap<Integer, FileStructure>();
	
	static {
		FILE_STRUCTURES.put(2007, new FileStructure(500000, 200, 0x18, 0x7DFA6C01, 4, 0x20, 0));
	}

	private URL loadFile(String path) {
		URL resource = this.getClass().getClassLoader().getResource(path);

		System.out.println("URL: " + resource);

		return resource;
	}
	
	private int readInt(File inFile, long position, int xor) throws Exception {
		int ret = 0;
		
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(inFile, "r");
			
			raf.seek(position);
			
			byte[] recordBuffer = new byte[4];
			ByteBuffer record = ByteBuffer.wrap(recordBuffer);
			record.order(ByteOrder.LITTLE_ENDIAN);
			
			raf.read(recordBuffer);
			
			if (xor > 0) {
				byte[] byteArray = record.array();
				
				for (int i = 0; i < byteArray.length; i++) {
					byte b = byteArray[i];
					
					int xorIndex = byteArray.length - 1 - i;
					
					long x = ((long)0xFF << (xorIndex * 8));

					long xorb = xor & x;
					
					long bshift = 8 * xorIndex;
					long xorbb = xorb >> bshift;
					
					b = (byte) (b ^ xorbb);
					
					byteArray[i] = b;
				}
			}
			
			IntBuffer intRecordBuffer = record.asIntBuffer();
	
			ret = intRecordBuffer.get();
			
		}
		finally {
			if (raf != null)
				raf.close();
		}
			
		return ret;
	}

	private void decodeAvi(String inputFile, String outputFile, int encodedLength) throws Exception {
	    File inFile = new File(inputFile);
	    long fileSize = inFile.length();
	    System.out.println("In : " + inFile.getAbsolutePath() + ", Size: " + fileSize);

	    int version = readInt(inFile, 0x00000000C, 0);
	    
	    System.out.println("Version: " + version);

	    FileStructure fileStructure = FILE_STRUCTURES.get(version);
	    
	    if (fileStructure != null) {
	    	int vid = readInt(inFile, fileStructure.getVidL(), fileStructure.getVidX());
	    	System.out.println("Vid: " + vid);
	    	
	    	int mcr = readInt(inFile, fileStructure.getMcrL(), fileStructure.getMcrX());
	    	System.out.println("Mcr: " + mcr);
	    	
	    	long vids = fileSize - mcr - vid - fileStructure.getLenM();
	    	System.out.println("Vids: " + vids);
	    	
	    	RandomAccessFile raf = null;
	    	try {
	    		raf = new RandomAccessFile(inFile, "r");
	    		
	    		raf.seek(vids);

	    		File outFile = new File(outputFile);
	    		FileOutputStream fout = new FileOutputStream(outFile);
	    		System.out.println("Out: " + outFile.getAbsolutePath());
	    		
	    		final int bufLen = 8192;
	    		boolean useXor = false;
	    		int length = (int) inFile.length();

	    		byte[] buf = new byte[bufLen];
	    		boolean first = true;
	    		int totalRead = 0;

	    		while (totalRead < length) {
	    			int readChars = raf.read(buf);

	    			// Do not exceed _encodedLength_.
	    			if (totalRead + readChars > length) {
	    			    readChars = length - totalRead;
	    			}

	    			if (readChars > -1) {
	    				if (first) {
	    					if (buf[0] == 'V' && buf[1] == 'S' && buf[2] == 'P') {
	    						useXor = true;

	    						System.out.println("Using XOR to decode file.");
	    					}
	    				}

	    				if (useXor && totalRead <= encodedLength) {
	    					for (int i = 0; i < readChars && totalRead + i < encodedLength; i++) {
	    						buf[i] = (byte) (((int) buf[i]) ^ fileStructure.getAviX());
	    					}
	    				}

	    				if (first) {
	    					if (buf[3] == 'F') {
	    						buf[0] = 'R';
	    						buf[1] = 'I';
	    						buf[2] = 'F';

	    						System.out.println("Found RIFF Header.");
	    					}
	    					else {
	    						buf[0] = (char) 0;
	    						buf[1] = (char) 0;
	    						buf[2] = (char) 1;

	    						System.out.println("Not using RIFF Header.");
	    					}

	    					// Read the length
	    					int l = (int) buf[4] & 0xFF;
	    					l += ((int) buf[5] & 0xFF) << 8;
	                        l += ((int) buf[6] & 0xFF) << 16;
	                        l += ((int) buf[7] & 0xFF) << 24;
	                        l += 8;

	    					System.out.println("Read length: " + l);

	    					length = l;

	    					if (buf[112] == 'v' && buf[113] == 's' && buf[114] == 'p' && buf[115] == 'x') {
	    						buf[112] = 'd';
	    						buf[113] = 'i';
	    						buf[114] = 'v';
	    						buf[115] = 'x';

	    						System.out.println("Found vspx.");
	    					}
	    					if (buf[188] == 'V' && buf[189] == 'S' && buf[190] == 'P' && buf[191] == 'X') {
	    						buf[188] = 'D';
	    						buf[189] = 'I';
	    						buf[190] = 'V';
	    						buf[191] = 'X';

	    						System.out.println("Found VSPX.");
	    					}

	    					first = false;
	    				}

	    				fout.write(buf, 0, readChars);

	    				totalRead += readChars;
	    			}
	    		}

	    		fout.close();
	    	}
	    	finally {
	    		if (raf != null)
	    			raf.close();
	    	}
	    }
	    /*
		FileInputStream fin = new FileInputStream(inFile);

		File outFile = new File(outputFile);
		System.out.println("Out: " + outFile.getAbsolutePath());
		FileOutputStream fout = new FileOutputStream(outFile);

		final int bufLen = 8192;
		boolean useXor = false;
		int length = (int) inFile.length();

		byte[] buf = new byte[bufLen];
		boolean first = true;
		int totalRead = 0;

		while (totalRead < length) {
			int readChars = fin.read(buf, 0, bufLen);

			// Do not exceed _encodedLength_.
			if (totalRead + readChars > length) {
			    readChars = length - totalRead;
			}

			if (readChars > -1) {
				if (first) {
					if (buf[0] == 'V' && buf[1] == 'S' && buf[2] == 'P') {
						useXor = true;

						System.out.println("Using XOR to decode file.");
					}
				}

				if (useXor && totalRead <= encodedLength) {
					for (int i = 0; i < readChars && totalRead + i < encodedLength; i++) {
						buf[i] = (byte) (((int) buf[i]) ^ AVI_XOR_MASK);
					}
				}

				if (first) {
					if (buf[3] == 'F') {
						buf[0] = 'R';
						buf[1] = 'I';
						buf[2] = 'F';

						System.out.println("Found RIFF Header.");
					}
					else {
						buf[0] = (char) 0;
						buf[1] = (char) 0;
						buf[2] = (char) 1;

						System.out.println("Not using RIFF Header.");
					}

					// Read the length
					int l = (int) buf[4] & 0xFF;
					l += ((int) buf[5] & 0xFF) << 8;
                    l += ((int) buf[6] & 0xFF) << 16;
                    l += ((int) buf[7] & 0xFF) << 24;
                    l += 8;

					System.out.println("Read length: " + l);

					length = l;

					if (buf[112] == 'v' && buf[113] == 's' && buf[114] == 'p' && buf[115] == 'x') {
						buf[112] = 'd';
						buf[113] = 'i';
						buf[114] = 'v';
						buf[115] = 'x';

						System.out.println("Found vspx.");
					}
					if (buf[188] == 'V' && buf[189] == 'S' && buf[190] == 'P' && buf[191] == 'X') {
						buf[188] = 'D';
						buf[189] = 'I';
						buf[190] = 'V';
						buf[191] = 'X';

						System.out.println("Found VSPX.");
					}

					first = false;
				}

				fout.write(buf, 0, readChars);

				totalRead += readChars;
			}
		}

		fin.close();
		fout.close();
		*/

		System.out.println("\r\nDone.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DecoderMainV main = new DecoderMainV();

		if (args.length != 2) {
			System.out.println("Syntax error, expecting two parameters: <inputFile> <outputFile>");
			System.exit(1);
		}

		String inputFile = args[0];
		String outputFile = args[1];

		try {
            main.decodeAvi(inputFile, outputFile, 1500000);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class FileStructure {

		public FileStructure(long aviL, int aviX, long vidL, int vidX,
				int lenM, long mcrL, int mcrX) {
			super();
			this.aviL = aviL;
			this.aviX = aviX;
			this.vidL = vidL;
			this.vidX = vidX;
			this.lenM = lenM;
			this.mcrL = mcrL;
			this.mcrX = mcrX;
		}
		
		private long aviL;
		private int aviX;
		private long vidL;
		private int vidX;
		private int lenM;
		private long mcrL;
		private int mcrX;
		
		public long getAviL() {
			return aviL;
		}
		public int getAviX() {
			return aviX;
		}
		public long getVidL() {
			return vidL;
		}
		public int getVidX() {
			return vidX;
		}
		public int getLenM() {
			return lenM;
		}
		public long getMcrL() {
			return mcrL;
		}
		public int getMcrX() {
			return mcrX;
		}

		
	}
	
}