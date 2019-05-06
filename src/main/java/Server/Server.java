package Server;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Server {

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
        System.out.println("about to run chat server");
        ChatServer chat = new ChatServer();
        chat.run();
        System.out.println("running chat server...");
    }
}

class ChatServer extends Thread {
    Configuration config;
    ChatServer() {
        config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(9092);
        System.out.println(" ---- chat server obj created --- ");
    }

    @Override
    public void run() {
        final SocketIOServer server = new SocketIOServer(config);
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {

            }
        });
        server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                // broadcast messages to all clients
                // client.joinRoom();
                if(data != null) {
                    System.out.println(data.getUserName());
                    System.out.println(data.getMessage());
                }
                System.out.println(client.isChannelOpen());
                if(client.getSessionId() != null) System.out.println(client.getSessionId());
                System.out.println("ackrequest boolean is requested – " + ackRequest.isAckRequested());
                System.out.println("----------");
                server.getBroadcastOperations().sendEvent("chatevent", data);
            }

        });


        System.out.println("after event listener");
        server.start();
        System.out.println("after server.start() method");
    }
}