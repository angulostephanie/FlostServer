import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.servlet.http.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.*;

import org.json.JSONObject;
import org.json.JSONArray;

@RestController
public class ItemController {
    @RequestMapping(value = "/createItem", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> createItem(@RequestBody String payload, HttpServletRequest request) {
        JSONObject payloadObj = new JSONObject(payload);
        Connection conn = createConnection();

        int userID = payloadObj.getInt("user_id");
        int itemID = payloadObj.getInt("item_id");
        String name = payloadObj.getString("item_name");
        String description = payloadObj.getString("item_desc");
        String type = payloadObj.getString("item_type");
        String reward = payloadObj.getString("item_reward");
        String location = payloadObj.getString("item_location");
        Date now = new Date(System.currentTimeMillis());
        Timestamp timestamp = new Timestamp(now.getTime());

        String insertTableSQL = "Insert INTO Items"  +
                "(user_id, item_name, item_desc, item_type, item_reward, item_location, timestamp)" +
                "values (?,?,?,?,?,?,?)";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setInt(1, userID);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, type);
            ps.setString(5, reward);
            ps.setString(6, location);
            ps.setTimestamp(7, timestamp);
            ps.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        } finally {
            try { if(conn != null) conn.close(); }
            catch(SQLException e) { e.printStackTrace(); }
        }


		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */


        //Returns the response with a String, headers, and HTTP status
        JSONObject responseObj = new JSONObject();
        responseObj.put("user_id ", userID);
        responseObj.put("item_id ", itemID);
        responseObj.put("item_name ", name);
        responseObj.put("message", "item added successfully");
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }
    /*
    @RequestMapping(value = "/queryItems", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> listLostItems(HttpServletRequest request) {
//		Creating http headers object to place into response entity the server will return.
//		This is what allows us to set the content-type to application/json or any other content-type
//		we would want to return
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONArray itemsArray = new JSONArray();
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/classdb?useUnicode=true&characterEncoding=UTF-8", "root", "password");
            String query = "SELECT userid, item_name, item_type FROM flost.lostItems";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String itemName = rs.getString("item_name");
                int itemType = rs.getString("item_type");

                JSONObject obj = new JSONObject();
                obj.put("item_name", name);
                obj.put("item_type", itemType);
                itemsArray.put(obj);
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) { conn.close(); }
            }catch(SQLException se) {

            }

        }
        return new ResponseEntity(itemsArray.toString(), responseHeaders, HttpStatus.OK);
    }
    */

    /*
    @RequestMapping(value = "/searchItems", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> searchLostItems(HttpServletRequest request) {
        String itemType = request.getParameter("itemType");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Connection conn = null;
        JSONArray itemsArray = new JSONArray();
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/classdb?useUnicode=true&characterEncoding=UTF-8", "root", "password");
            String query = "SELECT userid, item_name, item_type FROM flost.lostItems WHERE item_type=?";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            stmt.setString(1, itemType);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String itemName = rs.getString("item_name");
                int itemType = rs.getString("item_type");

                JSONObject obj = new JSONObject();
                obj.put("item_name", name);
                obj.put("item_type", itemType);
                itemsArray.put(obj);
            }
        } catch (SQLException e ) {
        } finally {
            try {
                if (conn != null) { conn.close(); }
            }catch(SQLException se) {

            }

        }
        return new ResponseEntity(itemsArray.toString(), responseHeaders, HttpStatus.OK);
    }
    */
    public static String bytesToHex(byte[] in) {
        StringBuilder builder = new StringBuilder();
        for(byte b: in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static Connection createConnection() {
        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        String TIMEZONE_THING = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=" +
                "false&serverTimezone=UTC";
        String UNICODE = "?useUnicode=true&characterEncoding=UTF-8";
        String DB_URL = "jdbc:mysql://localhost/flost" + UNICODE + TIMEZONE_THING;
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
