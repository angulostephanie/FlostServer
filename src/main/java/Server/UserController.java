package Server;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.JSONObject;

@RestController
public class UserController {
    @RequestMapping(value = "/createUser", method = RequestMethod.POST)
    public ResponseEntity<String> createUser(@RequestBody String payload, HttpServletRequest request) {
        JSONObject payloadObj = new JSONObject(payload);


        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        Connection conn = createConnection();

        String userID = payloadObj.getString("user_id");

        JSONObject existingUser = doesUserExist(conn, userID);

        if(existingUser != null) {
            return new ResponseEntity<>(existingUser.toString(), responseHeaders, HttpStatus.OK);
        }

        JSONObject responseObj = new JSONObject();
        String firstName = payloadObj.getString("first_name");
        String email = payloadObj.getString("email");

        String insertTableSQL = "Insert INTO Users" +
                "(user_id, first_name, email)" +
                "values (?,?,?)";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setString(1, userID);
            ps.setString(2, firstName);
            ps.setString(3, email);
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



        responseObj.put("user_id ", userID);
        responseObj.put("first_name ", firstName);
        responseObj.put("email ", email);
        responseObj.put("message", "user created successfully");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    private JSONObject doesUserExist(Connection conn, String userID) {
        JSONObject userObj = null;
        String insertTableSQL = "SELECT * FROM Users WHERE `user_id` = ?";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setString(1, userID);
            ResultSet results = ps.executeQuery();
            if(results.next()) {
                String first = results.getString("first_name");
                String email = results.getString("email");
                System.out.println("HELLO THERE");
                userObj = new JSONObject();
                userObj.put("message", "user already exist, welcome back!");
                userObj.put("first_name", first);
                userObj.put("email", email);
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
        String PASSWORD = "password123";
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