package ex5.mx;

import java.util.Properties;

import java.io.IOException;
import java.io.Closeable;

import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

import javax.mail.event.MessageCountListener;
import javax.mail.event.MessageCountEvent;

import com.sun.mail.imap.IMAPFolder;

import ex5.utils.Pushable;

/**
 * Wrapper for IMAP operations
 */
public class IMAP implements  Closeable {

  private final Properties  CONFIG;  
  private final Session     SESSION;
  private final Store       STORE;
  private final Folder      FOLDER;

  private boolean   pollThreadStarted = false;

  /**
   * Instantiates a new IMAP session.
   *
   * @param _p the _p
   * @throws NoSuchProviderException the no such provider exception
   * @throws MessagingException the messaging exception
   */
  public IMAP(Properties _p) throws NoSuchProviderException, MessagingException {
    CONFIG = _p;
    SESSION = Session.getDefaultInstance(_p);
    STORE = SESSION.getStore(_p.getProperty("mail.store.protocol"));

    STORE.connect(CONFIG.getProperty("mail.imap.host"), 
                  CONFIG.getProperty("mail.user"),
                  CONFIG.getProperty("mail.password"));

    FOLDER = (IMAPFolder) STORE.getFolder("INBOX");
    if (!FOLDER.isOpen()) FOLDER.open(Folder.READ_WRITE);

  }

  /**
   * Gets the message specified by _msgnum
   *
   * @param _msgnum the number of the message to fetch
   * @return the message (Properties repr)
   */
  public Properties getMessage(int _msgnum) {
    return getMessage(_msgnum, "inbox");
  }

  /**
   * Gets the message.
   *
   * @param _msgnum the _msgnum
   * @param _folder the _folder
   * @return the message
   */
  public Properties getMessage(int _msgnum, String _folder) {
    try {
      Properties msg = getMessage(FOLDER.getMessage(_msgnum));
      return msg;
    } catch (MessagingException e) {
      return null;
    }
  }

  /**
   * Get a Properties representation of a message from a Message object.
   *
   * @param _message the _message
   * @return the message
   */
  public Properties getMessage(Message _message) {
    try {
      InternetAddress sender = (InternetAddress) _message.getFrom()[0];

      String name = (sender.getPersonal() != null) ? sender.getPersonal() 
                                                   : sender.getAddress().split("@")[0];

      String subject = (_message.getSubject() != null) ? _message.getSubject()
                                                       : "<NO SUBJECT>";

      String content;
      try {
        content = getText(_message);
      } catch (IOException e) {
        content = "<ERROR READING SUBJECT>";
      }

      Properties p = new Properties();
      p.setProperty("sender.name", name);
      p.setProperty("sender.email", sender.getAddress());
      p.setProperty("email.subject", subject);
      p.setProperty("email.contentType", _message.getContentType());
      p.setProperty("email.content", content);
      p.setProperty("email.id", Integer.toString(_message.getMessageNumber()));
      return p;
    } catch (MessagingException e) {
      return null;
    }
  }

  /**
   * Gets 20 messages and pushes them asynchronously onto a pushable object.
   *
   * @param _to the _to
   * @return the messages
   */
  public void getMessages(Pushable<Properties> _to) {
    getMessages(_to, "inbox", 20);
  }

  /**
   * Fetches messages and pushes them asynchronously onto a pushable object.
   *
   * @param _to where to push the messages
   * @param _folder which IMAP folder to get messages from
   * @param _count the number of messages to fetch
   */
  public void getMessages(Pushable<Properties> _to, String _folder, int _count) {
    try {
      Message[] messages = FOLDER.getMessages(FOLDER.getMessageCount() - _count, FOLDER.getMessageCount());
      for (int i = messages.length - 1; i > -1; i--)
        _to.push(getMessage(messages[i]));
    } catch (MessagingException e) {
    
    }
  }

  /**
   * This originally from JavaMail FAQs
   * http://www.oracle.com/technetwork/java/javamail/faq/index.html#mainbody
   *
   * @param p a message part
   * @return the text contained within the message
   * @throws MessagingException the messaging exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private String getText(Part p) throws
              MessagingException, IOException {
    if (p.isMimeType("text/*"))
      return (String)p.getContent();

    if (p.isMimeType("multipart/alternative")) {
      // prefer html text over plain text
      Multipart mp = (Multipart)p.getContent();
      String text = null;
      for (int i = 0; i < mp.getCount(); i++) {
          Part bp = mp.getBodyPart(i);
          if (bp.isMimeType("text/plain")) {
            if (text == null)
              text = getText(bp);
            continue;
          } else if (bp.isMimeType("text/html")) {
            String s = getText(bp);
            if (s != null)
              return s;
          } else {
            return getText(bp);
          }
      }
      return text;
    } else if (p.isMimeType("multipart/*")) {
      Multipart mp = (Multipart)p.getContent();
      for (int i = 0; i < mp.getCount(); i++) {
        String s = getText(mp.getBodyPart(i));
        if (s != null)
          return s;
      }
    }

    return null;
  }

  /**
   * Close.
   */
  public void close() {
    try {
      if (FOLDER != null) FOLDER.close(false);
      if (STORE != null) STORE.close();
    } catch (Exception e) {
      // Don't worry, be happy
    }
  }

  private class PollThread implements Runnable {
    public void run() {
      while (FOLDER.isOpen()) {
        try {
          Thread.sleep(5000);
          FOLDER.getMessageCount();
        } catch (MessagingException e) {

        } catch (InterruptedException e) {

        }
      }
    }
  }

  public void addMessageCountListener(final Pushable<Properties> _push) {
    if (!pollThreadStarted) {
      pollThreadStarted = true;
      new Thread(new PollThread()).start();
    }

    FOLDER.addMessageCountListener(new MessageCountListener() {

      public void messagesAdded(MessageCountEvent e) {
        _push.put(getMessage(e.getMessages()[0]), 0);
      }

      public void messagesRemoved(MessageCountEvent e) {

      }
      
    });
  }

}
