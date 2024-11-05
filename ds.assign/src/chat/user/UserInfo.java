package chat.network.user;

public class UserInfo {
    private static int nextId = 1;
    private String name;
    private int idUser;

    public UserInfo(String name) {
        this.name = name;
        this.idUser = nextId ++;
    }

    public String getName() {
        return name;
    }

    public int getIdUser() {
        return idUser;
    }



}
