package systems.dmx.sendmail;

/**
 * 
 * @author Malte Reißig
 */
public interface SendmailService {

    public void doEmailUser(String username, String subject, String message, String htmlMessage);

    public void doEmailUser(String fromUsername, String toUsername, String subject, String message, String htmlMessage);

    public void doEmailRecipient(String subject, String message, String htmlMessage, String recipientEmail);

    public void doEmailRecipientAs(String fromEmail, String fromName, String subject,
            String message, String htmlMessage, String recipientEmail);

    public void doEmailSystemMailbox(String subject, String message, String htmlMessage);

}
