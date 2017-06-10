package es.deusto.arduinosinger.utils;

/**
 * Interface to receive notifications about HTTP operations from {@link es.deusto.arduinosinger.utils.SimpleHttpClient}
 * @author ivazquez
 */
public interface SimpleHttpClientListener{
	/**
	 * @param result the contents for the HTTP operation, null if error
	 */
	public void resultAvailable(String result);
}

