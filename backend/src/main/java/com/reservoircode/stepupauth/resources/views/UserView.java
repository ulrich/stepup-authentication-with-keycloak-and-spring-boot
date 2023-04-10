package com.reservoircode.stepupauth.resources.views;

import com.reservoircode.stepupauth.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserView {

    private String email;

    public static UserView from(User user) {
        return new UserViewBuilder()
                .email(user.getEmail()).build();
    }

    public static User to(UserView userView) {
        return new User(null, userView.getEmail());
    }
}
