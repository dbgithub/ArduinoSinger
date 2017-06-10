package es.deusto.arduinosinger.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Simple HTTP GET client implementation that works as a Thread
 * @author ivazquez
 *
 */
public class SimpleHttpClient extends Thread{
	
	private String _url;
	private String _content;
	private boolean _finished;
	
	private SimpleHttpClientListener _listener;
	
	/**
	 * @param url the url to retrieve
	 */
	public SimpleHttpClient(String url) {
		this._url = url;
		this._content = null;
		this._finished = false;
	}
	
	/**
	 * @return the content retrieved
	 */
	public String getContent(){
		return this._content;
	}
	
	/**
	 * @return true if the HTTP operation has finished
	 */
	public boolean isFinished(){
		return this._finished;
	}
	
	/**
	 * @param l the listener to receive the indication when the HTTP operation is over
	 */
	public void setListener(SimpleHttpClientListener l){
		this._listener = l;
	}
	
	// Threaded execution
	public void run (){
		doGet();
	}
	
	/**
	 * For non-threaded execution call this method directly
	 * @return the HTTP reply contents
	 */
	public String doGet(){
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(_url).openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.connect();
			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String content = "", line = null;
				while((line = br.readLine()) != null){
					content += line;
				}
				conn.disconnect();
				this._content = content;
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(conn != null)
			conn.disconnect();

		this._finished = true;
		if(this._listener != null)
			this._listener.resultAvailable(this._content);
		return this._content;
	}
	
}
