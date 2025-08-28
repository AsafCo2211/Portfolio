package cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class InputProvider {

    public enum Action { UP, DOWN, LEFT, RIGHT, CAST, SKIP, QUIT, NONE }

    private final Map<Character, Action> mapping;
    private final Scanner in = new Scanner(System.in);

    public InputProvider(Map<Character, Action> mapping) {
        this.mapping = mapping;
    }

    public Action nextAction() {
        String line = in.nextLine().trim().toLowerCase();
        if (line.isEmpty()) return Action.NONE;
        return mapping.getOrDefault(line.charAt(0), Action.NONE);
    }

    public static Map<Character, Action> promptForMapping() {
        record Q(String prompt, Action act) {}
        Q[] qs = {
                new Q("MOVING UP", Action.UP),
                new Q("MOVING DOWN", Action.DOWN),
                new Q("MOVING LEFT", Action.LEFT),
                new Q("MOVING RIGHT", Action.RIGHT),
                new Q("SKIP TURN", Action.SKIP),
                new Q("CAST SPECIAL ABILITY", Action.CAST),
                new Q("QUIT GAME", Action.QUIT)
        };

        Map<Character, Action> map = new HashMap<>();
        Scanner sc = new Scanner(System.in);

        for (Q q : qs) {
            while (true) {
                System.out.print("please choose the key for " + q.prompt() + " : ");
                String line = sc.nextLine().trim().toLowerCase();
                if (line.length() != 1 || map.containsKey(line.charAt(0))) {
                    System.out.println("Invalid or already used – try again.\n");
                    continue;
                }
                map.put(line.charAt(0), q.act());
                System.out.println();
                break;
            }
        }
        return map;
    }
}
