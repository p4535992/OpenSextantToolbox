package org.opensextant.tagger;

import java.util.HashMap;
import java.util.Map;

public class Match {

	protected long start;
	protected long end;
	protected String type = "";
	protected String matchText = "";
	protected Map<String, Object> features = new HashMap<String, Object>();

	public Match(String type, String text, int start, int end) {
		this.start = start;
		this.end = end;
		this.type = type;
		this.matchText = text;
	}

	public Match() {
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMatchText() {
		return matchText;
	}

	public void setMatchText(String matchText) {
		this.matchText = matchText;
	}

	public Map<String, Object> getFeatures() {
		return features;
	}

	public void setFeatures(Map<String, Object> features) {
		this.features = features;
	}

}
