package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class LooperController implements Runnable {
	private int iteration = 0;
	private int count = 0;
	private String auth = null;
	private String fullURL = null;
	private static int completed = 0;

	public static void main(String[] args) {
		if (args.length == 3) try {
			System.out.print("BluePages w3id: ");
			String id = System.console().readLine();

			System.out.print("Password: ");
			String pwd = new String(System.console().readPassword());
			System.out.println();

			loop(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), id, pwd);
		} catch (Throwable t) {
			t.printStackTrace();
		} else {
			System.out.println("Usage:   loopctl <url> <count> <threads>");
			System.out.println("Example: loopctl http://looper-service:9080/looper 5000 20");
			System.out.println("Result:  Looper runs 100,000 times total (spread across 20 parallel threads)");
		}
	}

	public LooperController(String url, int index, int times, String id, String pwd) {
		fullURL = url+"?id=Looper"+index;
		iteration = index;
		count = times;
		String credentials = id+":"+pwd;
		auth = "Basic "+Base64.getEncoder().encodeToString(credentials.getBytes());
    System.out.println("AUTH: "+auth);
	}

	public static void loop(String url, int times, int threads, String id, String pwd) throws InterruptedException {
		for (int index=1; index<=threads; index++) {
			LooperController controller = new LooperController(url, index, times, id, pwd);
			Thread thread = new Thread(controller);
			thread.start(); //launch the run() method in a separate thread
		}

		//don't let the JVM exit until all the spawned threads complete
		while (completed<threads) Thread.sleep(1000);
	}

	private String invokeREST(String verb, String uri) throws IOException {
		URL url = new URL(uri);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod(verb);
		conn.setRequestProperty("Content-Type", "text/plain");
		conn.setRequestProperty("Authorization", auth);
		conn.setDoOutput(true);
		InputStream stream = conn.getInputStream();

		String response = stringFromStream(stream);

		stream.close();

		return response; //I use JsonStructure here so I can return a JsonObject or a JsonArray
	}

	private String stringFromStream(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder out = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			out.append(line+"\n");
		}
		return out.toString();
	}

	@Override
	public void run() {
		try {
			for (int index=1; index<=count; index++) {
				System.out.println("Thread #"+iteration+", iteration #"+index);

				long start = System.currentTimeMillis();

				System.out.println(invokeREST("GET", fullURL));

				long end = System.currentTimeMillis();

				System.out.println("Elapsed time for thread #"+iteration+", iteration #"+index+": "+(end-start)+" ms");
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		completed++;
	}
}
