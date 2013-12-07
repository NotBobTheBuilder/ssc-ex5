package ex5.mx;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

/**
 * Wrapper for SMTP operations
 */
public class SMTP {

  private final Properties  CONFIG;
  private final Session     SESSION;
  private final Transport   TRANSPORT;

  /**
   * Instantiates a new SMTP wrapper.
   *
   * @param _p the _p
   * @throws NoSuchProviderException the no such provider exception
   * @throws MessagingException the messaging exception
   */
  public SMTP(Properties _p) throws NoSuchProviderException, MessagingException {
    CONFIG = _p;
    SESSION = Session.getDefaultInstance(CONFIG);
    TRANSPORT = SESSION.getTransport("smtp");
    TRANSPORT.connect(CONFIG.getProperty("mail.host"),
            CONFIG.getProperty("mail.user"),
            CONFIG.getProperty("mail.password"));
    TRANSPORT.close();
  }

  /**
   * Send an email over SMTP.
   *
   * @param _to recipient
   * @param _cc cc'd users
   * @param _bcc bcc'd users
   * @param _subject the subject
   * @param _file path to attachment
   * @param _message the message
   * @throws MessagingException the messaging exception
   */
  public void send(String _to, String _cc, String _bcc, String _subject,
                   String _file, String _message) throws MessagingException {

        MimeMessage message = new MimeMessage(SESSION);

        message.setFrom(new InternetAddress(CONFIG.getProperty("mail.address")));

        message.setRecipients(Message.RecipientType.TO, _to);
        if (_cc.length()  > 0) message.setRecipients(Message.RecipientType.CC, _cc);
        if (_bcc.length() > 0) message.setRecipients(Message.RecipientType.BCC, _bcc);

        message.setSubject(_subject);
        message.setText(_message);

        message.saveChanges();
        send(message);
  }

  /**
   * Send a message (in MimeMessage representation).
   *
   * @param _m the message to send
   * @throws MessagingException the messaging exception
   */
  public void send(MimeMessage _m) throws MessagingException {
    TRANSPORT.connect(CONFIG.getProperty("mail.host"),
                      CONFIG.getProperty("mail.user"),
                      CONFIG.getProperty("mail.password"));
    TRANSPORT.sendMessage(_m, _m.getAllRecipients());
    TRANSPORT.close();
  }
  
}
