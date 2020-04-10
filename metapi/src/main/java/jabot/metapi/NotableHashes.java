package jabot.metapi;

/** Notable hash digests calculated peover the contents */
public interface NotableHashes extends NotableKeys {
	static final String SHA1 = "SHA-1";
	static final String MURMURHASH3 = "MurmurHash3";
	static final String SIPHASH = "SipHash";
}
