package view;

import model.Client;
import lib.utils;


public class ClientView {

    public void show(Client client){
       utils.dbg(client.getDisplayName());
    }
}
