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
    private static final String CLIENT_ID = "302127086091-es2d4803pni58crrri2i22pnfru2oqo6.apps.googleusercontent.com";
    private static final HttpTransport httpTransport = new NetHttpTransport();
    private static final JsonFactory jacksonFactory = new JacksonFactory();
    private static final String OXY_EMAIL = "oxy.edu";
    /*
        Checks if user not in Users table,
        if user is in table, login successfully,
        if not, insert user into Users table.
     */
    @RequestMapping(value = "/authenticateUser", method = RequestMethod.POST)
    public ResponseEntity<String> authenticateUser(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload); // just the ID token
        String payloadToken = payloadObj.getString("token");


        JSONObject responseObj = new JSONObject();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jacksonFactory)
                .setAudience(Collections.singletonList(CLIENT_ID))
                .setIssuers(Arrays.asList("https://accounts.google.com", "accounts.google.com"))
                .build();

        try {
            GoogleIdToken token = verifier.verify(payloadToken);

            if(token == null) {
                System.out.println("TOKEN WAS NOT SUCCESSFULLY VERIFIED");
                responseObj.put("error", 1);
                responseObj.put("message", "google token was not verified, try again");
                return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }

            GoogleIdToken.Payload googlePayload = token.getPayload();
            if(!googlePayload.getHostedDomain().equals(OXY_EMAIL)) {
                System.out.println(googlePayload.getHostedDomain());
                System.out.println(OXY_EMAIL);
                System.out.println("MUST USE A OXY DOMAIN EMAIL");
                responseObj.put("error", 2);
                responseObj.put("message", "user must login with oxy email");
                return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }

            System.out.println("----------------------------------------------------");
            System.out.println(googlePayload.toPrettyString());
            System.out.println("----------------------------------------------------");

            String firstName = googlePayload.get("given_name").toString();
            String lastName = googlePayload.get("family_name").toString();
            String email = googlePayload.getEmail();
            String photoURL = googlePayload.get("picture").toString();

            System.out.println(firstName + "\n" + lastName + "\n" + email + "\n" + photoURL);

            Connection conn = createConnection();
            JSONObject existingUser = doesUserExist(conn, email);

            if(existingUser != null) { return new ResponseEntity<>(existingUser.toString(), responseHeaders, HttpStatus.OK); }

            responseObj.put("first_name", firstName);
            responseObj.put("last_name", lastName);
            responseObj.put("email", email);
            if(photoURL != null) responseObj.put("photo_url", photoURL);

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

        } catch(GeneralSecurityException e) {
            e.printStackTrace();
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
                userObj.put("first_name", results.getString("first_name"));
                userObj.put("last_name", results.getString("last_name"));
                userObj.put("email",  email);
                if(userObj.get("photo_url") != null) userObj.getString("photo_url");
                userObj.put("message", "user already exist, welcome back!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userObj;
    }

    private Connection createConnection() {
        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        String TIMEZONE_THING = "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=PST";
        String DB_URL = "jdbc:mysql://localhost/flost" +  TIMEZONE_THING;
        String USER = "root";
        String PASSWORD = "password123"; // password123
        Connection conn;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            return conn;
        } catch(SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}