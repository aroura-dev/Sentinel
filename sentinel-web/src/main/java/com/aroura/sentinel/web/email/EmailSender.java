package com.aroura.sentinel.web.email;

public interface EmailSender {

    boolean send(String email, String code);
}