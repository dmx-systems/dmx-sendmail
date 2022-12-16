package systems.dmx.sendmail.util;

import static systems.dmx.sendmail.util.SendgridWebApiV3.BCC;
import static systems.dmx.sendmail.util.SendgridWebApiV3.CC;
import static systems.dmx.sendmail.util.SendgridWebApiV3.PERSONALIZATIONS;
import static systems.dmx.sendmail.util.SendgridWebApiV3.SUBJECT;
import static systems.dmx.sendmail.util.SendgridWebApiV3.TO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import systems.dmx.core.JSONEnabled;

import javax.activation.MimeType;

/**
 * 
 * @author Malte Reißig
 */
public class SendgridMail implements JSONEnabled {
    
    private static Logger log = Logger.getLogger(SendgridMail.class.getName());

    private final static String DM4_HOST_URL = System.getProperty("dm4.host.url");

    private JSONObject mail = null;
    private String apiKey = null;

    public SendgridMail(String apiKey, String from, String fromName) {
        try {
            // set up mail
            this.apiKey = apiKey;
            this.mail = new JSONObject();
            // initialize mail sender and mail settings
            addSender(from, fromName, mail);
            JSONObject settings = initializeMailSettings(mail);
            addHostUrlFooter(settings);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addRecipient(String recipient, String recipientName, String type, String subject) throws JSONException {
        addPersonalizations(recipient, recipientName, type, subject, mail);
    }

    public void addHTMLMessage(String htmlMessage) throws JSONException {
        addContent("text/html", htmlMessage, mail);
    }

    public void addTextMessage(String textMessage) throws JSONException {
        addContent("text/plain", textMessage, mail);
    }

    public void send() {
        try {
            URL apiUrl = new URL("https://api.sendgrid.com/v3/mail/send");
            HttpURLConnection httpConnection = (HttpURLConnection) apiUrl.openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setDoOutput(true);
            httpConnection.setRequestProperty("Authorization", "Bearer " + apiKey);
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setRequestProperty("User-Agent", "DeepaMehta 4 Sendgrid Plugin (Java, OSGi)");
            if (mail != null) {
                // See https://sendgrid.com/docs/API_Reference/Web_API_v3/Mail/index.html
                log.info("Sendgrid API Mail Request: " + mail.toString());
                OutputStream output = httpConnection.getOutputStream();
                output.write(mail.toString().getBytes("UTF-8"));
                // Read in potential error response
                InputStream response = httpConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(response));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                // Check request status code
                if (httpConnection.getResponseCode() != 202) {
                    log.severe("An error occured during sending mail via the Sendgrid API Response"
                            + "Status: (" + httpConnection.getResponseCode() + "), Answer:" + sb.toString());
                } else {
                    log.info("Sendgrid API Response OK (" + httpConnection.getResponseCode() + ")");
                }
            } else {
                log.severe("Could not send SendgridApi Mail due to a previous error.");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void addPersonalizations(String recipient, String recipientName, String recipientType,
            String subject, JSONObject query) throws JSONException {
        JSONArray personals = new JSONArray();
        if (query.has(PERSONALIZATIONS)) {
            personals = query.getJSONArray(PERSONALIZATIONS);
        } else {
            query.put(PERSONALIZATIONS, personals);
        }
        addRecipient(recipient, recipientName, recipientType, subject, personals);
    }

    private void addRecipient(String recipient, String recipientName, String recipientType, String subject, JSONArray personals) throws JSONException {
        JSONArray standardRecipients = null;
        JSONArray copyRecipients = null;
        JSONArray blindCopyRecipients = null;
        for (int idx = 0; idx < personals.length(); idx++) {
            JSONObject recipientObject = personals.getJSONObject(idx);
            if (recipientObject.has(TO)) standardRecipients = recipientObject.getJSONArray(TO);
            if (recipientObject.has(CC)) copyRecipients = recipientObject.getJSONArray(CC);
            if (recipientObject.has(BCC)) blindCopyRecipients = recipientObject.getJSONArray(BCC);
        }
        if (recipientType.equals(TO)) {
            if (standardRecipients == null) {
                standardRecipients = new JSONArray();
                personals.put(new JSONObject().put(TO, standardRecipients).put(SUBJECT, subject));
            }
            standardRecipients.put(createRecipientObject(recipient, recipientName));   
        }
        if (recipientType.equals(CC)) {
            if (copyRecipients == null) {
                copyRecipients = new JSONArray();
                personals.put(new JSONObject().put(CC, copyRecipients).put(SUBJECT, subject));
            }
            copyRecipients.put(createRecipientObject(recipient, recipientName));   
        }
        if (recipientType.equals(BCC)) {
            if (blindCopyRecipients == null) {
                blindCopyRecipients = new JSONArray();
                personals.put(new JSONObject().put(CC, blindCopyRecipients).put(SUBJECT, subject));
            }
            blindCopyRecipients.put(createRecipientObject(recipient, recipientName));   
        }
    }

    private JSONObject createRecipientObject(String recipient, String recipientName) throws JSONException {
        JSONObject entry = new JSONObject();
        if (recipient == null || !recipient.contains("@") || !recipient.contains(".")) {
            throw new RuntimeException("Invalid recipient address " + recipient);
        }
        entry.put("email", recipient);
        if (recipientName != null && !recipientName.isEmpty()) entry.put("name", recipientName);
        return entry;
    }
    
    private void addSender(String from, String fromName, JSONObject query) throws JSONException {
        JSONObject entry = new JSONObject();
        entry.put("email", from);
        if (fromName != null && !fromName.isEmpty()) entry.put("name", fromName);
        query.put("from", entry);
    }

    private void addContent(String contentType, String contentValue, JSONObject query) throws JSONException {
        JSONObject content = new JSONObject();
        content.put("type", contentType);
        content.put("value", contentValue);
        if (query.has("content")) {
            query.getJSONArray("content").put(content);
        } else {
            query.put("content", new JSONArray().put(content));
        }
    }
 
    private void addHostUrlFooter(JSONObject settings) throws JSONException {
        String htmlValue = "<br/><br/>---<br/><a href=\""+DM4_HOST_URL+"\">" + DM4_HOST_URL + "</a><br/>";
        String textValue = "\n\n---\n" + DM4_HOST_URL + "\n";
        addFooterSetting(textValue, htmlValue, settings);
    }
    
    private void addFooterSetting(String textValue, String htmlValue, JSONObject settings) throws JSONException {
        JSONObject footer = new JSONObject();
        if (settings.has("footer")) {
            footer = settings.getJSONObject("footer");
            footer.put("text", textValue);
            if (htmlValue != null) footer.put("html", htmlValue);
        } else {
            settings.put("footer", footer);
            footer.put("text", textValue);
            if (htmlValue != null) footer.put("html", htmlValue);
        }
        footer.put("enable", true);
    }
    
    private JSONObject initializeMailSettings(JSONObject query) throws JSONException {
        if (!query.has("mail_settings")) {
            query.put("mail_settings", new JSONObject());
        }
        return query.getJSONObject("mail_settings");
    }

    @Override
    public JSONObject toJSON() {
        return mail;
    }

}
