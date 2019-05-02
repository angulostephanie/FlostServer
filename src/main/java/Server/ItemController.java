package Server;

import org.json.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.JSONObject;

@RestController
public class ItemController {
    /*
        Posts item into Items table
     */
    @RequestMapping(value = "/postItem", method = RequestMethod.POST)
    public ResponseEntity<String> postItem(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload);
        JSONObject responseObj = new JSONObject();
        Connection conn = createConnection();

        int itemID = payloadObj.getInt("item_id");
        String userID = payloadObj.getString("user_id");
        String title = payloadObj.getString("item_name");
        String description = payloadObj.getString("item_desc");
        String type = payloadObj.getString("item_type"); // lost or found
        String location = payloadObj.getString("item_location");
        double reward = payloadObj.getDouble("item_reward");

        String insertTableSQL = "Insert INTO Items"  +
                "(item_id, user_id, item_name, item_desc, item_type, item_reward, item_location, item_timestamp)" +
                "values (?,?,?,?,?,?,?,?)";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setInt(1, itemID);
            ps.setString(2, userID);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setString(5, type);
            ps.setDouble(6, reward);
            ps.setString(7, location);
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();

        } catch(SQLException e) {
            e.printStackTrace();
            responseObj.put("error", e.getErrorCode());
            responseObj.put("message", "could not post item to db :/ [" + e.getMessage() + "]");
            return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            try { if(conn != null) conn.close(); }
            catch(SQLException e) { e.printStackTrace(); }
        }

        responseObj.put("item_id", itemID);
        responseObj.put("user_id", userID);
        responseObj.put("message", "item posted to db successfully");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    /*
       Gets items based on whether its type 'lost' or 'found'
       If no parameter is passed, return all.
       This is needed for the list/map views.
     */
    @RequestMapping(value = "/getItems", method = RequestMethod.GET)
    public ResponseEntity<String> getItems(@RequestBody(required = false) String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        boolean all = false;

        JSONArray items = new JSONArray();
        JSONObject payloadObj = new JSONObject(payload);
        if(payloadObj.isNull("item_type")) all = true;
        String query = all ? "SELECT * From Items ORDER BY `item_timestamp` ASC" :
                "SELECT * FROM Items WHERE item_type = ? ORDER BY `item_timestamp` ASC";

        Connection conn = createConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(query);

            if(!all) {
                String type = payloadObj.getString("item_type");
                ps.setString(1, type);
            }
            ResultSet results = ps.executeQuery();

            while(results.next()) {
                JSONObject item = new JSONObject();
                item.put("item_id", results.getInt("item_id"));
                item.put("user_id", results.getString("user_id"));
                item.put("item_name", results.getString("item_name"));
                item.put("item_desc", results.getString("item_desc"));
                item.put("item_type", results.getString("item_type"));
                item.put("item_location", results.getString("item_location"));
                item.put("item_reward", results.getDouble("item_reward"));
                item.put("item_timestamp", results.getTimestamp("item_timestamp"));
                items.put(item);
            }
            ps.close();
        } catch(SQLException e) {
            e.printStackTrace();
            JSONObject errorObj = new JSONObject();
            errorObj.put("message", "could not query items");

            return new ResponseEntity(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);

        }
        return new ResponseEntity(items.toString(), responseHeaders, HttpStatus.OK);
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
