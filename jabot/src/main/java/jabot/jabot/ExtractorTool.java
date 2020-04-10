package jabot.jabot;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import jabot.metapi.Metadata;
import jabot.metapi.MetadataExtractor;
import jabot.metika.TikaExtractor;

public class ExtractorTool {
	
	public static void main(final String [] args) {
		try {
			final MetadataExtractor extractor = new TikaExtractor();
			
			final java.io.File target = new java.io.File(args[0]);
			final Metadata meta = new Metadata();
			
			try(final InputStream in = new FileInputStream(target)) {
				extractor.extractMetadata(in, meta);
			};
			
			printMetadata(System.out, meta);
			
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void printMetadata(final PrintStream out, final Metadata meta) {
		for (final Map.Entry<String, List<String>> entry : meta.entrySet()) {
			out.print(entry.getKey());
			out.println(": [");
			for (final String value : entry.getValue()) {
				out.println(value);
				out.println();
			}
			out.println("]");
			out.println();
			out.println();
		}
	}
	

}
