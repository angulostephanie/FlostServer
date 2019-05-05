package Server;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;

class GoogleSignInAuthentication {
    private static final String CLIENT_ID = "302127086091-es2d4803pni58crrri2i22pnfru2oqo6.apps.googleusercontent.com";
    private static final HttpTransport httpTransport = new NetHttpTransport();
    private static final JsonFactory jacksonFactory = new JacksonFactory();
    private static final String OXY_EMAIL = "oxy.edu";

    static GoogleIdToken.Payload getGooglePayload(String payloadToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jacksonFactory)
                .setAudience(Collections.singletonList(CLIENT_ID))
                .setIssuers(Arrays.asList("https://accounts.google.com", "accounts.google.com"))
                .build();
        try {
            GoogleIdToken token = verifier.verify(payloadToken);
            GoogleIdToken.Payload googlePayload = token.getPayload();

            return googlePayload;
        } catch(GeneralSecurityException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
            e.printStackTrace();
            System.out.println("TOKEN WAS NOT SUCCESSFULLY VERIFIED");
        }
        return null;
    }
    static boolean isOxyEmail(GoogleIdToken.Payload googlePayload) {
        return googlePayload.getHostedDomain().equals(OXY_EMAIL);
    }
    static ResponseEntity<String> getErrorResponse(GoogleIdToken.Payload googlePayload, HttpHeaders responseHeaders) {
        JSONObject errorObj = new JSONObject();
        if(googlePayload == null) {
            System.out.println("TOKEN WAS NOT SUCCESSFULLY VERIFIED");
            errorObj.put("error", 1);
            errorObj.put("message", "google token was not verified, try again");
            return new ResponseEntity<>(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } else if(!isOxyEmail(googlePayload)) {
            System.out.println(googlePayload.getHostedDomain());
            System.out.println("Domain must be: " + OXY_EMAIL);
            System.out.println("MUST USE A OXY DOMAIN EMAIL");
            errorObj.put("error", 2);
            errorObj.put("message", "user must login with oxy email");
            return new ResponseEntity<>(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
        return null;
    }
    static ResponseEntity<String> getUnmatchingEmailErrorResponse(HttpHeaders responseHeaders) {
        JSONObject errorObj = new JSONObject();
        errorObj.put("message", "emails do not match, >:(");
        return new ResponseEntity<>(errorObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
    }
}
