package Server;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
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
public class ChatController {
    /*
        Creates a mapping of the chat room id to the associated email.
        Example, if sangulo@oxy.edu and trane@oxy.edu begin a chat, the table would look something like this.

        chat_room_id,   owner_email
        1               sangulo@oxy.edu
        1               trane@oxy.edu
     */
    @RequestMapping(value = "/createChatRoom", method = RequestMethod.POST)
    public ResponseEntity<String> createChatRoom(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload);
        System.out.println(payload);
        String token = payloadObj.getString("token");
        GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(token);

        if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
            System.out.println("error, google error response");
            return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
        }
        String googleEmail = googlePayload.getEmail();
        int chatRoomID = payloadObj.getInt("chat_room_id");
        String owner_email = payloadObj.getString("current_email");
        if(!googleEmail.equals(owner_email)) { return GoogleSignInAuthentication.getUnmatchingEmailErrorResponse(responseHeaders); }

        String otherEmail = payloadObj.getString("other_email");

        Connection conn = SQLConnection.createConnection();
        JSONObject responseObj = new JSONObject();

        String insertTableSQL = "Insert INTO ChatRooms"  +
                "(chat_room_id, owner_email)" +
                "values (?,?)";
        PreparedStatement ps1;
        PreparedStatement ps2;
        try {
            ps1 = conn.prepareStatement(insertTableSQL);
            ps2 = conn.prepareStatement(insertTableSQL);

            ps1.setInt(1, chatRoomID);
            ps1.setString(2, owner_email);
            ps1.executeUpdate();

            ps2.setInt(1, chatRoomID);
            ps2.setString(2, otherEmail);
            ps2.executeUpdate();

            ps1.close();
            ps2.close();

        } catch(SQLException e) {
            e.printStackTrace();
            responseObj.put("error", e.getErrorCode());
            responseObj.put("message", "could not create a chat room :/ [" + e.getMessage() + "]");
            return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            try { if(conn != null) conn.close(); }
            catch(SQLException e) { e.printStackTrace(); }
        }


        responseObj.put("message", "chat room successfully created :)");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    /*
        Gets all the chat rooms a user is a part of.
        i.e. if sangulo@oxy.edu is chatting with both trane@oxy.edu (chat room id = 1) and dih@oxy.edu (chat room id = 2)
            this function will return a JSONArray [{"chat_room_id" : 1}, {"chat_room_id" : 2}]
     */
    @RequestMapping(value = "/getChatRooms", method = RequestMethod.GET)
    public ResponseEntity<String> getChatRooms(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONArray chatRooms = new JSONArray();
        JSONObject payloadObj = new JSONObject(payload);

        String token = payloadObj.getString("token");
        GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(token);
        if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
            System.out.println("error, google error response");
            return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
        }

        String googleEmail = googlePayload.getEmail();
        String currentUserEmail = payloadObj.getString("current_email");

        // someone is trying to hack lol, stahp
        if(!googleEmail.equals(currentUserEmail)) { return GoogleSignInAuthentication.getUnmatchingEmailErrorResponse(responseHeaders); }

        Connection conn = SQLConnection.createConnection();

        String query = "SELECT * FROM ChatRooms WHERE `owner_email` = ?";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(query);
            ps.setString(1, currentUserEmail);
            ResultSet results = ps.executeQuery();

            while(results.next()) {
                JSONObject chatRoom = new JSONObject();
                chatRoom.put("chat_room_id", results.getInt("chat_room_id"));
                chatRooms.put(chatRoom);
            }
            ps.close();
        } catch(SQLException e) {
            e.printStackTrace();
            System.out.println("Error code [" + e.getErrorCode() + "]");
            JSONObject errorObj = new JSONObject();
            errorObj.put("message", "could not query messages from this chat");
            return new ResponseEntity(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(chatRooms.toString(), responseHeaders, HttpStatus.OK);
    }
}
