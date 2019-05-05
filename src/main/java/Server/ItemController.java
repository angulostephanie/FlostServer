package Server;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.json.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import javax.servlet.http.*;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        System.out.println(payload);
        JSONObject responseObj = new JSONObject();


        String token = payloadObj.getString("token");
        GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(token);
        if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
            System.out.println("error, google error response");
            return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
        }
        String googleEmail = googlePayload.getEmail();
        String email = payloadObj.getString("email");

        // someone is trying to hack lol, stahp
        if(!googleEmail.equals(email)) { return GoogleSignInAuthentication.getUnmatchingEmailErrorResponse(responseHeaders); }

        int itemID = payloadObj.getInt("item_id");

        //  Date and time in this case mean when the item was
        //  lost or found, not when the object was uploaded/created.

        String date = payloadObj.getString("item_date");
        String time = payloadObj.getString("item_time");
        String title = payloadObj.getString("item_name");
        String description = payloadObj.getString("item_desc");
        String type = payloadObj.getString("item_type");
        String location = payloadObj.getString("item_location");

        Connection conn = SQLConnection.createConnection();
        // user only has a static image id if they don't upload their own image
        // this id maps to a static image hosted on the android app
        int staticImageID = -1;
        if(payloadObj.has("static_image_id")) { staticImageID =  payloadObj.getInt("static_image_id"); }


        String insertTableSQL = "Insert INTO Items"  +
                "(item_id, email, item_name, item_desc, item_date, item_time, item_type, item_location, " +
                "item_timestamp, static_image_id)" +
                "values (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(insertTableSQL);
            ps.setInt(1, itemID);
            ps.setString(2, email);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setString(5, date);
            ps.setString(6, time);
            ps.setString(7, type); // type
            ps.setString(8, location);
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.setInt(10, staticImageID);
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
        responseObj.put("email", email);
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

        String token = payloadObj.getString("token");
        GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(token);
        if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
            System.out.println("error, google error response");
            return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
        }

        if(payloadObj.isNull("item_type")) all = true;
        String query = all ? "SELECT * From Items ORDER BY `item_timestamp` DESC" :
                "SELECT * FROM Items WHERE item_type = ? ORDER BY `item_timestamp` DESC";

        Connection conn = SQLConnection.createConnection();
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
                item.put("email", results.getString("email"));
                item.put("item_name", results.getString("item_name"));
                item.put("item_desc", results.getString("item_desc"));
                item.put("item_date", results.getString("item_date"));
                item.put("item_time", results.getString("item_time"));
                item.put("item_type", results.getString("item_type"));
                item.put("item_location", results.getString("item_location"));
                item.put("item_image", results.getBlob("item_image"));
                item.put("item_timestamp", results.getTimestamp("item_timestamp"));
                items.put(item);
            }
            ps.close();
        } catch(SQLException e) {
            e.printStackTrace();
            System.out.println("Error code [" + e.getErrorCode() + "]");
            JSONObject errorObj = new JSONObject();
            errorObj.put("message", "could not query items");

            return new ResponseEntity(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);

        }
        return new ResponseEntity(items.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/deleteItem", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteItem(@RequestBody String payload, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        JSONObject payloadObj = new JSONObject(payload);
        System.out.println(payload);
        JSONObject responseObj = new JSONObject();
        Connection conn = SQLConnection.createConnection();

        String token = payloadObj.getString("token");
        GoogleIdToken.Payload googlePayload = GoogleSignInAuthentication.getGooglePayload(token);
        if(GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders) != null) {
            System.out.println("error, google error response");
            return GoogleSignInAuthentication.getErrorResponse(googlePayload, responseHeaders);
        }
        String googleEmail = googlePayload.getEmail();
        String email = payloadObj.getString("email");

        // someone is trying to hack lol, stahp
        if(!googleEmail.equals(email)) { return GoogleSignInAuthentication.getUnmatchingEmailErrorResponse(responseHeaders); }

        int itemID = payloadObj.getInt("item_id");

        String deleteItemSQL = "DELETE FROM Items WHERE `item_id` = ? AND `email` = ?";
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(deleteItemSQL);
            ps.setInt(1, itemID);
            ps.setString(2, email);
            System.out.println("executing update");
            ps.executeUpdate();

        } catch(SQLException e) {
            e.printStackTrace();
            responseObj.put("error", e.getErrorCode());
            responseObj.put("message", "could not delete item to db :/ [" + e.getMessage() + "]");
            return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            try { if(conn != null) conn.close(); }
            catch(SQLException e) { e.printStackTrace(); }
        }

        responseObj.put("message", "item deleted from db successfully");
        return new ResponseEntity<>(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }
}
