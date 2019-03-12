
public enum ContentType {
	
	HTML("HTML");

	private final String extension;

	ContentType(String extension) {
		this.extension = extension;
	}
	
	@Override
	public String toString() {
		switch (this) {
			case HTML:
				return "Content-Type: text/html";
			default:
				return null;
		}
	}

	public String getExtension() {
		return extension;
	}
}
