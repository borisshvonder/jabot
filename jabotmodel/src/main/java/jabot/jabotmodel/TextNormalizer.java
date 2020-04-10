package jabot.jabotmodel;

import org.apache.commons.lang3.Validate;

public class TextNormalizer {
	private static TranslitTable TRANSLIT_TABLE = new TranslitTable(
		"абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
		"abvgdeezziiklmnoprstufhc4hh_i_eua"
	);
	
	/**
	 * Transliterates from non-english languages to english. Low quality, since each input char is replaced by one 
	 * output character, but pretty fast
	 */
	public static String transliterateFastToEnglish(final String text) {
		final StringBuilder transliterated = new StringBuilder(text.length());
		for (int i=0; i<text.length(); i++) {
			final char input = text.charAt(i);
			transliterated.append(TRANSLIT_TABLE.translit(input));
		}
		return transliterated.toString();
	}
	
	/**
	 * Replaces all non-alphanumeric characters with spaces and makes sure there are no spaces at front,
	 * at end, and no consequitive spaces
	 */
	public static String maximumNormalization(final String text) {
		final StringBuilder b = new StringBuilder(text.length());

		boolean delimeter = false;
		for (int i=0; i<text.length(); i++) {
			final char c = text.charAt(i);
			if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '.') {
				if (!delimeter) {
					delimeter = true;
					if (i>0) {
						b.append(' ');
					}
				}
			} else {
				delimeter = false;
				b.append(c);
			}
		}
		
		final int last = b.length()-1;
		if (b.length() > 0 && b.charAt(last) == ' ') {
			b.delete(last, last+1);
		}
		
		return b.toString();
	}

	private static final class TranslitTable {
		private final String from, to;
		
		public TranslitTable(final String ...translits) {
			Validate.isTrue(translits.length > 0, "translits should not be empty");
			Validate.isTrue(translits.length % 2 == 0, "translits should come in pairs");
			
			final int length = translits.length/2;
			
			final StringBuilder fromBuilder = new StringBuilder(2*length*translits[0].length());
			final StringBuilder toBuilder = new StringBuilder(2*fromBuilder.capacity());
			
			for (int i=0; i<length; i++) {
				final int k = i*2;
				final String fromPart = translits[k];
				final String toPart = translits[k+1];
				
				Validate.isTrue(fromPart.length() == toPart.length(), "transliteration pairs should have same size");
				
				fromBuilder.append(translits[k].toLowerCase());
				toBuilder.append(translits[k+1].toLowerCase());
				fromBuilder.append(translits[k].toUpperCase());
				toBuilder.append(translits[k+1].toUpperCase());
			}
			
			this.from = fromBuilder.toString();
			this.to = toBuilder.toString();
		}
		
		public char translit(final char input) {
			final int idx = from.indexOf(input);
			return idx < 0 ? input : to.charAt(idx);
		}
	}
}
