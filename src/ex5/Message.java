package ex5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.AuthenticationFailedException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import ex5.mx.SMTP;

/**
 * Servlet implementation class Message.
 * Provides a JSON API for sending messages and checking credentials.
 * Uses HTTP Basic Authentication for credential handling
 */
@WebServlet("/messages")
public class Message extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
       
	/**
	 * Verify that sent credentials are correct
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SMTP auth = checkAuth(request, response);
		if (auth == null)
			return;
		// Appropriate invalid headers are already sent. Reaching here means we're OK.
		send(response, 200, "");
	}

	/**
	 * Handle a POST request - send an email.
	 *
	 * @param request Request object
	 * @param response Response object
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		/*
		 *  We parse a JSON object (using GSON API) with structure like so:
		 *  {
		 *    "message" : "message body",
		 *    "subject" : "subject",
		 *    "to" : ["recipient1@example.org", "recipient2@example.org"]
		 *  }
		 *  these are held in:
		 *  - String message
		 *  - String subject
		 *  - ArrayList<String> to
		 */
		JsonObject data = new JsonParser().parse(request.getReader()).getAsJsonObject();
		
		String message = data.get("message").getAsString();
		String subject = data.get("subject").getAsString();
		
		JsonArray json_to = data.get("to").getAsJsonArray();
		ArrayList<String> recipients = new ArrayList<String>();
		for (JsonElement i : json_to)
			recipients.add(i.getAsString());
		
		SMTP smtp = checkAuth(request, response);
		if (smtp == null)
			return;
		
		try {
			smtp.send(recipients.get(0), "", "", subject, "", message);
		} catch (MessagingException e) {
			send(response, 500, "Error sending message; try again");	
		}
		//If we made it this far, the message has been sent
		send(response, 202, "");
	}
	
	/**
	 * Send a HTTP status code and message
	 *
	 * @param response the HTTPServletResponse instance to respond with
	 * @param status HTTP status code to send
	 * @param message HTTP message body
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void send(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getOutputStream().println(message);
	}
	
	/**
	 * Attempt to construct an SMTP class for sending email. If this fails, send an appropriate HTTP error code & message
	 *
	 * @param request HTTP Servlet request, needed for credentials
	 * @param response HTTP Response object - used to send errors if needed.
	 * @return a working SMTP instance, or null (indicating an error has already been sent)
	 * @throws ServletException
	 * @throws IOException
	 */
	private SMTP checkAuth(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String auth = request.getHeader("Authorization");
		
		if (auth == null) {
			send(response, 401, "Authorisation Needed");
			return null;
		}
				
		String[] creds = auth.split(" ");
		if (!creds[0].equalsIgnoreCase("Basic")) {
			send(response, 501, "Only Basic authentication is available - " + creds[0] + " is not acceptable");
			return null;
		}
		
		try {
			creds[1] = new String(Base64.decode(creds[1]));
		} catch (Base64DecodingException e) {
			send(response, 400, "invalid base64 in authorization header");
			return null;
		}

		try {
			int split = creds[1].indexOf(":");
			if (split == -1) {
				send(response, 400, "malformed authorization header (missing colon)");
			}
			SMTP a = new SMTP(getSettings(creds[1].substring(0, split),
					          			  creds[1].substring(split + 1)));
			return a;
		} catch (AuthenticationFailedException e) {
			send(response, 401, "Authentication Failed");
		} catch (MessagingException e ) {
			send(response, 500, "Internal Server error");
		}
		return null;
	}
	
	/**
	 * Create a properties object for bham.ac.uk email using user credentials
	 *
	 * @param username - the username of the account to send from
	 * @param password - the password of the account to send from
	 * @return the settings
	 */
	private Properties getSettings(String username, String password) {
		Properties p = new Properties();
	
		p.setProperty("mail.user", username);
		p.setProperty("mail.address", username + "@bham.ac.uk");
		p.setProperty("mail.password", password);
		p.setProperty("mail.smtp.host", "auth-smtp.bham.ac.uk");
		p.setProperty("mail.smtp.auth", "true");
		p.setProperty("mail.smtp.port", "465");
		p.setProperty("mail.smtp.socketFactory.port", "465");
		p.setProperty("mail.smtp.socketFactory.class",
	                   "javax.net.ssl.SSLSocketFactory");
		return p;
	}
	
}
