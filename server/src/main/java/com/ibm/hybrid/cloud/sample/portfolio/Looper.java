/*
       Copyright 2017 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.portfolio;

//JSON Web Token (JWT) construction
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;

//CDI 1.2
import javax.inject.Inject;
import javax.enterprise.context.RequestScoped;

//JAX-RS 2.0  (JSR 339)
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;

//mpConfig 1.2
import org.eclipse.microprofile.config.inject.ConfigProperty;

//mpRestClient 1.0
import org.eclipse.microprofile.rest.client.inject.RestClient;


@ApplicationPath("/")
@Path("/")
@RequestScoped
/** Runs a set of Porfolio REST API calls in a loop. */
public class Looper extends Application {
	private static final String BASE_ID = "Looper";
	private static final String SYMBOL1 = "IBM";
	private static final String SYMBOL2 = "AAPL";
	private static final String SYMBOL3 = "GOOG";

	private @Inject @RestClient PortfolioClient portfolioClient;
	private @Inject @ConfigProperty(name = "JWT_AUDIENCE") String jwtAudience;
	private @Inject @ConfigProperty(name = "JWT_ISSUER") String jwtIssuer;

	public static void main(String[] args) {
		if (args.length == 1) try {
			Looper looper = new Looper();
			looper.loop(null, Integer.parseInt(args[0]));
		} catch (Throwable t) {
			t.printStackTrace();
		} else {
			System.out.println("Usage: Looper <count>");
		}
	}

    @GET
    @Path("/")
	@Produces("text/plain")
	public String loop(@QueryParam("id") String id, @QueryParam("count") Integer count) {
		StringBuffer response = new StringBuffer();

		try {
      if (id==null) id = BASE_ID;
			if (count==null) count=1; //isn't autoboxing cool?
	
			System.out.println("Entering looper, with ID: "+id+" and count: "+count);
	
			String jwt = "Bearer "+createJWT(id);
	
			System.out.println("Created a JWT");
			//System.out.println("JWT: "+jwt);
	
			long beginning = System.currentTimeMillis();
	
			for (int index=1; index<=count; index++) {
				if (count>1) { //only show if they asked for multiple iterations
					response.append("\nIteration #"+index+"\n");
				}
	
				long start = System.currentTimeMillis();
	
				response.append(iteration(id, jwt));
	
				long end = System.currentTimeMillis();
	
				response.append("Elapsed time for this iteration: "+(end-start)+" ms\n\n");
			}
	
			if (count>1) { //only show if they asked for multiple iterations
				long ending = System.currentTimeMillis();
				double average = ((double) (ending-beginning))/((double) count);
	
				response.append("Overall average time per iteration: "+average+" ms\n");
			}

			System.out.println("Exiting looper");
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return response.toString();
	}

    @GET
    @Path("/jwt")
	@Produces("text/plain")
	public String getjwt() {
		StringBuffer response = new StringBuffer();

		try {
			System.out.println("Entering jwt");
	
			String jwt = "Bearer "+createJWT("admin");
	
      response.append(jwt);
	
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return response.toString();
	}


	public StringBuffer iteration(String id, String jwt) {
		StringBuffer response = new StringBuffer();

		response.append("0:  DELETE /portfolio/"+id+"\n"+
			portfolioClient.deletePortfolio(jwt, id)+"\n\n"); //Remove this portfolio

		response.append("1:  GET /portfolio\n"+
			portfolioClient.getPortfolios(jwt)+"\n\n"); //Summary of all portfolios

		response.append("2:  POST /portfolio/"+id+"\n"+
			portfolioClient.createPortfolio(jwt, id)+"\n\n"); //Create a new portfolio

		response.append("3:  PUT /portfolio/"+id+"?symbol="+SYMBOL1+"&shares=1000\n"+
			portfolioClient.updatePortfolio(jwt, id, SYMBOL1, 1)+"\n\n"); //Buy stock for this portfolio

		response.append("4:  PUT /portfolio/"+id+"?symbol="+SYMBOL2+"&shares=2\n"+
			portfolioClient.updatePortfolio(jwt, id, SYMBOL2, 2)+"\n\n"); //Buy stock for this portfolio

		response.append("5:  PUT /portfolio/"+id+"?symbol="+SYMBOL3+"&shares=3\n"+
			portfolioClient.updatePortfolio(jwt, id, SYMBOL3, 3)+"\n\n"); //Buy stock for this portfolio

		response.append("6:  GET /portfolio/"+id+"\n"+
			portfolioClient.getPortfolio(jwt, id)+"\n\n"); //Get details of this portfolio

		response.append("7:  GET /portfolio\n"+
			portfolioClient.getPortfolios(jwt)+"\n\n"); //Summary of all portfolios, to see results

		response.append("8:  PUT /portfolio/"+id+"?symbol="+SYMBOL1+"&shares=6000\n"+
			portfolioClient.updatePortfolio(jwt, id, SYMBOL1, 6)+"\n\n"); //Buy more of this stock for this portfolio

		response.append("9:  PUT /portfolio/"+id+"?symbol="+SYMBOL3+"&shares=-32\n"+
			portfolioClient.updatePortfolio(jwt, id, SYMBOL3, -3)+"\n\n"); //Sell all of this stock for this portfolio

		response.append("10: GET /portfolio/"+id+"\n"+
			portfolioClient.getPortfolio(jwt, id)+"\n\n"); //Get details of this portfolio again

		response.append("11: DELETE /portfolio/"+id+"\n"+
			portfolioClient.deletePortfolio(jwt, id)+"\n\n"); //Remove this portfolio

		response.append("12: GET /portfolio\n"+
			portfolioClient.getPortfolios(jwt)+"\n\n"); //Summary of all portfolios, to see back to beginning

		return response;
	}

	/**
	 * Create Json Web Token.
	 * return: the base64 encoded and signed token. 
	 */
	private String createJWT(String userName) {
		String jwtTokenString = null;

		try {
			// create() uses default settings.  
			// For other settings, specify a JWTBuilder element in server.xml
			// and call create(builder id)
			JwtBuilder builder = JwtBuilder.create();

			if (userName == null) userName = "null";

			// Put the user info into a JWT Token
			builder.subject(userName);
			builder.claim("upn", userName);

			// Set the audience to our sample's value
			builder.claim("aud", jwtAudience);

			//builder.claim("groups", groups);

			//convention is the issuer is the url, but for demo portability a fixed value is used.
			//builder.claim("iss", request.getRequestURL().toString());
			builder.claim("iss", jwtIssuer);

			JwtToken theToken = builder.buildJwt();			
			jwtTokenString = theToken.compact();
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException(t);
		}

		return jwtTokenString;
	}
}
