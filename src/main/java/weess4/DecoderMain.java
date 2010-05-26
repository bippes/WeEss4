package weess4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

public class DecoderMain {
	private static final int AVI_XOR_MASK = 170;

	private URL loadFile(String path) {
		URL resource = this.getClass().getClassLoader().getResource(path);

		System.out.println("URL: " + resource);

		return resource;
	}

	private void decodeAvi(String inputFile, String outputFile, int encodedLength) throws Exception {
	    File inFile = new File(inputFile);
	    System.out.println("In : " + inFile.getAbsolutePath());
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

		System.out.println("\r\nDone.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DecoderMain main = new DecoderMain();

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

}
