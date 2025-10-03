package org.oosd.net;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TetrisClient {
    private static final String HOST = "localhost";
    private static final int PORT = 3000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final ObjectMapper mapper = new ObjectMapper();

    /** Sends the PureGame to the server and returns the optimal move. Opens and closes the socket each call. */
    public OpMove requestMove(PureGame game) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), (int) CONNECT_TIMEOUT.toMillis());
            socket.setSoTimeout((int) READ_TIMEOUT.toMillis());

            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                String jsonGame = mapper.writeValueAsString(game);
                out.write(jsonGame);
                out.write("\n");              // server reads one JSON line
                out.flush();

                String response = in.readLine();
                if (response == null || response.isEmpty()) {
                    throw new EOFException("Empty response from server");
                }
                return mapper.readValue(response, OpMove.class);
            }
        }
    }
}
