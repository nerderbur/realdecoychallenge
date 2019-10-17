package me.leonwright;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
