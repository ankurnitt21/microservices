package com.example.user_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequest {

    public interface Create {}
    public interface Update {}

    @NotBlank(message = "name must not be blank", groups = {Create.class})
    @Size(max = 255, message = "name must be at most 255 characters", groups = {Create.class, Update.class})
    private String name;

    @NotBlank(message = "email must not be blank", groups = {Create.class})
    @Email(message = "email must be a valid email address", groups = {Create.class, Update.class})
    @Size(max = 320, message = "email must be at most 320 characters", groups = {Create.class, Update.class})
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


