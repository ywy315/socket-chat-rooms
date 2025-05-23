package org.chatTest;

import org.chatTest.Exception.LoginFailException;
import org.chatTest.Exception.RegisterFailException;
import org.chatTest.Exception.UserExistsException;
import org.chatTest.Utils.SQLUtils;

import java.io.*;
import java.net.*;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                try {
                    if (in.readObject() instanceof String message) {
                        System.out.println("Received: " + message);///////////
                        if (message.startsWith("MESSAGE")) {
                            //组播发送端代码
                            // 创建MulticastSocket对象
                            MulticastSocket ms = new MulticastSocket();
                            byte[] bytes = message.getBytes();
                            InetAddress address = InetAddress.getByName("255.255.255.255");//这里设置的ip为广播地址
                            int port = 10000;
                            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, address, port);
                            //调用MulticastSocket发送数据方法发送数据
                            ms.send(dp);
                            System.out.println("Sent " + message + " to all clients");
                            //释放资源
                            ms.close();
                        } else if (message.startsWith("ADD_FRIEND")) {
                            handleAddFriends(message);
                        } else if (message.startsWith("REQUEST_FRIENDS")) {
                            handleRequestFriends(message);
                        } else if (message.startsWith("LOGIN")) {
                            handleLogin(message);
                        } else if (message.startsWith("REGISTER")) {
                            handleRegister(message);
                        } else if (message.startsWith("CHANGE_STATUS")) {
                            changeStatus(message);
                        }
                    }
                } catch (IOException e) {

                }
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private void handleRegister(String request) throws Exception {
        String[] parts = request.split(":");
        String username = parts[1];
        String password = parts[2];
        try {
            SQLUtils.registerUser(username, password);
        } catch (RegisterFailException e) {
            out.writeObject("注册失败");
            return;
        } catch (UserExistsException e) {
            out.writeObject("用户名已存在");
            return;
        }
        out.writeObject("注册成功");
        out.flush();
    }

    private void handleLogin(String request) throws Exception {
        String[] parts = request.split(":");
        String username = parts[1];
        String password = parts[2];

        try {
            Map<String, Object> result = SQLUtils.loginUser(username, password);
            //不允许同时重复登陆未做

            if (result.containsKey("userId")) {
                int userId = (int) result.get("userId");
                out.writeObject("LOGIN_SUCCESS:" + userId);
                out.flush();
            }
        } catch (LoginFailException e) {
            out.writeObject("登录失败" + e.getMessage());
            out.flush();
        }

    }

    private void handleAddFriends(String request) throws Exception {
        String[] parts = request.split(":");
        String userid = parts[1];
        String friendid = parts[2];
        try {
            SQLUtils.establishFriends(Integer.parseInt(userid), Integer.parseInt(friendid));
        } catch (LoginFailException e) {
            out.writeObject("添加失败" + e.getMessage());
            out.flush();
        }

    }

    private void handleRequestFriends(String request) throws Exception {
        int userId = Integer.parseInt(request.split(":")[1]);
        String[] friendsWithStatus = SQLUtils.getFriendsWithStatus(userId);
        StringBuilder sb = new StringBuilder("FRIENDS:");
        for (String friend : friendsWithStatus) {
            if (sb.length() > 8) sb.append(",");
            sb.append(friend);
        }
        out.writeObject(sb.toString());
        out.flush();
    }


    private void changeStatus(String request) throws Exception {
        String[] parts = request.split(":");
        String userid = parts[1];
        String status = parts[2];
        try {
            SQLUtils.changeStatus(Integer.parseInt(userid), status);
        } catch (LoginFailException e) {
            out.writeObject("修改失败" + e.getMessage());
            out.flush();
        }

    }
}


