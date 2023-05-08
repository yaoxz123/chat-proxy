package com.chat.proxy.model;

import com.chat.proxy.contant.Role;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MessageRecord {

    private List<Chat> chatList = new ArrayList<>();

    public void addChat(Role role,String content){
        Chat chat = new Chat();
        chat.setRole(role.getRole());
        chat.setContent(content);
        chatList.add(chat);
    }
}
