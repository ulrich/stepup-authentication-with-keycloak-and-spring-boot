package com.reservoircode.stepupauth.repository;

import com.reservoircode.stepupauth.domain.User;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class UserRepository {

    private final Map<String, User> users = new HashMap<>();

    public UserRepository() {
        User user1 = new User(null, "foo@gmail.com");
        User user2 = new User(null, "bar@gmail.com");

        users.put(user1.getEmail(), user1);
        users.put(user2.getEmail(), user2);
    }

    public User getUserByEmail(String email) {
        return users.get(email);
    }

    public void createUserByEmail(User user) {
        users.put(user.getEmail(), user);
    }

    public void updateUserByEmail(String email, User user) {
        users.remove(email);
        users.put(user.getEmail(), user);
    }

    public void deleteUserByEmail(String email) {
        users.remove(email);
    }
}
