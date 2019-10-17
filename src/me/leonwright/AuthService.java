package me.leonwright;

import java.util.HashMap;

public class AuthService {
    // Declaring Users
    private HashMap<String, String> userOne = new HashMap<String, String>();
    private HashMap<String, String> userTwo = new HashMap<String, String>();

    AuthService() {
        // Creating userOne
//        userOne.put("username", "dannyboi");
//        userOne.put("password", "dre@margh_shelled");
//
//        // Creating userTwo
//        userTwo.put("username", "matty7");
//        userTwo.put("password", "win&win99");

        userOne.put("username", "1");
        userOne.put("password", "1");

        // Creating userTwo
        userTwo.put("username", "2");
        userTwo.put("password", "2");
    }

    public boolean login(String username, String password) {
        if (username.equals(userOne.get("username")) && password.equals(userOne.get("password"))) {
            return true;
        } else return username.equals(userTwo.get("username")) && password.equals(userTwo.get("password"));
    }
}
