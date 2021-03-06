package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Exchanger;
import database.*;
import database.MessageForClient;

public class ServerProcessing implements Runnable {
    private DatagramSocket socket;

    private SpaceMarineCollection collection;
    private Exchanger<CommandForServer> exchangerCommand;
    private Exchanger<MessageForClient> exchangerMessage;
    private InetAddress address;
    private int port;
    private MessageForClient message;
    private Postgre postgre;

    public ServerProcessing(DatagramSocket socket, SpaceMarineCollection collection, Exchanger<CommandForServer> exchangerCommand, Exchanger<MessageForClient> exchangerMessage, Postgre postgre) {
        this.socket = socket;
        this.collection = collection;
        this.exchangerCommand = exchangerCommand;
        this.exchangerMessage = exchangerMessage;
        this.postgre = postgre;
    }

    @Override
    public void run() {
        SpaceMarine spaceMarine;
        User user;
        String check;
        String key;
        while(true){
            try {
                String owner;
                CommandForServer command = exchangerCommand.exchange(null);
                address = command.getAddress();
                port = command.getPort();
                switch (command.getCommandName().split(" ")[0]) {
                    case "askCollection":
                        message = new MessageForClient("1", address, port);
                        message.setSpaceMarineCollection(collection);
                        break;
                    case "add_user":
                        user = (User) command.getArgument();
                        message = new MessageForClient(postgre.addNewUser(user), address, port);
                        break;
                    case "help":
                        message = new MessageForClient("All commands : " + Commands.show(), address, port);
                        break;
                    case "info":
                        message = new MessageForClient(collection.info(), address, port);
                        break;
                    case "show":
                        message = new MessageForClient(collection.show(), address, port);
                        break;
                    case "insert":
                        spaceMarine = (SpaceMarine)command.getArgument();
                        key = command.getCommandName().split(" ")[1];
                        owner = command.getCommandName().split(" ")[2];
                        spaceMarine.setOwner(owner);
                        System.out.println(owner);
                        check = collection.insert(key, spaceMarine);
                        message = new MessageForClient(check, address, port);
                        if(check == "Element is inserted") {
                            spaceMarine.setKey(Long.parseLong(key));
                            collection.update(key, spaceMarine, owner);
                            postgre.insert(spaceMarine);
                        }
                        break;
                    case "update":
                        spaceMarine = (SpaceMarine)command.getArgument();
                        key = command.getCommandName().split(" ")[1];
                        owner = command.getCommandName().split(" ")[2];
                        spaceMarine.setOwner(owner);
                        long id = Long.parseLong(command.getCommandName().split(" ")[1]);
                        spaceMarine.setId(id);
                        message = new MessageForClient(collection.update(command.getCommandName().split(" ")[1], spaceMarine, owner), address, port);
                        break;
                    case "remove":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(collection.remove(command.getCommandName().split(" ")[1], owner), address, port);
                        break;
                    case "clear":
                        owner = command.getCommandName().split(" ")[1];
                        message = new MessageForClient(collection.clear(owner), address, port);
                        break;
                    case "execute_script":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(Console.executeFile(command.getCommandName().split(" ")[1], collection, postgre, owner), address, port);
                        break;
                    case "replace_if_lowe":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(collection.replace_if_lowe(command.getCommandName().split(" ")[1], (SpaceMarine) command.getArgument(), owner), address, port);
                        break;
                    case "remove_greater_key":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(collection.remove_greater_key(command.getCommandName().split(" ")[1], owner), address, port);
                        break;
                    case "remove_lower_key":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(collection.remove_lower_key(command.getCommandName().split(" ")[1], owner), address, port);
                        break;
                    case "remove_any_by_health":
                        owner = command.getCommandName().split(" ")[2];
                        message = new MessageForClient(collection.remove_any_by_health(command.getCommandName().split(" ")[1], owner), address, port);
                        break;
                    case "group_counting_by_health":
                        message = new MessageForClient(collection.group_counting_by_health());
                        break;
                    case "count_less_than_health":
                        message = new MessageForClient(collection.count_less_than_health(command.getCommandName().split(" ")[1]), address, port);
                        break;
                }
                exchangerMessage.exchange(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private byte[] serialize(MessageForClient message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(message);
        byte[] buffer = byteArrayOutputStream.toByteArray();
        objectOutputStream.flush();
        byteArrayOutputStream.flush();
        byteArrayOutputStream.close();
        objectOutputStream.close();
        return buffer;
    }
    private void send(MessageForClient message) throws IOException {
        byte[] sendBuffer = serialize(message);
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        socket.send(sendPacket);
    }
}
