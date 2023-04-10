package com.reservoircode.stepupauth.resources;

import com.reservoircode.stepupauth.domain.User;
import com.reservoircode.stepupauth.repository.UserRepository;
import com.reservoircode.stepupauth.resources.views.UserView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "user",
        produces = {"application/json"})
public class UserResource {

    private final UserRepository userRepository;

    @Autowired
    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserView get(@RequestParam String email) {
        return UserView.from(userRepository.getUserByEmail(email));
    }

    @PutMapping
    public ResponseEntity<Void> create(@RequestBody UserView userView) {
        User user = UserView.to(userView);

        userRepository.createUserByEmail(user);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Void> update(@RequestParam String email, @RequestBody UserView userView) {
        User user = UserView.to(userView);

        userRepository.updateUserByEmail(email, user);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String email) {
        userRepository.deleteUserByEmail(email);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
