package systems.dmx.sendmail;

import systems.dmx.sendmail.util.SendgridWebApiV3;
import systems.dmx.sendmail.util.SendgridMail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;
import org.apache.commons.mail.HtmlEmail;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.util.JavaUtils;

/**
 * 
 * @author Malte Reißig <malte@dmx.berlin>
 */
public class SendmailPlugin extends PluginActivator implements SendmailService {

    private static Logger log = Logger.getLogger(SendmailPlugin.class.getName());

    // Sender Information
    private String SYSTEM_FROM_NAME = null; // DMX Sendmail | Your Name
    private String SYSTEM_FROM_MAILBOX = null; // dmx@localhost | Your Email Address
    // Recipient Information
    private String SYSTEM_ADMIN_MAILBOX = null; // root@localhost | Your Admin's Email Address
    // Plugin Configuration
    private String SENDMAIL_TYPE = null; // smtp | sendgrid

    // SMTP Configuration
    private String SMTP_HOST = null; // localhost | ip/hostname  
    private String SMTP_USERNAME = null; // empty | username
    private String SMTP_PASSWORD = null; // empty | password
    private int SMTP_PORT = -1; // 25 | port
    private String SMTP_SECURITY = null; // empty | tls | smtps
    private boolean SMTP_DEBUG = false; // false | true
    // Sendgrid API Configuration
    private String SENDGRID_API_KEY = null; // empty

    private boolean GREETING_ENABLED = false;

    private String GREETING_SUBJECT;
    private final String DEFAULT_GREETING_SUBJECT = "Sendmail Plugin Activated";
    private String GREETING_MESSAGE;
    private String GREETING_HTML_MESSAGE;

    private final String DEFAULT_GREETING_MESSAGE = "Hello dear, this is your new email sending service.\n\nWe hope you can enjoy the comforts!";

    @Override
    public void init() {
        try {
            loadPluginPropertiesConfig();
            // Test the service and our configuration
            log.info("Sending test mail per " + SENDMAIL_TYPE + " on init to \"" + SYSTEM_ADMIN_MAILBOX + "\"");
            if (GREETING_ENABLED) {
                doEmailSystemMailbox(GREETING_SUBJECT, GREETING_MESSAGE, GREETING_HTML_MESSAGE);
            }
        } catch (IOException ex) {
            Logger.getLogger(SendmailPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadPluginPropertiesConfig() throws IOException {
        String sendmailType = System.getProperty("dmx.sendmail.type");
        SENDMAIL_TYPE = (sendmailType == null) ? "smtp" : sendmailType.trim();
        String fromName = System.getProperty("dmx.sendmail.system_from_name");
        SYSTEM_FROM_NAME = (fromName == null) ? "DMX Sendmail" : fromName.trim();
        String fromMailbox = System.getProperty("dmx.sendmail.system_from_mailbox");
        SYSTEM_FROM_MAILBOX = (fromMailbox == null) ? "dmx@localhost" : fromMailbox.trim();
        String adminMailbox = System.getProperty("dmx.sendmail.system_admin_mailbox");
        SYSTEM_ADMIN_MAILBOX = (adminMailbox == null) ? "root@localhost" : adminMailbox.trim();
        log.info("\n\tdmx.sendmail.system_from_name: " + SYSTEM_FROM_NAME + "\n"
            + "\tdmx.sendmail.system_from_mailbox: " + SYSTEM_FROM_MAILBOX + "\n"
            + "\tdmx.sendmail.system_admin_mailbox: " + SYSTEM_ADMIN_MAILBOX + "\n"
            + "\tdmx.sendmail.type: " + SENDMAIL_TYPE);

        GREETING_ENABLED = Boolean.parseBoolean(System.getProperty("dmx.sendmail.greeting_enabled", "false"));
        log.info("\n\tdmx.sendmail.greeting_enabled: " + GREETING_ENABLED);
        GREETING_SUBJECT = System.getProperty("dmx.sendmail.greeting_subject", DEFAULT_GREETING_SUBJECT);
        log.info("\n\tdmx.sendmail.greeting_subject: " + (GREETING_SUBJECT.equals(DEFAULT_GREETING_SUBJECT) ? "<built-in subject>" : "<custom subject>"));
        GREETING_MESSAGE = System.getProperty("dmx.sendmail.greeting_message", DEFAULT_GREETING_MESSAGE);
        log.info("\n\tdmx.sendmail.greeting_message: " + (GREETING_MESSAGE.equals(DEFAULT_GREETING_MESSAGE) ? "<built-in message>" : "<custom message>"));
        GREETING_HTML_MESSAGE = System.getProperty("dmx.sendmail.greeting_html_message", null);
        log.info("\n\tdmx.sendmail.greeting_html_message: " + (GREETING_HTML_MESSAGE == null ? "<not explicitly set>" : "<custom message>"));

        String smtpHostName = System.getProperty("dmx.sendmail.smtp_host");
        SMTP_HOST = (smtpHostName == null) ? "localhost" : smtpHostName.trim();
        String smtpUsername = System.getProperty("dmx.sendmail.smtp_username");
        SMTP_USERNAME = (smtpUsername == null) ? "" : smtpUsername.trim();
        String passwd = System.getProperty("dmx.sendmail.smtp_password");
        SMTP_PASSWORD = (passwd == null) ? "" : passwd;
        String smtpPort = System.getProperty("dmx.sendmail.smtp_port");
        SMTP_PORT = (smtpPort == null) ? 25 : Integer.parseInt(smtpPort);
        String smtpSecurity = System.getProperty("dmx.sendmail.smtp_security");
        SMTP_SECURITY = (smtpSecurity == null) ? "" : smtpSecurity.trim();
        String smtpDebug = System.getProperty("dmx.sendmail.smtp_debug");
        SMTP_DEBUG = (smtpDebug == null) ? SMTP_DEBUG : Boolean.parseBoolean(smtpDebug);
        if (SENDMAIL_TYPE.toLowerCase().equals("smtp")) {
            log.info("\n\tdmx.sendmail.smtp_host: " + SMTP_HOST + "\n"
                + "\tdmx.sendmail.smtp_username: " + SMTP_USERNAME + "\n"
                + "\tdmx.sendmail.smtp_password: PASSWORD HIDDEN FOR LOG" + "\n"
                + "\tdmx.sendmail.smtp_port: " + SMTP_PORT + "\n"
                + "\tdmx.sendmail.smtp_security: " + SMTP_SECURITY + "\n"
                + "\tdmx.sendmail.smtp_debug: " + SMTP_DEBUG);
        } else if (SENDMAIL_TYPE.toLowerCase().equals("sendgrid")) {
            SENDGRID_API_KEY = System.getProperty("dmx.sendmail.sendgrid_api_key");
            if (SENDGRID_API_KEY.isEmpty()) {
                log.severe("Configuration Error: DMX Sendmail is configured to send mails via "
                        + "Sendgrid API but has no\"dmx.sendmail.sendgrid_api_key\" value set");
            } else {
                log.info("dmx.sendmail.sendgrid_api_key: API KEY HIDDEN FOR LOG");
            }
        } else {
            log.severe("Configuration Error: DMX Sendmail has an invalid \"dmx.sendmail.type\" value set");
        }
    }

    @Override
    public void doEmailUser(String username, String subject, String message, String htmlMessage) {
        String userMailbox = dmx.getPrivilegedAccess().getEmailAddress(username);
        if (userMailbox != null) {
            sendMailTo(userMailbox, subject, message, htmlMessage);
        } else {
            log.severe("Sending email notification to user not possible, \""
                    +username+"\" has not signed-up with an Email Address");
        }
    }

    @Override
    public void doEmailUser(String fromUsername, String toUsername, String subject, String message, String htmlMessage) {
        String senderMailbox = dmx.getPrivilegedAccess().getEmailAddress(toUsername);
        String recipientMailbox = dmx.getPrivilegedAccess().getEmailAddress(toUsername);
        if (recipientMailbox != null && senderMailbox != null) {
            sendMailFromTo(senderMailbox, fromUsername, recipientMailbox, toUsername, subject, message, htmlMessage);
        } else {
            log.severe("Sending email notification to user not possible. Either \""
                    +toUsername+"\" or \"" + fromUsername + "\" has not signed-up with an Email Address");
        }
    }

    @Override
    public void doEmailRecipientAs(String from, String fromName,
            String subject, String message, String htmlMessage, String recipientMail) {
        sendMailFromTo(from, fromName, recipientMail, null, subject, message, htmlMessage);
    }

    @Override
    public void doEmailRecipient(String subject, String message, String htmlMessage, String recipientMail) {
        sendMailTo(recipientMail, subject, message, htmlMessage);
    }

    @Override
    public void doEmailSystemMailbox(String subject, String message, String htmlMessage) {
        sendMailTo(SYSTEM_ADMIN_MAILBOX, subject, message, htmlMessage);
    }

    private void sendMailTo(String recipient, String subject, String textMessage, String htmlMessage) {
        sendMailFromTo(SYSTEM_FROM_MAILBOX, SYSTEM_FROM_NAME, recipient, null, subject, textMessage, htmlMessage);
    }

    private void sendMailFromTo(String sender, String senderName, String recipientMailbox,
            String recipientName, String subject, String textMessage, String htmlMessage) {
        if (textMessage == null && htmlMessage == null) {
            throw new IllegalArgumentException("Either textMessage or htmlMessage must not be null but never both!");
        }
        try {
            // Send mail using the Sendgrid API
            if (SENDMAIL_TYPE.toLowerCase().equals("sendgrid")) {
                SendgridWebApiV3 mailApi = new SendgridWebApiV3(SENDGRID_API_KEY);
                SendgridMail mail = mailApi.newMailFromTo(sender, senderName, recipientMailbox, recipientName, subject, textMessage, htmlMessage);
                mail.send();
            // Send mail using the SMTP Protocol
            } else if (SENDMAIL_TYPE.toLowerCase().equals("smtp")) {
                sendSystemMail(recipientMailbox, subject, textMessage, htmlMessage);
            }
        } catch (Exception json) {
            throw new RuntimeException("Sending mail via " + SENDMAIL_TYPE + " failed", json);
        }
    }
    
    /**
     * @param recipient     String of Email Addresses message is sent to.
     *                      Multiple recipients can be separated by ";". **Must not** be NULL.
     * @param subject       String Subject text for the message.
     * @param htmlMessage       String Text content of the message.
     */
    private void sendSystemMail(String recipient, String subject, String textMessage, String htmlMessage) {
        // Hot Fix: Classloader issue we have in OSGi since using Pax web
        Thread.currentThread().setContextClassLoader(SendmailPlugin.class.getClassLoader());
        log.info("BeforeSend: Set classloader to " + Thread.currentThread().getContextClassLoader().toString());
        HtmlEmail email = new HtmlEmail(); // Include in configurations options?
        email.setDebug(SMTP_DEBUG);
        email.setHostName(SMTP_HOST);
        email.setSmtpPort(SMTP_PORT);
        if(SMTP_SECURITY.toLowerCase().equals("smtps")) {
            email.setSslSmtpPort("" + SMTP_PORT);
            email.setSSLOnConnect(true);
            log.info("Set SSLOnConnect...");
        } else if (SMTP_SECURITY.toLowerCase().equals("tls")) {
            email.setSslSmtpPort("" + SMTP_PORT);
            email.setSSLOnConnect(true);
            email.setStartTLSEnabled(true);
            log.info("Set SSLOnConnect + StartTLSEnabled...");
        }
        // SMTP Auth
        if (!SMTP_USERNAME.isEmpty() && !SMTP_PASSWORD.isEmpty()) {
            log.info("Using SMTP Authentication...");
            email.setAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
        }
        try {
            email.setFrom(SYSTEM_FROM_MAILBOX, SYSTEM_FROM_NAME);
            email.setSubject(subject);

            // If textMessage is not given, generate it from html message
            // If htmlMessage is not given, use textMessage.
            String targetTextMessage = textMessage == null ? JavaUtils.stripHTML(htmlMessage) : textMessage;
            String targetHtmlMessage = htmlMessage == null ? textMessage : htmlMessage;

            email.setHtmlMsg(new String(targetHtmlMessage.getBytes("UTF-8"), 0));
            email.setTextMsg(targetTextMessage);
            String recipientValue = recipient.trim();
            Collection<InternetAddress> recipients = new ArrayList<InternetAddress>();
            if (recipientValue.contains(";")) {
                for (String recipientPart : recipientValue.split(";")) {
                    recipients.add(new InternetAddress(recipientPart.trim()));
                }
            } else {
                recipients.add(new InternetAddress(recipientValue));
            }
            email.setTo(recipients);
            email.send();
            log.info("Mail was SUCCESSFULLY sent to " + email.getToAddresses() + " mail addresses");
        } catch (Exception ex) {
            throw new RuntimeException("Sending mail per SMTP FAILED", ex);
        } finally {
            // Fix: Classloader issue we have in OSGi since using Pax web
            Thread.currentThread().setContextClassLoader(CoreService.class.getClassLoader());
            log.info("AfterSend: Set Classloader back " + Thread.currentThread().getContextClassLoader().toString());
        }
    }

}
