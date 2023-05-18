package Model;

public enum Language {
	ES("Español"),
	CA("Catalan"),
	EN("English"),
	FR("French"),
	IT("Italian"),
	DE("German"),
	PT("Portuguese"),
	DA("Danish"),
	HU("Hungarian"),
	HR("Croatian");

	private String message;

	Language (String message){
		this.message = message;
	}

	@Override
	public String toString(){
		return message;
	}
}
