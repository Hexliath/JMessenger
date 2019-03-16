package entities.api;

import entities.enums.EnumChannelRole;

public class ChannelUserApiBody extends GenericApiEntity {

    private UserApiBody user;
    private EnumChannelRole role;

    public ChannelUserApiBody(UserApiBody user, EnumChannelRole role) {
        this.user = user;
        this.role = role;
    }

    public ChannelUserApiBody() {
    }

    public UserApiBody getUser() {
        return user;
    }

    public void setUser(UserApiBody user) {
        this.user = user;
    }

    public EnumChannelRole getRole() {
        return role;
    }

    public void setRole(EnumChannelRole role) {
        this.role = role;
    }
}
