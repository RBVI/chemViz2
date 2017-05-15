package edu.ucsf.rbvi.chemViz2.internal.model;

public class HTMLObject {
	String html;
	String innerHTML;

	public HTMLObject() {
		html = null;
		innerHTML = null;
	}

	public HTMLObject(String innerHTML) {
		this.innerHTML = innerHTML;
		this.html = htmlWrap(innerHTML);
	}

	public String getInnerHTML() { return innerHTML; }
	public void setInnerHTML(String innerHTML) { 
		this.innerHTML = innerHTML; 
		this.html = htmlWrap(innerHTML);
	}
	public String getHTML() { return html; }
	public void setHTML(String html) { 
		this.html = html; 
	}

	public String toString() { return html; }

	String htmlWrap(String innerHTML) {
		if (innerHTML == null)
			return null;
		if (innerHTML.startsWith("<html>"))
			return innerHTML;
		return "<html>"+innerHTML+"</html>";
	}

}
