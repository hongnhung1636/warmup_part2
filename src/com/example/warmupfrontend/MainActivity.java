package com.example.warmupfrontend;



import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {
	public final static String HTTP_RESPONSE = "com.example.WarmUpFrontEnd.MESSAGE";
	public static String messageToSend = "";
	public static String currentLoggedInUsername = "";
	public static View tempView = null;
	
	public static final Integer RESPONSE_SUCCESS = 1;
	public static final Integer RESPONSE_ERR_BAD_CREDENTIALS  = -1;
	public static final Integer RESPONSE_ERR_USER_EXISTS = -2;
	public static final Integer RESPONSE_ERR_BAD_USERNAME = -3;
	public static final Integer RESPONSE_ERR_BAD_PASSWORD = -4;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		currentLoggedInUsername = "";
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	
	public void onClick_Login(View view){
		messageToSend = "";
		currentLoggedInUsername = "";
		
		tempView = view; // saved here, so we don't have to pass it around to every func call
		// TODO: attach spinning loading bar icon
		startInternetConnection("/users/login");
		
	}
	
	public void onClick_AddNewUser(View view){
		messageToSend = "";
		currentLoggedInUsername = "";
		
		tempView = view; // saved here, so we don't have to pass it around to every func call
		startInternetConnection("/users/add");
		
		
	}
	
	public void startUpLoggedInActivity() {
		
		Intent intent = new Intent(this, LoggedInActivity.class);
		intent.putExtra(HTTP_RESPONSE, messageToSend);
		// note:These parameters cannot be passed by reference — only by value

	    startActivity(intent);
	}
	
	
	
	
	/* This will parse a json-formatted string
	 *  and will do the proper action for what it says!
	 * 
	 */
	public void parseResult(String st) {
		//ex/ '{"count": 23, "errCode": 1}';
		st = st.replace("{", ""); st = st.replace("}", "");
		st = st.replace(" ", ""); st = st.replace(",", "");
		st = st.replace(":", ""); st = st.substring(1); // last one cuts off first "
		
		String[] temp = st.split('"'+"");
		
		
		List<String> keys = new ArrayList<String>();
		List<String> vals = new ArrayList<String>();
		
		Boolean writeToKey = true;
		for (int i = 0; i < temp.length; i ++) {
			//System.out.println("--:"+temp[i]+":" );
			if (temp[i].toString() == "}" || temp[i].toString() == "{") {
				// do nothing
			} else {
				if (writeToKey) {
					keys.add(temp[i]);	
				} else {
					vals.add(temp[i]);
				}
				writeToKey = !writeToKey;
			}
		}
		
		// now decision time:
		Integer count = 0;
		Integer errortype = 0;
		String debugString = "";
		
		Iterator<String> keysIter = keys.iterator();
		Iterator<String> valsIter = vals.iterator();
		for( ; keysIter.hasNext() && valsIter.hasNext();) {
			String key = keysIter.next();
			String val = valsIter.next();
			debugString += key +"_"+val+"____";
			//debugString += (key.compareTo("errCode") == 0) +"__" +(Integer.parseInt(val) > 0);
			if (key.compareTo("errCode") == 0) {
				errortype = Integer.parseInt(val);
			} else if (key.compareTo("count") == 0) {
				count = Integer.parseInt(val);
			}
		}
		
		
		TextView messagetext = (TextView) findViewById(R.id.textView1);
		switch (errortype) {
			case 1: messageToSend = "Welcome "+currentLoggedInUsername+", you have been logged in "+count+" times.";
					startUpLoggedInActivity(); break;
			case -1: messagetext.setText("Error! Cannot find username/password in database"); break;
			case -2: messagetext.setText("Error! Username is taken"); break;
			case -3: messagetext.setText("Error! Bad Syntax for Username"); break;
			case -4: messagetext.setText("Error! Bad Syntax for Password"); break;
			
					
			default: messagetext.setText("Unknown error!!!  Debugging stats:"+debugString+"_:_"+keys.toString() + vals.toString());  break;
		

		}
		
		//return keys.toString() + vals.toString();
	}
	
	
	
	
	/////////
	/////////////////////
	/////  internet connection handler stuffs:
	/////////////////////
	/////////
	
	public void startInternetConnection(String path/*, String type, Map<String, String> data*/) {   
		// decided for now to have data retrieved later
		// in future, there will also be more params that will define what to do in case of success and error
		// but now, this warmup app doesn't require that much level of abstraction.
		
		String baseUrl = "http://glacial-savannah-7456.herokuapp.com";
		//String stringUrl = "http://glacial-savannah-7456.herokuapp.com/users/login";
		
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
	    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
	    
	    if (networkInfo != null && networkInfo.isConnected()) {
	        // connection is good
	    	// so now we start up an Async Task
	    	
	    	new StartUpAsyncTask().execute(baseUrl+path);
	    	
	    } else {
	    	// connection is bad!!!! display error
	    	
	    	TextView messagetext = (TextView) findViewById(R.id.textView1);
	    	messagetext.setText("Error!!  Cannot establish internet connection.  Please make sure your app is running as administrator or something like that...");

	    }
		
	}
	
	 private class StartUpAsyncTask extends AsyncTask<String, Void, String> {//AsyncTask {
       @Override
       protected String doInBackground(String... urls) {
             
           // params comes from the execute() call: params[0] is the url.
           try {
        	   
        	   EditText usertext = (EditText) findViewById(R.id.editUser);
        	   EditText passwordtext = (EditText) findViewById(R.id.editPassword);
        	   
        	   currentLoggedInUsername = usertext.getText().toString();
        	   
        	   Map<String, String> data = new HashMap<String, String>();
   			   data.put("user", currentLoggedInUsername);
   			   data.put("password", passwordtext.getText().toString());
   			
   			   return doPost(urls[0], data);
   			   //return "temp!!!";
   			
        	   
           }  catch (Exception name) {//catch (IOException e) {
        	   // this exception catching is not thorough, and in a real
        	   // app, with more dev time available, it will be done better.
               return "Exception occured with url name: "+name;
           }
       }
       // onPostExecute displays the results of the AsyncTask.
       @Override
       protected void onPostExecute(String result) {
    	   // here, the response is received, but it could be good (then transition)
    	   // or it could be bad, in which we stay here and update the message.
    	  
    	   parseResult(result);
    	   /// this class will determine what to do
    	   
    	   //startUpLoggedInActivity();
    	   
    	   
       }
	 } // end of 
	 
	 
	 /* This function will take the url and data and send a POST request.
	  * It will return (String) the response from the server.  If it can't get a response,
	  * then it throws an Exception.  Built from Tutorial found at: http://alien.dowling.edu/~vassil/tutorials/javapost.php
	  */
	 public static String doPost(String url, Map<String, String> data) throws Exception {
    	
    	
		URL siteUrl = new URL(url);
		
		HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
		//conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		
		Set<String> keys = data.keySet();
		Iterator<String> keyIter = keys.iterator();
		String content = "";
		
		for(int i=0; keyIter.hasNext(); i++) {
			Object key = keyIter.next();
			if(i!=0) {
				content += "&";
			}
			content += key + "=" + URLEncoder.encode(data.get(key), "UTF-8");
		}
		out.writeBytes(content);
		out.flush();
		out.close();

		
		//int responseCode = conn.getResponseCode();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = "";
		String ret = "";
		while((line=in.readLine())!=null) {
			ret += line;
		}
		return ret;
	 }
	 
	 
	 
	 /* Not used in this app, but here for reference
	  * 
	  */
	 public static String doGet(String url) throws Exception {
			URL siteUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			
			
			
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			String ret = "";
			while((line=in.readLine())!=null) {
				ret += line;
			}
			return ret;
		}
	

}
