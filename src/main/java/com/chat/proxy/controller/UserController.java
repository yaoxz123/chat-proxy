package com.chat.proxy.controller;


import com.chat.proxy.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping(value = "/list")
    public User List(){
        User user = new User();
        user.setId(1);
        user.setName("a");
        return user;
    }
}
