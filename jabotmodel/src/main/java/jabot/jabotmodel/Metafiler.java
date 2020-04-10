package jabot.jabotmodel;

import java.io.IOException;
import java.net.URI;

import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;

public interface Metafiler {
	
	default File sample(URI target) throws IOException, ExtractionException {
		final Metadata meta = new Metadata();
		return sample(target, meta);
	}

	File sample(URI target, Metadata meta) throws IOException, ExtractionException;

}