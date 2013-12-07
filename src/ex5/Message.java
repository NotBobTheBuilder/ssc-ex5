package ex5;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.AuthenticationFailedException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import ex5.mx.SMTP;

/**
 * Servlet implementation class Message
 */
@WebServlet("/messages")
public class Message extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Message() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SMTP auth = checkAuth(request, response);
		if (auth == null)
			return;
		
		send(response, 200, "");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	private void send(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getOutputStream().println(message);
	}
	
	private void send(HttpServletResponse response, String message) throws IOException {
		response.getOutputStream().println(message);
	}

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
	
	private Properties getSettings(String username, String password) {
		Properties p = new Properties();
	
		p.setProperty("mail.user", username);
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
