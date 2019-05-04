package Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.*;

@RestController
public class MessageController {
    /*
        Posts message object in the Messages table
     */
    @RequestMapping(value = "/postMessage", method = RequestMethod.POST)
    public ResponseEntity<String> postItem(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload);
        System.out.println(payload);
        JSONObject responseObj = new JSONObject();
        Connection conn = SQLConnection.createConnection();

        int messageID = payloadObj.getInt("message_id");
        String senderEmail = payloadObj.getString("sender_email");
        String receiverEmail = payloadObj.getString("receiver_email");
        String messageContent = payloadObj.getString("message_content");
        System.out.println("message says this :" + messageContent);
        String insertTableSQL = "Insert INTO Messages" +
                "(message_id, sender_email, receiver_email, message_content, message_timestamp)" +
                "values (?,?,?,?,?)";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setInt(1, messageID);
            ps.setString(2, senderEmail);
            ps.setString(3, receiverEmail);
            ps.setString(4, messageContent);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            responseObj.put("error", e.getErrorCode());
            responseObj.put("message", "could not post message to db :/ [" + e.getMessage() + "]");
            return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        responseObj.put("message", "message was successfully added to db :)");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }
    /*
        Ideally, we want to get messages specific to a chat room.
        We can uniquely if messages belong to a chat room given the sender/receiver email.
        So – parameters will be those 2 emails.
     */
    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    public ResponseEntity<String> getMessages(@RequestBody(required = false) String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONArray messages = new JSONArray();
        JSONObject payloadObj = new JSONObject(payload);

        String senderEmail = payloadObj.getString("sender_email");
        String receiverEmail = payloadObj.getString("receiver_email");

        String query = "SELECT * FROM Messages WHERE `sender_email` = ? AND `receiver_email` = ?";
        Connection conn = SQLConnection.createConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(query);
            ps.setString(1, senderEmail);
            ps.setString(2, receiverEmail);

            ResultSet results = ps.executeQuery();

            while(results.next()) {
                JSONObject message = new JSONObject();
                message.put("message_id", results.getInt("message_id"));
                message.put("sender_email", results.getString("sender_email"));
                message.put("receiver_email", results.getString("receiver_email"));
                message.put("message_content", results.getString("message_content"));
                messages.put(message);
            }
            ps.close();
        } catch(SQLException e) {
            e.printStackTrace();
            System.out.println("Error code [" + e.getErrorCode() + "]");
            JSONObject errorObj = new JSONObject();
            errorObj.put("message", "could not query messages from this chat");

            return new ResponseEntity(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);

        }
        return new ResponseEntity(messages.toString(), responseHeaders, HttpStatus.OK);
    }
    }
