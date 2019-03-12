public enum Status {
	_100("100 Continue"), //
	_200("200 OK"), //
	_201("201 Created"), //
	_202("202 Accepted"), //
	_204("204 No Content"), //
	_304("304 Not Modified"), //
	_400("400 Bad Request"), //
	_401("401 Unauthorized"), //
	_403("403 Forbidden"), //
	_404("404 Not Found"), //
	_408("408 Request Time-out"), //
	_414("414 Request-URI Too Large"), //
	_500("500 Internal Server Error"), //
	_501("501 Not Implemented"), //
	_502("502 Bad Gateway"); //

	private final String status;

	Status(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return status;
	}
}