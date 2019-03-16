package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lib.utils;
import model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


    public class NotifySocket {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public NotifySocket(String ip, int port) throws IOException {
            startConnection(ip, port);
        }

        public void startConnection(String ip, int port) throws IOException {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        public String sendMessage(String msg) {
            try {
                out.println(msg);
                return in.readLine();
            }
            catch (IOException e){
                return "";
            }
        }

        public BufferedReader getIn() {
            return in;
        }
    }

