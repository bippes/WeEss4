package weess4;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

public class DecoderMain {
	private static final int AVI_XOR_MASK = 170;

	private URL loadFile(String path) {
		URL resource = this.getClass().getClassLoader().getResource(path);

		System.out.println("URL: " + resource);

		return resource;
	}

	private void decodeAvi(String inputFile, String outputFile) throws Exception {
		InputStreamReader inReader = new FileReader(inputFile);
		File outFile = new File(outputFile);
		System.out.println("Out: " + outFile.getAbsolutePath());
		OutputStreamWriter outWriter = new FileWriter(outFile);

		final int bufLen = 250000;
		boolean useXor = false;

		char[] buf = new char[bufLen];
		int readChars = inReader.read(buf, 0, bufLen);
		boolean first = true;

		if (readChars > -1) {
			if (first) {
				if (buf[0] == 'V' && buf[1] == 'S' && buf[2] == 'P') {
					useXor = true;
				}
			}

			if (useXor) {
				for (int i = 0; i < readChars; i++) {
					buf[i] = (char) (((int) buf[i]) ^ AVI_XOR_MASK);
				}
			}

			if (first) {
				if (buf[3] == 'F') {
					buf[0] = 'R';
					buf[1] = 'I';
					buf[2] = 'F';
				}
				else {
					buf[0] = (char) 0;
					buf[1] = (char) 0;
					buf[2] = (char) 1;
				}

				String sbuf = String.copyValueOf(buf, 0, readChars);
				sbuf = sbuf.replace("VSPX", "DIVX");
				sbuf = sbuf.replace("vidsvspx", "vidsdivx");
				sbuf.getChars(0, readChars, buf, 0);

				first = false;
			}

			outWriter.write(buf, 0, readChars);
		}

		inReader.close();
		outWriter.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DecoderMain main = new DecoderMain();

		try {
			URL url = main.loadFile("4/33.4");

			main.decodeAvi(url.getPath(), "33.avi");
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
