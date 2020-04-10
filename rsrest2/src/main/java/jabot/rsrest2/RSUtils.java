package jabot.rsrest2;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class RSUtils {
	static List<String> chopMessage(final String text, final int maxLength) {
		if (text.length() <= maxLength) {
			return Arrays.asList(text);
		} else {
			final List<String> ret = new LinkedList<>();

			int start = 0;
			int end = text.indexOf('\n');
			if (end<0 || end-start>maxLength) {
				end = start + maxLength;
				if (end > text.length()) {
					end = text.length();
				}
			}
			while (start<text.length()) {
				ret.add(text.substring(start, end));
				start = end;
				if (end<text.length() && text.charAt(end)=='\n') {
					start++;
				}
				end = text.indexOf('\n', start);
				if (end<0 || end-start>maxLength) {
					end = start + maxLength;
					if (end > text.length()) {
						end = text.length();
					}
				}
			}
			
			return ret;
		}
	}
}
