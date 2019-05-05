package Server;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.Arrays;
import java.util.Collections;

import org.json.JSONObject;

@RestController
public class UserController {

    /*
       Takes in Google sign in token, verifies it, gets email and other user data
       Stores user data into db, if not already there.
     */
    @RequestMapping(value = "/authenticateUser", method = RequestMethod.POST)
    public ResponseEntity<String> authenticateUser(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload); // just the ID token
        String payloadToken = payloadObj.getString("token");

        JSONObject responseObj = new JSONObject();

        try {

            GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(payloadToken);

            if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
                return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
            }

            System.out.println("----------------------------------------------------");
            System.out.println(googlePayload.toPrettyString());
            System.out.println("----------------------------------------------------");

            String firstName = googlePayload.get("given_name").toString();
            String lastName = googlePayload.get("family_name").toString();
            String email = googlePayload.getEmail();
            String photoURL = googlePayload.get("picture").toString();

            System.out.println(firstName + "\n" + lastName + "\n" + email + "\n" + photoURL);

            Connection conn = SQLConnection.createConnection();
            JSONObject existingUser = doesUserExist(conn, email);

            if(existingUser != null) { return new ResponseEntity<>(existingUser.toString(), responseHeaders, HttpStatus.OK); }

            responseObj.put("first_name", firstName);
            responseObj.put("last_name", lastName);
            responseObj.put("email", email);
            if(!photoURL.isEmpty()) responseObj.put("photo_url", photoURL);

            String insertTableSQL = "Insert INTO Users" +
                    "(email, first_name, last_name, photo_url)" +
                    "values (?,?,?, ?)";
            PreparedStatement ps;
            try {
                ps = conn.prepareStatement(insertTableSQL);
                ps.setString(1, email);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, photoURL);
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
                responseObj.put("error", e.getErrorCode());
                responseObj.put("message", "could not successfully insert");
                return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            } finally {
                try {
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    responseObj.put("error", e.getErrorCode());
                    responseObj.put("message", "could not close connection");
                }
            }

        } catch(IOException e) {
            e.printStackTrace();
        }

        responseObj.put("message", "successful :)");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    private JSONObject doesUserExist(Connection conn, String email) {
        JSONObject userObj = null;
        String insertTableSQL = "SELECT * FROM Users WHERE `email` = ?";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setString(1, email);
            ResultSet results = ps.executeQuery();
            if(results.next()) {
                userObj = new JSONObject();
                System.out.println("-------------");
                System.out.println("UserController::doesUserExist()");
                userObj.put("first_name", results.getString("first_name"));
                userObj.put("last_name", results.getString("last_name"));
                userObj.put("email",  email);
                if(userObj.has("photo_url")) { userObj.getString("photo_url"); }
                System.out.println(userObj.toString());
                System.out.println("-------------");
                userObj.put("message", "user already exist, welcome back!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userObj;
    }
}